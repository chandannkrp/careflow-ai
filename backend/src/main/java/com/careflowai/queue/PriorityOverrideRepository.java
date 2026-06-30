package com.careflowai.queue;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriorityOverrideRepository extends JpaRepository<PriorityOverride, UUID> {
}
