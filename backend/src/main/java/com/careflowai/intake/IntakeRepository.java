package com.careflowai.intake;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntakeRepository extends JpaRepository<Intake, UUID> {
    Optional<Intake> findTopByPatientIdOrderByCreatedAtDesc(UUID patientId);
}
