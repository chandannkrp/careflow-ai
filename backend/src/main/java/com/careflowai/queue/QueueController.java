package com.careflowai.queue;

import com.careflowai.common.QueueStatus;
import com.careflowai.common.UrgencyCategory;
import com.careflowai.queue.dto.OverridePriorityRequest;
import com.careflowai.queue.dto.QueueEntryResponse;
import com.careflowai.queue.dto.UpdatePlacementRequest;
import com.careflowai.queue.dto.UpdateStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping
    public List<QueueEntryResponse> queue(@RequestParam(required = false) UrgencyCategory category,
                                          @RequestParam(required = false) String department,
                                          @RequestParam(required = false) QueueStatus status) {
        return queueService.getQueue(category, department, status);
    }

    @PostMapping("/{patientId}/override")
    public QueueEntryResponse overridePriority(@PathVariable UUID patientId,
                                               @Valid @RequestBody OverridePriorityRequest request) {
        return queueService.overridePriority(patientId, request);
    }

    @PostMapping("/{patientId}/status")
    public QueueEntryResponse updateStatus(@PathVariable UUID patientId,
                                           @Valid @RequestBody UpdateStatusRequest request) {
        return queueService.updateStatus(patientId, request);
    }

    @PostMapping("/{patientId}/placement")
    public QueueEntryResponse updatePlacement(@PathVariable UUID patientId,
                                              @Valid @RequestBody UpdatePlacementRequest request) {
        return queueService.updatePlacement(patientId, request);
    }
}
