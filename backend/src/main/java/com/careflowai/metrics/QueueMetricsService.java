package com.careflowai.metrics;

import com.careflowai.common.UrgencyCategory;
import com.careflowai.queue.QueueEntry;
import com.careflowai.queue.QueueService;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueueMetricsService {

    private final QueueService queueService;

    public QueueMetricsService(QueueService queueService) {
        this.queueService = queueService;
    }

    @Transactional(readOnly = true)
    public QueueMetricsResponse currentMetrics() {
        Instant now = Instant.now();
        List<QueueEntry> entries = queueService.sortedEntries(now);
        Map<UrgencyCategory, Long> patientsByUrgency = new EnumMap<>(UrgencyCategory.class);
        Map<UrgencyCategory, Long> averageWait = new EnumMap<>(UrgencyCategory.class);
        Map<UrgencyCategory, Long> longestWait = new EnumMap<>(UrgencyCategory.class);

        for (UrgencyCategory category : UrgencyCategory.values()) {
            List<QueueEntry> matching = entries.stream()
                .filter(entry -> entry.getUrgencyCategory() == category)
                .toList();
            patientsByUrgency.put(category, (long) matching.size());
            averageWait.put(category, averageWaitMinutes(matching, now));
            longestWait.put(category, longestWaitMinutes(matching, now));
        }

        long criticalAndHigh = entries.stream()
            .filter(entry -> entry.getUrgencyCategory() == UrgencyCategory.CRITICAL
                || entry.getUrgencyCategory() == UrgencyCategory.HIGH)
            .count();

        return new QueueMetricsResponse(
            entries.size(),
            criticalAndHigh,
            patientsByUrgency,
            averageWait,
            longestWait,
            queueService.overrideCount()
        );
    }

    private long averageWaitMinutes(List<QueueEntry> entries, Instant now) {
        if (entries.isEmpty()) {
            return 0;
        }
        long total = entries.stream()
            .mapToLong(entry -> waitMinutes(entry, now))
            .sum();
        return total / entries.size();
    }

    private long longestWaitMinutes(List<QueueEntry> entries, Instant now) {
        return entries.stream()
            .mapToLong(entry -> waitMinutes(entry, now))
            .max()
            .orElse(0);
    }

    private long waitMinutes(QueueEntry entry, Instant now) {
        return Math.max(0, Duration.between(entry.getWaitingSince(), now).toMinutes());
    }
}
