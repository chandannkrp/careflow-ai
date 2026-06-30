package com.careflowai.agent;

import com.careflowai.common.QueueStatus;
import com.careflowai.common.StaffRole;
import com.careflowai.common.UrgencyCategory;
import com.careflowai.intake.Intake;
import com.careflowai.patient.Patient;
import com.careflowai.staff.StaffUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.UUID;

@Entity
public class PatientFlashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Patient patient;

    @ManyToOne(optional = false)
    private Intake intake;

    @ManyToOne
    private StaffUser assignedStaff;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffRole audienceRole;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(nullable = false)
    private String actionLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyCategory urgencyCategory;

    @Column(nullable = false)
    private int urgencyScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean resolved;

    private String resolvedBy;

    private Instant resolvedAt;

    protected PatientFlashcard() {
    }

    public PatientFlashcard(Patient patient, Intake intake, StaffUser assignedStaff, StaffRole audienceRole) {
        this.patient = patient;
        this.intake = intake;
        this.assignedStaff = assignedStaff;
        this.audienceRole = audienceRole;
        this.department = intake.getDepartment();
    }

    public void refresh(String title, String summary, String actionLabel, UrgencyCategory urgencyCategory,
                        int urgencyScore, QueueStatus status) {
        this.title = title;
        this.summary = summary;
        this.actionLabel = actionLabel;
        this.urgencyCategory = urgencyCategory;
        this.urgencyScore = urgencyScore;
        this.status = status;
        this.department = intake.getDepartment();
        if (status != QueueStatus.DISCHARGED && status != QueueStatus.LEFT_WITHOUT_BEING_SEEN) {
            this.resolved = false;
            this.resolvedBy = null;
            this.resolvedAt = null;
        }
    }

    public void resolve(String staffName) {
        this.resolved = true;
        this.resolvedBy = staffName;
        this.resolvedAt = Instant.now();
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

    public StaffUser getAssignedStaff() {
        return assignedStaff;
    }

    public StaffRole getAudienceRole() {
        return audienceRole;
    }

    public String getDepartment() {
        return department;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public UrgencyCategory getUrgencyCategory() {
        return urgencyCategory;
    }

    public int getUrgencyScore() {
        return urgencyScore;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isResolved() {
        return resolved;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
