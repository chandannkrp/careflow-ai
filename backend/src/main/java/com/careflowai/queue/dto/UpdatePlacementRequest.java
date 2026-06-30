package com.careflowai.queue.dto;

import com.careflowai.common.QueueStatus;
import com.careflowai.common.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePlacementRequest(
    @NotNull QueueStatus status,
    @NotBlank String department,
    String actorName,
    StaffRole actorRole
) {
}
