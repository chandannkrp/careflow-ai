package com.careflowai.patient;

import com.careflowai.common.AgeBand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String displayId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgeBand ageBand;

    @Column(columnDefinition = "text")
    private String contactMetadata;

    @Column(nullable = false)
    private Instant createdAt;

    protected Patient() {
    }

    public Patient(String displayId, AgeBand ageBand) {
        this.displayId = displayId;
        this.ageBand = ageBand;
    }

    public void updateContactMetadata(String contactMetadata) {
        this.contactMetadata = contactMetadata;
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

    public String getDisplayId() {
        return displayId;
    }

    public AgeBand getAgeBand() {
        return ageBand;
    }

    public String getContactMetadata() {
        return contactMetadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
