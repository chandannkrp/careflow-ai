package com.careflowai.agent;

import com.careflowai.intake.Intake;
import com.careflowai.patient.Patient;
import com.careflowai.staff.StaffUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.UUID;

@Entity
public class CareTeamAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Patient patient;

    @ManyToOne(optional = false)
    private Intake intake;

    @ManyToOne(optional = false)
    private StaffUser assignedDoctor;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false, length = 500)
    private String assignmentReason;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant assignedAt;

    protected CareTeamAssignment() {
    }

    public CareTeamAssignment(Patient patient, Intake intake, StaffUser assignedDoctor, String assignmentReason) {
        this.patient = patient;
        this.intake = intake;
        this.assignedDoctor = assignedDoctor;
        this.department = intake.getDepartment();
        this.assignmentReason = assignmentReason;
    }

    @PrePersist
    void onCreate() {
        if (assignedAt == null) {
            assignedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public Intake getIntake() {
        return intake;
    }

    public StaffUser getAssignedDoctor() {
        return assignedDoctor;
    }

    public String getDepartment() {
        return department;
    }

    public String getAssignmentReason() {
        return assignmentReason;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
