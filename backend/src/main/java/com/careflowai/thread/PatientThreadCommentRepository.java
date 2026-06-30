package com.careflowai.thread;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientThreadCommentRepository extends JpaRepository<PatientThreadComment, UUID> {
    @EntityGraph(attributePaths = "attachments")
    List<PatientThreadComment> findByPatientIdOrderByCreatedAtAsc(UUID patientId);
}
