package com.careflowai.metrics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final QueueMetricsService queueMetricsService;

    public MetricsController(QueueMetricsService queueMetricsService) {
        this.queueMetricsService = queueMetricsService;
    }

    @GetMapping("/queue")
    public QueueMetricsResponse queueMetrics() {
        return queueMetricsService.currentMetrics();
    }
}
