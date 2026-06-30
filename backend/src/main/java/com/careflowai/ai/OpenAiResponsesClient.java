package com.careflowai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiResponsesClient {

    private final OpenAiProperties properties;
    private final RestClient restClient;

    public OpenAiResponsesClient(OpenAiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = restClientBuilder
            .baseUrl("https://api.openai.com/v1")
            .requestFactory(requestFactory)
            .build();
    }

    public boolean isAvailable() {
        return properties.canCallApi();
    }

    public String respond(String developerInstruction, String userInput) {
        Map<String, Object> request = Map.of(
            "model", properties.model(),
            "input", List.of(
                Map.of("role", "developer", "content", developerInstruction),
                Map.of("role", "user", "content", userInput)
            )
        );

        JsonNode response = restClient.post()
            .uri("/responses")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(JsonNode.class);

        if (response == null) {
            return "";
        }

        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual()) {
            return outputText.asText();
        }

        StringBuilder text = new StringBuilder();
        response.path("output").forEach(output -> output.path("content").forEach(content -> {
            JsonNode nodeText = content.path("text");
            if (nodeText.isTextual()) {
                text.append(nodeText.asText()).append('\n');
            }
        }));
        return text.toString().trim();
    }
}
