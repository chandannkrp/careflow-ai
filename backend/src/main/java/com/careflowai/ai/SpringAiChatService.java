package com.careflowai.ai;

import com.careflowai.ai.dto.AiChatRequest;
import com.careflowai.ai.dto.AiChatResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class SpringAiChatService {

    private static final String TEST_CHAT_INSTRUCTION = """
        You are CareFlow AI's test chat assistant.
        Keep responses concise, helpful, and plain text.
        Do not provide medical diagnosis, prescriptions, or treatment advice.
        """;

    private final ChatClient chatClient;

    public SpringAiChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public AiChatResponse chat(AiChatRequest request) {
        String response = respond(TEST_CHAT_INSTRUCTION, request.message());
        return new AiChatResponse(response, List.of(), true, Instant.now());
    }

    public String respond(String systemInstruction, String userMessage) {
        return chatClient.prompt()
            .system(systemInstruction)
            .user(userMessage)
            .call()
            .content();
    }
}
