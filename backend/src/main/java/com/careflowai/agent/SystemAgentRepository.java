package com.careflowai.agent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemAgentRepository extends JpaRepository<SystemAgent, UUID> {
    Optional<SystemAgent> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<SystemAgent> findAllByOrderByTaskTypeAscNameAsc();
}
