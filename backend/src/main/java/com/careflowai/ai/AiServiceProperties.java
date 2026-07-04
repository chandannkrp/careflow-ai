package com.careflowai.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-service")
public record AiServiceProperties(
    String url
) {
    public boolean isConfigured() {
        return url != null && !url.isBlank();
    }
}
