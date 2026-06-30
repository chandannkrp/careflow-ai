package com.careflowai.intake;

import com.careflowai.agent.PatientAgentService;
import com.careflowai.ai.AiAssessmentOutput;
import com.careflowai.ai.AiAssessmentService;
import com.careflowai.assessment.ScoreResult;
import com.careflowai.assessment.UrgencyAssessment;
import com.careflowai.assessment.UrgencyAssessmentRepository;
import com.careflowai.assessment.UrgencyScoringService;
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
    private final UrgencyScoringService scoringService;
    private final AiAssessmentService aiAssessmentService;
    private final QueueService queueService;
    private final PatientAgentService patientAgentService;
    private final StaffUserService staffUserService;
    private final IntakeMapper intakeMapper;

    public IntakeService(PatientRepository patientRepository,
                         IntakeRepository intakeRepository,
                         UrgencyAssessmentRepository assessmentRepository,
                         UrgencyScoringService scoringService,
                         AiAssessmentService aiAssessmentService,
                         QueueService queueService,
                         PatientAgentService patientAgentService,
                         StaffUserService staffUserService,
                         IntakeMapper intakeMapper) {
        this.patientRepository = patientRepository;
        this.intakeRepository = intakeRepository;
        this.assessmentRepository = assessmentRepository;
        this.scoringService = scoringService;
        this.aiAssessmentService = aiAssessmentService;
        this.queueService = queueService;
        this.patientAgentService = patientAgentService;
        this.staffUserService = staffUserService;
        this.intakeMapper = intakeMapper;
    }

    @Transactional
    public IntakeResponse create(CreateIntakeRequest request) {
        patientRepository.findByDisplayId(request.patientDisplayId()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient display ID already exists.");
        });

        StaffUser actor = staffUserService.resolveActor(request.staffName(), null, request.department());
        Patient patient = patientRepository.save(new Patient(request.patientDisplayId(), request.ageBand()));
        Intake intake = intakeRepository.save(toIntake(patient, request));
        UrgencyAssessment assessment = createAssessment(patient, intake);
        QueueEntry queueEntry = queueService.upsertFromAssessment(patient, intake, assessment);
        patientAgentService.handleIntakeIngested(patient, intake, assessment, queueEntry, actor);
        return IntakeResponse.from(intake, UrgencyAssessmentResponse.from(assessment));
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
        queueService.upsertFromAssessment(intake.getPatient(), intake, assessment);
        return IntakeResponse.from(intake, UrgencyAssessmentResponse.from(assessment));
    }

    private Intake toIntake(Patient patient, CreateIntakeRequest request) {
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
            request.staffNotes()
        );
    }

    private UrgencyAssessment createAssessment(Patient patient, Intake intake) {
        ScoreResult result = scoringService.score(intake);
        UrgencyAssessment assessment = new UrgencyAssessment(patient, intake, result.category(), result.score());
        assessment.replaceScoreFactors(result.factors());
        AiAssessmentOutput advisory = aiAssessmentService.assess(intake);
        assessment.attachAdvisoryOutput(
            advisory.suggestedCategory(),
            advisory.suggestedScore(),
            advisory.redFlagIndicators(),
            advisory.missingOrAmbiguousDetails(),
            advisory.structuredSymptomSummary(),
            advisory.staffFacingExplanation(),
            advisory.confidenceLevel()
        );
        return assessmentRepository.save(assessment);
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
}
