package com.careflowai.vector;

import com.careflowai.intake.Intake;
import com.careflowai.patient.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.UUID;

@Entity
public class IntakeVectorDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false)
    private Intake intake;

    @ManyToOne(optional = false)
    private Patient patient;

    @Column(nullable = false)
    private String patientDisplayId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, columnDefinition = "text")
    private String embedding;

    @Column(nullable = false)
    private Instant updatedAt;

    protected IntakeVectorDocument() {
    }

    public IntakeVectorDocument(Intake intake, Patient patient, String patientDisplayId, String content, String embedding) {
        this.intake = intake;
        this.patient = patient;
        this.patientDisplayId = patientDisplayId;
        this.content = content;
        this.embedding = embedding;
    }

    public void refresh(String content, String embedding) {
        this.patientDisplayId = intake.getPatient().getDisplayId();
        this.content = content;
        this.embedding = embedding;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Intake getIntake() {
        return intake;
    }

    public Patient getPatient() {
        return patient;
    }

    public String getPatientDisplayId() {
        return patientDisplayId;
    }

    public String getContent() {
        return content;
    }

    public String getEmbedding() {
        return embedding;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
