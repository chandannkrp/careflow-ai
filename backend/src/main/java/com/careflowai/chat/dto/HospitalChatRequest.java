package com.careflowai.chat.dto;

import com.careflowai.common.StaffRole;
import jakarta.validation.constraints.NotBlank;

public record HospitalChatRequest(
    @NotBlank String authorName,
    StaffRole authorRole,
    @NotBlank String body
) {
}
