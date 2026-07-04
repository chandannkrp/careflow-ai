package com.careflowai.allocation.dto;

import com.careflowai.common.QueueStatus;
import com.careflowai.common.UrgencyCategory;
import java.time.Instant;
import java.util.UUID;

public record DoctorAllocationResponse(
    UUID doctorId,
    String staffCode,
    String displayName,
    String department,
    String specialty,
    boolean filled,
    String patientId,
    String patientDisplayId,
    QueueStatus patientStatus,
    UrgencyCategory urgencyCategory,
    String assignmentReason,
    Instant assignedAt
) {
}
