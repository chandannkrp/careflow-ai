package com.careflowai.queue.dto;

import com.careflowai.common.OverrideReason;
import com.careflowai.common.StaffRole;
import com.careflowai.common.UrgencyCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OverridePriorityRequest(
    @NotNull UrgencyCategory newCategory,
    @Min(0) @Max(100) int newScore,
    @NotNull OverrideReason reason,
    String note,
    String actorName,
    @NotNull StaffRole actorRole
) {
}
