package com.careflowai.queue.dto;

import com.careflowai.common.StaffRole;
import jakarta.validation.constraints.NotBlank;

public record AssignDoctorRequest(
    @NotBlank String doctorLookup,
    String actorName,
    StaffRole actorRole,
    String note
) {
}
