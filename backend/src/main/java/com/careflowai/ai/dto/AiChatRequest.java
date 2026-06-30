package com.careflowai.ai.dto;

import com.careflowai.common.StaffRole;
import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
    @NotBlank String message,
    String actorName,
    StaffRole actorRole
) {
}
