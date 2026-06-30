package com.careflowai.agent;

import com.careflowai.assessment.UrgencyAssessment;
import com.careflowai.common.QueueStatus;
import com.careflowai.common.StaffRole;
import com.careflowai.common.UrgencyCategory;
import com.careflowai.agent.dto.AgentDashboardResponse;
import com.careflowai.agent.dto.PatientFlashcardResponse;
import com.careflowai.agent.dto.PatientTimelineEventResponse;
import com.careflowai.agent.dto.ResolveFlashcardRequest;
import com.careflowai.intake.Intake;
import com.careflowai.patient.Patient;
import com.careflowai.queue.QueueEntry;
import com.careflowai.staff.StaffUser;
import com.careflowai.staff.StaffUserRepository;
import com.careflowai.staff.StaffUserService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientAgentService {

    private static final int CARD_LIMIT = 12;
    private static final int TIMELINE_LIMIT = 30;

    private final CareTeamAssignmentRepository assignmentRepository;
    private final PatientFlashcardRepository flashcardRepository;
    private final PatientTimelineEventRepository timelineRepository;
    private final StaffUserRepository staffUserRepository;
    private final StaffUserService staffUserService;
    private final SystemAgentService systemAgentService;

    public PatientAgentService(CareTeamAssignmentRepository assignmentRepository,
                               PatientFlashcardRepository flashcardRepository,
                               PatientTimelineEventRepository timelineRepository,
                               StaffUserRepository staffUserRepository,
                               StaffUserService staffUserService,
                               SystemAgentService systemAgentService) {
        this.assignmentRepository = assignmentRepository;
        this.flashcardRepository = flashcardRepository;
        this.timelineRepository = timelineRepository;
        this.staffUserRepository = staffUserRepository;
        this.staffUserService = staffUserService;
        this.systemAgentService = systemAgentService;
    }

    @Transactional
    public void handleIntakeIngested(Patient patient, Intake intake, UrgencyAssessment assessment, QueueEntry queueEntry,
                                     StaffUser intakeActor) {
        StaffUser assignedDoctor = systemAgentService.isActive("ASSIGNMENT_AGENT")
            ? resolveAssignedDoctor(intake, intakeActor)
            : intakeActor;
        CareTeamAssignment assignment = assignmentRepository.save(new CareTeamAssignment(
            patient,
            intake,
            assignedDoctor,
            "Matched by department and queue urgency after intake ingestion."
        ));

        appendEvent(patient, intake, intakeActor, "INTAKE_INGESTED", "Intake ingested",
            "%s created the intake for %s.".formatted(displayName(intakeActor), patient.getDisplayId()), "STAFF");
        if (systemAgentService.isActive("PRIORITY_AGENT")) {
            appendEvent(patient, intake, null, "QUEUE_SORTED", "Patient sorted",
                "Priority Agent placed %s as %s with score %d.".formatted(
                    patient.getDisplayId(),
                    assessment.getFinalCategory(),
                    assessment.getFinalScore()
                ), "AGENT");
        }
        if (systemAgentService.isActive("NOTIFICATION_AGENT")) {
            appendEvent(patient, intake, assignedDoctor, "DOCTOR_ASSIGNED", "Doctor assigned",
                "Notification Agent assigned %s to %s.".formatted(assignedDoctor.getDisplayName(), patient.getDisplayId()), "AGENT");
        }

        refreshFlashcards(patient, intake, queueEntry, assignment, intakeActor);
        if (systemAgentService.isActive("NOTIFICATION_AGENT")) {
            appendEvent(patient, intake, null, "FLASHCARDS_CREATED", "Flashcards created",
                "Agent published care-team flashcards for the assigned doctor and intake team.", "AGENT");
        }
    }

    @Transactional
    public void handleStatusChanged(QueueEntry entry, StaffUser actor) {
        appendEvent(entry.getPatient(), entry.getIntake(), actor, "STATUS_CHANGED", "Queue status updated",
            "%s moved %s to %s.".formatted(displayName(actor), entry.getPatient().getDisplayId(), entry.getStatus()),
            "STAFF");
        refreshFlashcardsForEntry(entry, actor);
    }

    @Transactional
    public void handlePriorityOverridden(QueueEntry entry, StaffUser actor) {
        appendEvent(entry.getPatient(), entry.getIntake(), actor, "PRIORITY_OVERRIDDEN", "Priority overridden",
            "%s changed %s to %s with score %d.".formatted(
                displayName(actor),
                entry.getPatient().getDisplayId(),
                entry.getUrgencyCategory(),
                entry.getUrgencyScore()
            ), "STAFF");
        refreshFlashcardsForEntry(entry, actor);
    }

    @Transactional(readOnly = true)
    public AgentDashboardResponse getDashboard(String staffLookup, String department) {
        Pageable cardPage = PageRequest.of(0, CARD_LIMIT);
        Pageable timelinePage = PageRequest.of(0, TIMELINE_LIMIT);
        StaffUser staff = resolveOptionalStaff(staffLookup);

        List<PatientFlashcard> cards = loadCards(staff, department, cardPage);
        List<UUID> patientIds = cards.stream()
            .map(card -> card.getPatient().getId())
            .distinct()
            .toList();

        List<PatientTimelineEvent> timeline = patientIds.isEmpty()
            ? loadTimelineByScope(staff, department, timelinePage)
            : timelineRepository.findByPatientIdInOrderByCreatedAtDesc(patientIds, timelinePage);

        return new AgentDashboardResponse(
            cards.stream().map(PatientFlashcardResponse::from).toList(),
            timeline.stream().map(PatientTimelineEventResponse::from).toList()
        );
    }

    @Transactional
    public PatientFlashcardResponse resolveFlashcard(UUID flashcardId, ResolveFlashcardRequest request) {
        PatientFlashcard flashcard = flashcardRepository.findById(flashcardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flashcard not found."));
        String resolver = request != null && StringUtils.hasText(request.staffName())
            ? request.staffName().trim()
            : "Care team";
        flashcard.resolve(resolver);
        appendEvent(
            flashcard.getPatient(),
            flashcard.getIntake(),
            null,
            "FLASHCARD_RESOLVED",
            "Flashcard resolved",
            "%s marked the %s card for %s resolved.".formatted(
                resolver,
                flashcard.getAudienceRole(),
                flashcard.getPatient().getDisplayId()
            ),
            "STAFF"
        );
        return PatientFlashcardResponse.from(flashcard);
    }

    private void refreshFlashcardsForEntry(QueueEntry entry, StaffUser actor) {
        CareTeamAssignment assignment = assignmentRepository
            .findTopByPatientIdAndActiveTrueOrderByAssignedAtDesc(entry.getPatient().getId())
            .orElseGet(() -> assignmentRepository.save(new CareTeamAssignment(
                entry.getPatient(),
                entry.getIntake(),
                resolveAssignedDoctor(entry.getIntake(), actor),
                "Matched by department after queue event."
            )));
        refreshFlashcards(entry.getPatient(), entry.getIntake(), entry, assignment, actor);
    }

    private void refreshFlashcards(Patient patient, Intake intake, QueueEntry queueEntry,
                                   CareTeamAssignment assignment, StaffUser intakeActor) {
        PatientFlashcard doctorCard = flashcardRepository
            .findTopByPatientIdAndAudienceRoleAndAssignedStaffIdOrderByUpdatedAtDesc(
                patient.getId(),
                StaffRole.DOCTOR,
                assignment.getAssignedDoctor().getId()
            )
            .orElseGet(() -> new PatientFlashcard(patient, intake, assignment.getAssignedDoctor(), StaffRole.DOCTOR));
        doctorCard.refresh(
            doctorTitle(patient, queueEntry),
            discoveryBrief(intake, queueEntry),
            doctorAction(queueEntry.getStatus()),
            queueEntry.getUrgencyCategory(),
            queueEntry.getUrgencyScore(),
            queueEntry.getStatus()
        );
        flashcardRepository.save(doctorCard);

        PatientFlashcard intakeCard = flashcardRepository
            .findTopByPatientIdAndAudienceRoleAndDepartmentIgnoreCaseOrderByUpdatedAtDesc(
                patient.getId(),
                StaffRole.INTAKE_STAFF,
                intake.getDepartment()
            )
            .orElseGet(() -> new PatientFlashcard(patient, intake, intakeActor, StaffRole.INTAKE_STAFF));
        intakeCard.refresh(
            "Agent routed " + patient.getDisplayId(),
            "Sorted to %s/%d in %s and assigned to %s.".formatted(
                queueEntry.getUrgencyCategory(),
                queueEntry.getUrgencyScore(),
                intake.getDepartment(),
                assignment.getAssignedDoctor().getDisplayName()
            ),
            intakeAction(queueEntry.getStatus()),
            queueEntry.getUrgencyCategory(),
            queueEntry.getUrgencyScore(),
            queueEntry.getStatus()
        );
        flashcardRepository.save(intakeCard);
    }

    private StaffUser resolveAssignedDoctor(Intake intake, StaffUser fallbackActor) {
        String specialty = inferSpecialty(intake);
        return staffUserRepository
            .findFirstByRoleAndDepartmentIgnoreCaseAndSpecialtyIgnoreCaseAndActiveTrueOrderByCreatedAtAsc(
                StaffRole.DOCTOR,
                intake.getDepartment(),
                specialty
            )
            .or(() -> staffUserRepository.findFirstByRoleAndDepartmentIgnoreCaseAndActiveTrueOrderByCreatedAtAsc(
                StaffRole.DOCTOR,
                intake.getDepartment()
            ))
            .or(() -> staffUserRepository.findFirstByRoleAndActiveTrueOrderByCreatedAtAsc(StaffRole.DOCTOR))
            .or(() -> staffUserRepository.findFirstByRoleAndDepartmentIgnoreCaseAndActiveTrueOrderByCreatedAtAsc(
                StaffRole.CHARGE_NURSE,
                intake.getDepartment()
            ))
            .orElse(fallbackActor);
    }

    private String discoveryBrief(Intake intake, QueueEntry queueEntry) {
        if (!systemAgentService.isActive("DISCOVERY_BRIEF_AGENT")) {
            return doctorSummary(intake, queueEntry);
        }
        String symptoms = intake.getStructuredSymptoms().isEmpty()
            ? "no structured symptoms"
            : String.join(", ", intake.getStructuredSymptoms());
        String risks = riskSummary(intake);
        String vitals = "Vitals: HR %s, BP %s/%s, RR %s, SpO2 %s, Temp %s C.".formatted(
            valueOrUnknown(intake.getVitals().getHeartRate()),
            valueOrUnknown(intake.getVitals().getSystolicPressure()),
            valueOrUnknown(intake.getVitals().getDiastolicPressure()),
            valueOrUnknown(intake.getVitals().getRespiratoryRate()),
            valueOrUnknown(intake.getVitals().getOxygenSaturation()),
            valueOrUnknown(intake.getVitals().getTemperatureC())
        );
        return "%s is queued as %s/%d for %s. Symptoms noted: %s. %s %s Next action: %s.".formatted(
            intake.getPatient().getDisplayId(),
            queueEntry.getUrgencyCategory(),
            queueEntry.getUrgencyScore(),
            intake.getChiefComplaint(),
            symptoms,
            risks,
            vitals,
            doctorAction(queueEntry.getStatus())
        );
    }

    private String riskSummary(Intake intake) {
        List<String> risks = new java.util.ArrayList<>();
        if (intake.getRiskFlags().isChestPain()) {
            risks.add("chest pain");
        }
        if (intake.getRiskFlags().isBreathingDifficulty()) {
            risks.add("breathing difficulty");
        }
        if (intake.getRiskFlags().isAlteredMentalState()) {
            risks.add("altered mental state");
        }
        if (intake.getRiskFlags().isSevereBleeding()) {
            risks.add("severe bleeding");
        }
        if (intake.getRiskFlags().isPregnancy()) {
            risks.add("pregnancy");
        }
        if (intake.getRiskFlags().isPediatricRisk()) {
            risks.add("pediatric risk");
        }
        if (intake.getRiskFlags().isFallOrTrauma()) {
            risks.add("fall or trauma");
        }
        if (intake.getRiskFlags().isImmunocompromised()) {
            risks.add("immunocompromised");
        }
        return risks.isEmpty() ? "No risk flags selected." : "Risk flags: " + String.join(", ", risks) + ".";
    }

    private String valueOrUnknown(Object value) {
        return value == null ? "unknown" : String.valueOf(value);
    }

    private String inferSpecialty(Intake intake) {
        String signal = (intake.getChiefComplaint() + " " + String.join(" ", intake.getStructuredSymptoms()) + " "
            + String.valueOf(intake.getSymptomNotes())).toLowerCase();
        if (intake.getRiskFlags().isChestPain() || signal.contains("chest") || signal.contains("cardiac")
            || signal.contains("heart")) {
            return "Cardiology";
        }
        if (intake.getRiskFlags().isPediatricRisk() || signal.contains("child") || signal.contains("pediatric")) {
            return "Pediatrics";
        }
        if (intake.getRiskFlags().isFallOrTrauma() || signal.contains("fracture") || signal.contains("fall")
            || signal.contains("trauma") || signal.contains("bone")) {
            return "Orthopedics";
        }
        if (intake.getRiskFlags().isBreathingDifficulty() || signal.contains("breath") || signal.contains("asthma")
            || signal.contains("oxygen")) {
            return "Pulmonology";
        }
        if (intake.getRiskFlags().isPregnancy() || signal.contains("pregnan")) {
            return "Obstetrics";
        }
        return "Emergency Medicine";
    }

    private void appendEvent(Patient patient, Intake intake, StaffUser actor, String eventType, String title,
                             String description, String source) {
        timelineRepository.save(new PatientTimelineEvent(patient, intake, actor, eventType, title, description, source));
    }

    private StaffUser resolveOptionalStaff(String staffLookup) {
        if (!StringUtils.hasText(staffLookup)) {
            return null;
        }
        return staffUserService.getByLookup(staffLookup.trim());
    }

    private List<PatientFlashcard> loadCards(StaffUser staff, String department, Pageable pageable) {
        if (staff != null && staff.getRole() == StaffRole.DOCTOR) {
            return flashcardRepository.findByAssignedStaffIdOrderByUpdatedAtDesc(staff.getId(), pageable);
        }
        if (staff != null && staff.getRole() == StaffRole.INTAKE_STAFF && StringUtils.hasText(staff.getDepartment())) {
            return flashcardRepository.findByAudienceRoleAndDepartmentIgnoreCaseOrderByUpdatedAtDesc(
                StaffRole.INTAKE_STAFF,
                staff.getDepartment(),
                pageable
            );
        }
        String scopedDepartment = staff != null && StringUtils.hasText(staff.getDepartment())
            ? staff.getDepartment()
            : department;
        if (StringUtils.hasText(scopedDepartment)) {
            return flashcardRepository.findByDepartmentIgnoreCaseOrderByUpdatedAtDesc(scopedDepartment, pageable);
        }
        return flashcardRepository.findAllByOrderByUpdatedAtDesc(pageable);
    }

    private List<PatientTimelineEvent> loadTimelineByScope(StaffUser staff, String department, Pageable pageable) {
        String scopedDepartment = staff != null && StringUtils.hasText(staff.getDepartment())
            ? staff.getDepartment()
            : department;
        if (StringUtils.hasText(scopedDepartment)) {
            return timelineRepository.findByDepartmentIgnoreCaseOrderByCreatedAtDesc(scopedDepartment, pageable);
        }
        return timelineRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    private String doctorTitle(Patient patient, QueueEntry queueEntry) {
        return "%s care card - %s".formatted(patient.getDisplayId(), queueEntry.getUrgencyCategory());
    }

    private String doctorSummary(Intake intake, QueueEntry queueEntry) {
        return "%s. Complaint: %s. Pain/distress score %d. Current status: %s.".formatted(
            queueEntry.getUrgencyCategory(),
            intake.getChiefComplaint(),
            intake.getPainLevel(),
            queueEntry.getStatus()
        );
    }

    private String doctorAction(QueueStatus status) {
        return switch (status) {
            case WAITING, IN_TRIAGE -> "Review and start treatment";
            case IN_TREATMENT -> "Continue treatment";
            case DISCHARGED -> "Review discharge record";
            case LEFT_WITHOUT_BEING_SEEN -> "Review follow-up risk";
        };
    }

    private String intakeAction(QueueStatus status) {
        return switch (status) {
            case WAITING -> "Monitor queue placement";
            case IN_TRIAGE -> "Track triage handoff";
            case IN_TREATMENT -> "Treatment started";
            case DISCHARGED -> "Closed";
            case LEFT_WITHOUT_BEING_SEEN -> "Flag departure";
        };
    }

    private String displayName(StaffUser staffUser) {
        return staffUser == null ? "CareFlow Agent" : staffUser.getDisplayName();
    }
}
