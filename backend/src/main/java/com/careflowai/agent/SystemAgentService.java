package com.careflowai.agent;

import com.careflowai.agent.dto.SaveSystemAgentRequest;
import com.careflowai.agent.dto.SystemAgentResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SystemAgentService {

    private final SystemAgentRepository systemAgentRepository;

    public SystemAgentService(SystemAgentRepository systemAgentRepository) {
        this.systemAgentRepository = systemAgentRepository;
    }

    @Transactional(readOnly = true)
    public List<SystemAgentResponse> list() {
        return systemAgentRepository.findAllByOrderByTaskTypeAscNameAsc().stream()
            .map(SystemAgentResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public boolean isActive(String code) {
        return systemAgentRepository.findByCodeIgnoreCase(code)
            .map(SystemAgent::isActive)
            .orElse(true);
    }

    @Transactional
    public SystemAgentResponse create(SaveSystemAgentRequest request) {
        String code = normalizeCode(request.code());
        if (systemAgentRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Agent code already exists.");
        }
        return SystemAgentResponse.from(systemAgentRepository.save(new SystemAgent(
            code,
            request.name().trim(),
            request.taskType().trim().toUpperCase(),
            request.description().trim(),
            cleanOptional(request.instructions()),
            request.active()
        )));
    }

    @Transactional
    public SystemAgentResponse update(UUID agentId, SaveSystemAgentRequest request) {
        SystemAgent agent = systemAgentRepository.findById(agentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found."));
        String code = normalizeCode(request.code());
        systemAgentRepository.findByCodeIgnoreCase(code)
            .filter(existing -> !existing.getId().equals(agentId))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Agent code already exists.");
            });
        agent.update(
            code,
            request.name().trim(),
            request.taskType().trim().toUpperCase(),
            request.description().trim(),
            cleanOptional(request.instructions()),
            request.active()
        );
        return SystemAgentResponse.from(agent);
    }

    @Transactional
    public SystemAgentResponse toggle(UUID agentId, boolean active) {
        SystemAgent agent = systemAgentRepository.findById(agentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found."));
        agent.setActive(active);
        return SystemAgentResponse.from(agent);
    }

    private String normalizeCode(String code) {
        return code.trim().replaceAll("[^A-Za-z0-9_\\-]", "_").toUpperCase();
    }

    private String cleanOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
