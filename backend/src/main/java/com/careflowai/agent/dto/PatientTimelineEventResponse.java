package com.careflowai.agent.dto;

import com.careflowai.agent.PatientTimelineEvent;
import java.time.Instant;
import java.util.UUID;

public record PatientTimelineEventResponse(
    UUID id,
    UUID patientId,
    UUID intakeId,
    String patientDisplayId,
    String actorName,
    String department,
    String eventType,
    String title,
    String description,
    String source,
    Instant createdAt
) {
    public static PatientTimelineEventResponse from(PatientTimelineEvent event) {
        return new PatientTimelineEventResponse(
            event.getId(),
            event.getPatient().getId(),
            event.getIntake().getId(),
            event.getPatient().getDisplayId(),
            event.getActorStaffUser() == null ? null : event.getActorStaffUser().getDisplayName(),
            event.getDepartment(),
            event.getEventType(),
            event.getTitle(),
            event.getDescription(),
            event.getSource(),
            event.getCreatedAt()
        );
    }
}
