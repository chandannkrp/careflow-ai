package com.careflowai.thread;

import com.careflowai.intake.Intake;
import com.careflowai.patient.Patient;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class PatientThreadComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Patient patient;

    @ManyToOne(optional = false)
    private Intake intake;

    @Column(nullable = false)
    private String authorName;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientThreadAttachment> attachments = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    protected PatientThreadComment() {
    }

    public PatientThreadComment(Patient patient, Intake intake, String authorName, String body) {
        this.patient = patient;
        this.intake = intake;
        this.authorName = authorName;
        this.body = body;
    }

    public void addAttachment(PatientThreadAttachment attachment) {
        attachments.add(attachment);
        attachment.attachTo(this);
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

    public Intake getIntake() {
        return intake;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getBody() {
        return body;
    }

    public List<PatientThreadAttachment> getAttachments() {
        return attachments;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
