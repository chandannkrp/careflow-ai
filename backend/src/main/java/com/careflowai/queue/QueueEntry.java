package com.careflowai.queue;

import com.careflowai.common.QueueStatus;
import com.careflowai.common.UrgencyCategory;
import com.careflowai.intake.Intake;
import com.careflowai.patient.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.UUID;

@Entity
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false)
    private Patient patient;

    @ManyToOne(optional = false)
    private Intake intake;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyCategory urgencyCategory;

    @Column(nullable = false)
    private int urgencyScore;

    @Column(nullable = false)
    private Instant waitingSince;

    @Column(nullable = false)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus status;

    @Column(nullable = false)
    private boolean staffEscalated;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected QueueEntry() {
    }

    public QueueEntry(Patient patient, Intake intake, UrgencyCategory urgencyCategory, int urgencyScore,
                      Instant waitingSince, String department, QueueStatus status) {
        this.patient = patient;
        this.intake = intake;
        this.urgencyCategory = urgencyCategory;
        this.urgencyScore = urgencyScore;
        this.waitingSince = waitingSince;
        this.department = department;
        this.status = status;
    }

    public void applyCalculatedPriority(UrgencyCategory urgencyCategory, int urgencyScore, Intake intake) {
        if (!staffEscalated) {
            this.urgencyCategory = urgencyCategory;
            this.urgencyScore = urgencyScore;
        }
        this.intake = intake;
        this.department = intake.getDepartment();
        this.status = intake.getCurrentStatus();
    }

    public void applyStaffOverride(UrgencyCategory urgencyCategory, int urgencyScore) {
        this.urgencyCategory = urgencyCategory;
        this.urgencyScore = urgencyScore;
        this.staffEscalated = true;
    }

    public void updateStatus(QueueStatus status) {
        this.status = status;
        this.intake.setCurrentStatus(status);
    }

    public void updatePlacement(QueueStatus status, String department) {
        this.status = status;
        this.department = department;
        this.intake.setCurrentStatus(status);
        this.intake.setDepartment(department);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
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

    public UrgencyCategory getUrgencyCategory() {
        return urgencyCategory;
    }

    public int getUrgencyScore() {
        return urgencyScore;
    }

    public Instant getWaitingSince() {
        return waitingSince;
    }

    public String getDepartment() {
        return department;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public boolean isStaffEscalated() {
        return staffEscalated;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
