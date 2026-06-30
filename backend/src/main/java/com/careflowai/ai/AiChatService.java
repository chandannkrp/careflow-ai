package com.careflowai.ai;

import com.careflowai.ai.dto.AiChatRequest;
import com.careflowai.ai.dto.AiChatResponse;
import com.careflowai.metrics.QueueMetricsResponse;
import com.careflowai.metrics.QueueMetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private static final String INSTRUCTION = """
        You are CareFlow AI's staff operations assistant.
        Answer only administrative questions about intake completeness, queue operations,
        prioritization rationale, wait-time visibility, and workflow actions.
        Do not diagnose, prescribe, or recommend clinical treatment.
        Keep answers concise and return plain text.
        """;

    private final OpenAiResponsesClient responsesClient;
    private final QueueMetricsService queueMetricsService;
    private final ObjectMapper objectMapper;

    public AiChatService(OpenAiResponsesClient responsesClient,
                         QueueMetricsService queueMetricsService,
                         ObjectMapper objectMapper) {
        this.responsesClient = responsesClient;
        this.queueMetricsService = queueMetricsService;
        this.objectMapper = objectMapper;
    }

    public AiChatResponse chat(AiChatRequest request) {
        QueueMetricsResponse metrics = queueMetricsService.currentMetrics();
        if (!responsesClient.isAvailable()) {
            return fallbackResponse(request, metrics);
        }

        try {
            String response = responsesClient.respond(INSTRUCTION, objectMapper.writeValueAsString(Map.of(
                "staff", Map.of(
                    "name", request.actorName() == null ? "Unknown" : request.actorName(),
                    "role", request.actorRole() == null ? "Unknown" : request.actorRole()
                ),
                "queueMetrics", metrics,
                "message", request.message()
            )));
            if (response == null || response.isBlank()) {
                return fallbackResponse(request, metrics);
            }
            return new AiChatResponse(response, suggestedActions(request.message()), true, Instant.now());
        } catch (Exception ignored) {
            return fallbackResponse(request, metrics);
        }
    }

    private AiChatResponse fallbackResponse(AiChatRequest request, QueueMetricsResponse metrics) {
        String normalized = request.message().toLowerCase();
        String message;
        if (normalized.contains("critical") || normalized.contains("urgent")) {
            message = "There are " + metrics.criticalAndHighWaiting()
                + " critical or high-priority patients waiting. Review the queue filter for Critical and High first.";
        } else if (normalized.contains("wait")) {
            message = "Longest waits by urgency are visible in the dashboard. Use the queue sort by wait time for row-level review.";
        } else if (normalized.contains("intake")) {
            message = "For intake, capture arrival mode, chief complaint, symptoms, vitals, distress level, risk flags, department, and staff notes.";
        } else if (normalized.contains("next") || normalized.contains("phase")) {
            message = "Next phase hardening should cover patient detail, audit history, reliable demo flows, validation checks, and clear AI fallback behavior.";
        } else if (normalized.contains("status") || normalized.contains("treatment")) {
            message = "Use the status dropdown or Start action in the queue to move a patient into treatment. Metrics refresh after status changes.";
        } else {
            message = "I can help with queue status, intake completeness, wait-time visibility, prioritization rationale, and next workflow steps.";
        }
        return new AiChatResponse(message, suggestedActions(request.message()), false, Instant.now());
    }

    private List<String> suggestedActions(String message) {
        String normalized = message.toLowerCase();
        if (normalized.contains("refresh") || normalized.contains("reload")) {
            return List.of("refresh_queue", "refresh_dashboard");
        }
        if (normalized.contains("critical") || normalized.contains("urgent")) {
            return List.of("filter_critical_high");
        }
        if (normalized.contains("intake")) {
            return List.of("open_intake");
        }
        if (normalized.contains("next") || normalized.contains("phase")) {
            return List.of("refresh_queue", "refresh_dashboard");
        }
        return List.of();
    }
}
