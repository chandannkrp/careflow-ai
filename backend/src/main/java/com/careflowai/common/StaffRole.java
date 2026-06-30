package com.careflowai.common;

public enum StaffRole {
    INTAKE_STAFF,
    DOCTOR,
    TRIAGE_NURSE,
    CHARGE_NURSE,
    ADMIN;

    public boolean canOverridePriority() {
        return this == TRIAGE_NURSE || this == CHARGE_NURSE || this == ADMIN;
    }

    public boolean canUpdateQueueStatus() {
        return this == DOCTOR || canOverridePriority();
    }
}
