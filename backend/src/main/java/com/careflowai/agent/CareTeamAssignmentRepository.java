package com.careflowai.agent;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareTeamAssignmentRepository extends JpaRepository<CareTeamAssignment, UUID> {
    Optional<CareTeamAssignment> findTopByPatientIdAndActiveTrueOrderByAssignedAtDesc(UUID patientId);

    List<CareTeamAssignment> findByPatientIdAndActiveTrue(UUID patientId);

    List<CareTeamAssignment> findByActiveTrueOrderByAssignedAtDesc();
}
