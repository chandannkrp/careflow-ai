package com.careflowai.queue.dto;

import com.careflowai.agent.CareTeamAssignment;
import java.time.Instant;
import java.util.UUID;

public record AssignedDoctorResponse(
    UUID id,
    String staffCode,
    String displayName,
    String department,
    String specialty,
    String assignmentReason,
    Instant assignedAt
) {
    public static AssignedDoctorResponse from(CareTeamAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return new AssignedDoctorResponse(
            assignment.getAssignedDoctor().getId(),
            assignment.getAssignedDoctor().getStaffCode(),
            assignment.getAssignedDoctor().getDisplayName(),
            assignment.getAssignedDoctor().getDepartment(),
            assignment.getAssignedDoctor().getSpecialty(),
            assignment.getAssignmentReason(),
            assignment.getAssignedAt()
        );
    }
}
