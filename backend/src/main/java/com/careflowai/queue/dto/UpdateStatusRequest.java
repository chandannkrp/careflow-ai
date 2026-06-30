package com.careflowai.queue.dto;

import com.careflowai.common.QueueStatus;
import com.careflowai.common.StaffRole;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
    @NotNull QueueStatus status,
    String actorName,
    @NotNull StaffRole actorRole
) {
}
