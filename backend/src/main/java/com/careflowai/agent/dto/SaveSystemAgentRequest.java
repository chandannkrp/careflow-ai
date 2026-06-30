package com.careflowai.agent.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveSystemAgentRequest(
    @NotBlank String code,
    @NotBlank String name,
    @NotBlank String taskType,
    @NotBlank String description,
    String instructions,
    boolean active
) {
}
