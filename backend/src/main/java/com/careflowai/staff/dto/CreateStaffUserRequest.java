package com.careflowai.staff.dto;

import com.careflowai.common.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStaffUserRequest(
    @NotBlank String displayName,
    String staffCode,
    @NotNull StaffRole role,
    String department,
    String specialty,
    Boolean active
) {
}
