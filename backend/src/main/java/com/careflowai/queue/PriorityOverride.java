package com.careflowai.queue;

import com.careflowai.common.OverrideReason;
import com.careflowai.common.UrgencyCategory;
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
import java.time.Instant;
import java.util.UUID;

@Entity
public class PriorityOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Patient patient;

    @ManyToOne(optional = false)
    private StaffUser staffUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyCategory previousCategory;

    @Column(nullable = false)
    private int previousScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyCategory newCategory;

    @Column(nullable = false)
    private int newScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OverrideReason reason;

    @Column(columnDefinition = "text")
    private String note;

    @Column(nullable = false)
    private Instant createdAt;

    protected PriorityOverride() {
    }

    public PriorityOverride(Patient patient, StaffUser staffUser, UrgencyCategory previousCategory, int previousScore,
                            UrgencyCategory newCategory, int newScore, OverrideReason reason, String note) {
        this.patient = patient;
        this.staffUser = staffUser;
        this.previousCategory = previousCategory;
        this.previousScore = previousScore;
        this.newCategory = newCategory;
        this.newScore = newScore;
        this.reason = reason;
        this.note = note;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public StaffUser getStaffUser() {
        return staffUser;
    }

    public UrgencyCategory getPreviousCategory() {
        return previousCategory;
    }

    public int getPreviousScore() {
        return previousScore;
    }

    public UrgencyCategory getNewCategory() {
        return newCategory;
    }

    public int getNewScore() {
        return newScore;
    }

    public OverrideReason getReason() {
        return reason;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
