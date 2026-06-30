package com.careflowai.staff.dto;

import com.careflowai.common.StaffRole;
import com.careflowai.staff.StaffUser;
import java.util.UUID;

public record StaffUserResponse(
    UUID id,
    String staffCode,
    String displayName,
    StaffRole role,
    String department,
    String specialty,
    boolean active
) {
    public static StaffUserResponse from(StaffUser staffUser) {
        return new StaffUserResponse(
            staffUser.getId(),
            staffUser.getStaffCode(),
            staffUser.getDisplayName(),
            staffUser.getRole(),
            staffUser.getDepartment(),
            staffUser.getSpecialty(),
            staffUser.isActive()
        );
    }
}
