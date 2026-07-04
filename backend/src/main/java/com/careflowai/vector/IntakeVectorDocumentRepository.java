package com.careflowai.vector;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntakeVectorDocumentRepository extends JpaRepository<IntakeVectorDocument, UUID> {
    Optional<IntakeVectorDocument> findByIntakeId(UUID intakeId);
}
