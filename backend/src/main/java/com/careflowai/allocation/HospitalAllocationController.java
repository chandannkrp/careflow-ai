package com.careflowai.allocation;

import com.careflowai.allocation.dto.HospitalAllocationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/allocation")
public class HospitalAllocationController {

    private final HospitalAllocationService allocationService;

    public HospitalAllocationController(HospitalAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping
    public HospitalAllocationResponse currentAllocation() {
        return allocationService.currentAllocation();
    }
}
