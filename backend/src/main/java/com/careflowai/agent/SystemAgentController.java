package com.careflowai.agent;

import com.careflowai.agent.dto.SaveSystemAgentRequest;
import com.careflowai.agent.dto.SystemAgentResponse;
import com.careflowai.agent.dto.ToggleAgentRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class SystemAgentController {

    private final SystemAgentService systemAgentService;

    public SystemAgentController(SystemAgentService systemAgentService) {
        this.systemAgentService = systemAgentService;
    }

    @GetMapping
    public List<SystemAgentResponse> list() {
        return systemAgentService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SystemAgentResponse create(@Valid @RequestBody SaveSystemAgentRequest request) {
        return systemAgentService.create(request);
    }

    @PutMapping("/{agentId}")
    public SystemAgentResponse update(@PathVariable UUID agentId,
                                      @Valid @RequestBody SaveSystemAgentRequest request) {
        return systemAgentService.update(agentId, request);
    }

    @PatchMapping("/{agentId}/active")
    public SystemAgentResponse toggle(@PathVariable UUID agentId,
                                      @RequestBody ToggleAgentRequest request) {
        return systemAgentService.toggle(agentId, request.active());
    }
}
