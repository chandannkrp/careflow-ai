package com.careflowai.patient;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByDisplayId(String displayId);

    long countByDisplayIdStartingWith(String displayIdPrefix);
}
