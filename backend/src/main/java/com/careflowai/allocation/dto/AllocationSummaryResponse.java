package com.careflowai.allocation.dto;

public record AllocationSummaryResponse(
    long filledBeds,
    long vacantBeds,
    long filledDoctors,
    long vacantDoctors
) {
}
