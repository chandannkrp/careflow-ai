package com.careflowai.allocation.dto;

import java.util.List;

public record HospitalAllocationResponse(
    AllocationSummaryResponse summary,
    List<BedAllocationResponse> beds,
    List<DoctorAllocationResponse> doctors
) {
}
