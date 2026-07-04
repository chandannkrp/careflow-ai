package com.careflowai.intake;

import com.careflowai.agent.PatientAgentService;
import com.careflowai.ai.AiAssessmentOutput;
import com.careflowai.ai.AiAssessmentService;
import com.careflowai.assessment.UrgencyAssessment;
import com.careflowai.assessment.UrgencyAssessmentRepository;
import com.careflowai.assessment.dto.UrgencyAssessmentResponse;
import com.careflowai.common.QueueStatus;
import com.careflowai.intake.dto.CreateIntakeRequest;
import com.careflowai.intake.dto.IntakeResponse;
import com.careflowai.patient.Patient;
import com.careflowai.patient.PatientRepository;
import com.careflowai.queue.QueueEntry;
import com.careflowai.queue.QueueService;
import com.careflowai.staff.StaffUser;
import com.careflowai.staff.StaffUserService;
import com.careflowai.vector.SimpleIntakeVectorStore;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IntakeService {

    private final PatientRepository patientRepository;
    private final IntakeRepository intakeRepository;
    private final UrgencyAssessmentRepository assessmentRepository;
    private final AiAssessmentService aiAssessmentService;
    private final QueueService queueService;
    private final PatientAgentService patientAgentService;
    private final StaffUserService staffUserService;
    private final IntakeMapper intakeMapper;
    private final SimpleIntakeVectorStore vectorStore;

    public IntakeService(PatientRepository patientRepository,
                         IntakeRepository intakeRepository,
                         UrgencyAssessmentRepository assessmentRepository,
                         AiAssessmentService aiAssessmentService,
                         QueueService queueService,
                         PatientAgentService patientAgentService,
                         StaffUserService staffUserService,
                         IntakeMapper intakeMapper,
                         SimpleIntakeVectorStore vectorStore) {
        this.patientRepository = patientRepository;
        this.intakeRepository = intakeRepository;
        this.assessmentRepository = assessmentRepository;
        this.aiAssessmentService = aiAssessmentService;
        this.queueService = queueService;
        this.patientAgentService = patientAgentService;
        this.staffUserService = staffUserService;
        this.intakeMapper = intakeMapper;
        this.vectorStore = vectorStore;
    }

    @Transactional
    public IntakeResponse create(CreateIntakeRequest request) {
        String patientDisplayId = resolvePatientDisplayId(request.patientDisplayId());
        patientRepository.findByDisplayId(patientDisplayId).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient display ID already exists.");
        });

        StaffUser actor = staffUserService.resolveActor(request.staffName(), null, request.department());
        Patient patient = patientRepository.save(new Patient(patientDisplayId, request.ageBand()));
        Intake intake = intakeRepository.save(toIntake(patient, request, actor));
        UrgencyAssessment assessment = createAssessment(patient, intake);
        QueueEntry queueEntry = queueService.upsertFromAssessment(patient, intake, assessment);
        patientAgentService.handleIntakeIngested(patient, intake, assessment, queueEntry, actor);
        vectorStore.indexQueueEntry(queueEntry, assessment, assignmentSummary(patient));
        return IntakeResponse.from(intake, UrgencyAssessmentResponse.from(assessment));
    }

    @Transactional(readOnly = true)
    public String nextPatientDisplayId() {
        return generatePatientDisplayId();
    }

    @Transactional(readOnly = true)
    public IntakeResponse get(UUID intakeId) {
        Intake intake = intakeRepository.findById(intakeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intake not found."));
        UrgencyAssessment assessment = assessmentRepository.findTopByPatientIdOrderByAssessedAtDesc(intake.getPatient().getId())
            .orElse(null);
        return IntakeResponse.from(intake, assessment == null ? null : UrgencyAssessmentResponse.from(assessment));
    }

    @Transactional
    public IntakeResponse assess(UUID intakeId) {
        Intake intake = intakeRepository.findById(intakeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intake not found."));
        UrgencyAssessment assessment = createAssessment(intake.getPatient(), intake);
        QueueEntry queueEntry = queueService.upsertFromAssessment(intake.getPatient(), intake, assessment);
        vectorStore.indexQueueEntry(queueEntry, assessment, assignmentSummary(intake.getPatient()));
        return IntakeResponse.from(intake, UrgencyAssessmentResponse.from(assessment));
    }

    private Intake toIntake(Patient patient, CreateIntakeRequest request, StaffUser actor) {
        return new Intake(
            patient,
            request.arrivalTimestamp() == null ? Instant.now() : request.arrivalTimestamp(),
            request.arrivalMode(),
            request.chiefComplaint(),
            request.symptomNotes(),
            cleanSymptoms(request.structuredSymptoms()),
            request.painLevel(),
            intakeMapper.toVitals(request.vitals()),
            intakeMapper.toRiskFlags(request.riskFlags()),
            request.department(),
            request.currentStatus() == null ? QueueStatus.WAITING : request.currentStatus(),
            staffAttribution(actor)
        );
    }

    private String resolvePatientDisplayId(String requestedDisplayId) {
        if (requestedDisplayId != null && !requestedDisplayId.isBlank()) {
            return requestedDisplayId.trim().toUpperCase();
        }
        return generatePatientDisplayId();
    }

    private String generatePatientDisplayId() {
        String datePart = LocalDate.now(ZoneOffset.UTC).toString().replace("-", "");
        String prefix = "CF-" + datePart + "-";
        long nextNumber = patientRepository.countByDisplayIdStartingWith(prefix) + 1;
        String candidate;
        do {
            candidate = prefix + String.format("%04d", nextNumber++);
        } while (patientRepository.findByDisplayId(candidate).isPresent());
        return candidate;
    }

    private String staffAttribution(StaffUser actor) {
        if (actor == null) {
            return "Auto-filled by CareFlow intake workspace.";
        }
        return "Auto-filled from logged-in staff: %s (%s, %s).".formatted(
            actor.getDisplayName(),
            actor.getStaffCode(),
            actor.getRole()
        );
    }

    private UrgencyAssessment createAssessment(Patient patient, Intake intake) {
        AiAssessmentOutput advisory = aiAssessmentService.assess(intake);
        if (advisory.suggestedCategory() == null || advisory.suggestedScore() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "LLM triage response did not include a usable urgency category and score.");
        }
        int finalScore = Math.max(0, Math.min(100, advisory.suggestedScore()));
        UrgencyAssessment assessment = new UrgencyAssessment(patient, intake, advisory.suggestedCategory(), finalScore);
        assessment.replaceScoreFactors(scoreFactors(advisory));
        assessment.attachAdvisoryOutput(
            advisory.suggestedDiagnosis(),
            advisory.suggestedCategory(),
            advisory.suggestedScore(),
            advisory.redFlagIndicators(),
            advisory.missingOrAmbiguousDetails(),
            advisory.structuredSymptomSummary(),
            advisory.medicalAttentionNote(),
            advisory.staffFacingExplanation(),
            advisory.confidenceLevel()
        );
        return assessmentRepository.save(assessment);
    }

    private List<String> scoreFactors(AiAssessmentOutput advisory) {
        List<String> factors = new java.util.ArrayList<>();
        factors.add("LLM triage assessment selected the final urgency category and score.");
        if (!advisory.redFlagIndicators().isEmpty()) {
            factors.add("LLM red flags: " + String.join(", ", advisory.redFlagIndicators()));
        }
        if (advisory.suggestedDiagnosis() != null && !advisory.suggestedDiagnosis().isBlank()) {
            factors.add("LLM suggested diagnosis: " + advisory.suggestedDiagnosis());
        }
        if (advisory.medicalAttentionNote() != null && !advisory.medicalAttentionNote().isBlank()) {
            factors.add("Medical attention note: " + advisory.medicalAttentionNote());
        }
        if (advisory.staffFacingExplanation() != null && !advisory.staffFacingExplanation().isBlank()) {
            factors.add(advisory.staffFacingExplanation());
        }
        return factors;
    }

    private List<String> cleanSymptoms(List<String> symptoms) {
        if (symptoms == null) {
            return List.of();
        }
        return symptoms.stream()
            .filter(symptom -> symptom != null && !symptom.isBlank())
            .map(String::trim)
            .toList();
    }

    private String assignmentSummary(Patient patient) {
        return patientAgentService.currentAssignment(patient.getId())
            .map(assignment -> "Active doctor assignment: %s (%s). Reason: %s.".formatted(
                assignment.getAssignedDoctor().getDisplayName(),
                assignment.getAssignedDoctor().getStaffCode(),
                assignment.getAssignmentReason()
            ))
            .orElse(null);
    }
}
