package com.careflowai.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
    boolean enabled,
    String apiKey,
    String model
) {
    public boolean canCallApi() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
