package com.careflowai.intake.dto;

import java.time.Instant;

public record PatientReportResponse(
    String patientDisplayId,
    String report,
    boolean aiBacked,
    Instant createdAt
) {
}
