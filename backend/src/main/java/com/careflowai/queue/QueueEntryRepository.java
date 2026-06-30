package com.careflowai.queue;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, UUID> {
    Optional<QueueEntry> findByPatientId(UUID patientId);

    @Query("select distinct entry.department from QueueEntry entry order by entry.department")
    java.util.List<String> findDistinctDepartments();
}
