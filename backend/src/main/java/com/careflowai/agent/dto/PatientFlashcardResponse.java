package com.careflowai.agent.dto;

import com.careflowai.agent.PatientFlashcard;
import com.careflowai.common.QueueStatus;
import com.careflowai.common.StaffRole;
import com.careflowai.common.UrgencyCategory;
import java.time.Instant;
import java.util.UUID;

public record PatientFlashcardResponse(
    UUID id,
    UUID patientId,
    UUID intakeId,
    String patientDisplayId,
    String assignedStaffName,
    StaffRole audienceRole,
    String department,
    String title,
    String summary,
    String actionLabel,
    UrgencyCategory urgencyCategory,
    int urgencyScore,
    QueueStatus status,
    boolean resolved,
    String resolvedBy,
    Instant resolvedAt,
    Instant updatedAt
) {
    public static PatientFlashcardResponse from(PatientFlashcard flashcard) {
        return new PatientFlashcardResponse(
            flashcard.getId(),
            flashcard.getPatient().getId(),
            flashcard.getIntake().getId(),
            flashcard.getPatient().getDisplayId(),
            flashcard.getAssignedStaff() == null ? null : flashcard.getAssignedStaff().getDisplayName(),
            flashcard.getAudienceRole(),
            flashcard.getDepartment(),
            flashcard.getTitle(),
            flashcard.getSummary(),
            flashcard.getActionLabel(),
            flashcard.getUrgencyCategory(),
            flashcard.getUrgencyScore(),
            flashcard.getStatus(),
            flashcard.isResolved(),
            flashcard.getResolvedBy(),
            flashcard.getResolvedAt(),
            flashcard.getUpdatedAt()
        );
    }
}
