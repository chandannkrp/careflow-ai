package com.careflowai.agent;

import com.careflowai.common.StaffRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientFlashcardRepository extends JpaRepository<PatientFlashcard, UUID> {
    Optional<PatientFlashcard> findTopByPatientIdAndAudienceRoleAndAssignedStaffIdOrderByUpdatedAtDesc(
        UUID patientId,
        StaffRole audienceRole,
        UUID assignedStaffId
    );

    Optional<PatientFlashcard> findTopByPatientIdAndAudienceRoleAndDepartmentIgnoreCaseOrderByUpdatedAtDesc(
        UUID patientId,
        StaffRole audienceRole,
        String department
    );

    List<PatientFlashcard> findByAssignedStaffIdOrderByUpdatedAtDesc(UUID assignedStaffId, Pageable pageable);

    List<PatientFlashcard> findByAudienceRoleAndDepartmentIgnoreCaseOrderByUpdatedAtDesc(
        StaffRole audienceRole,
        String department,
        Pageable pageable
    );

    List<PatientFlashcard> findByDepartmentIgnoreCaseOrderByUpdatedAtDesc(String department, Pageable pageable);

    List<PatientFlashcard> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
