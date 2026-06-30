package com.careflowai.agent.dto;

import com.careflowai.agent.SystemAgent;
import java.time.Instant;
import java.util.UUID;

public record SystemAgentResponse(
    UUID id,
    String code,
    String name,
    String taskType,
    String description,
    String instructions,
    boolean active,
    Instant updatedAt
) {
    public static SystemAgentResponse from(SystemAgent agent) {
        return new SystemAgentResponse(
            agent.getId(),
            agent.getCode(),
            agent.getName(),
            agent.getTaskType(),
            agent.getDescription(),
            agent.getInstructions(),
            agent.isActive(),
            agent.getUpdatedAt()
        );
    }
}
