package com.careflowai.ai;

import com.careflowai.ai.dto.AiChatRequest;
import com.careflowai.ai.dto.AiChatResponse;
import com.careflowai.metrics.QueueMetricsResponse;
import com.careflowai.queue.dto.QueueEntryResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiServiceClient {

    private final AiServiceProperties properties;
    private final RestClient.Builder restClientBuilder;

    public AiServiceClient(AiServiceProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public Optional<AiChatResponse> chat(AiChatRequest request,
                                         QueueMetricsResponse metrics,
                                         List<QueueEntryResponse> queueContext) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            AiServiceChatResponse response = restClient().post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "message", request.message(),
                    "actorName", request.actorName() == null ? "" : request.actorName(),
                    "actorRole", request.actorRole() == null ? "" : request.actorRole().name(),
                    "context", Map.of(
                        "queueMetrics", metrics,
                        "currentQueue", queueContext
                    )
                ))
                .retrieve()
                .body(AiServiceChatResponse.class);

            if (response == null || response.message() == null || response.message().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new AiChatResponse(
                response.message(),
                response.suggestedActions() == null ? List.of() : response.suggestedActions(),
                response.aiBacked(),
                response.createdAt() == null ? Instant.now() : response.createdAt()
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return restClientBuilder
            .baseUrl(properties.url())
            .requestFactory(requestFactory)
            .build();
    }

    private record AiServiceChatResponse(
        String message,
        List<String> suggestedActions,
        boolean aiBacked,
        Instant createdAt
    ) {
    }
}
