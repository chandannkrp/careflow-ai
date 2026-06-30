package com.careflowai.ai.dto;

import java.time.Instant;
import java.util.List;

public record AiChatResponse(
    String message,
    List<String> suggestedActions,
    boolean aiBacked,
    Instant createdAt
) {
}
