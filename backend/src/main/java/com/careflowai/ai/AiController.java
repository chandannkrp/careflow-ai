package com.careflowai.ai;

import com.careflowai.ai.dto.AiChatRequest;
import com.careflowai.ai.dto.AiChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiChatService aiChatService;
    private final SpringAiChatService springAiChatService;

    public AiController(AiChatService aiChatService, SpringAiChatService springAiChatService) {
        this.aiChatService = aiChatService;
        this.springAiChatService = springAiChatService;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return aiChatService.chat(request);
    }

    @PostMapping("/test-chat")
    public AiChatResponse testChat(@Valid @RequestBody AiChatRequest request) {
        return springAiChatService.chat(request);
    }
}
