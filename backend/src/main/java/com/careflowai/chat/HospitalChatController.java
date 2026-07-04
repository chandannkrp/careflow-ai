package com.careflowai.chat;

import com.careflowai.chat.dto.HospitalChatMessageResponse;
import com.careflowai.chat.dto.HospitalChatRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hospital-chat")
public class HospitalChatController {

    private final HospitalChatService hospitalChatService;

    public HospitalChatController(HospitalChatService hospitalChatService) {
        this.hospitalChatService = hospitalChatService;
    }

    @GetMapping
    public List<HospitalChatMessageResponse> list() {
        return hospitalChatService.list();
    }

    @PostMapping
    public List<HospitalChatMessageResponse> post(@Valid @RequestBody HospitalChatRequest request) {
        return hospitalChatService.post(request);
    }
}
