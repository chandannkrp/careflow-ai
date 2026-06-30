package com.careflowai.metrics;

import com.careflowai.common.UrgencyCategory;
import java.util.Map;

public record QueueMetricsResponse(
    int currentQueueSize,
    long criticalAndHighWaiting,
    Map<UrgencyCategory, Long> patientsByUrgency,
    Map<UrgencyCategory, Long> averageWaitMinutesByUrgency,
    Map<UrgencyCategory, Long> longestWaitMinutesByUrgency,
    long overrideCount
) {
}
