package com.careflowai.agent;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientTimelineEventRepository extends JpaRepository<PatientTimelineEvent, UUID> {
    List<PatientTimelineEvent> findByPatientIdInOrderByCreatedAtDesc(Collection<UUID> patientIds, Pageable pageable);

    List<PatientTimelineEvent> findByDepartmentIgnoreCaseOrderByCreatedAtDesc(String department, Pageable pageable);

    List<PatientTimelineEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
