package com.careflowai.thread;

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
public class PatientThreadAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private PatientThreadComment comment;

    @Column(nullable = false)
    private String fileName;

    private String fileType;

    @Column(nullable = false, columnDefinition = "text")
    private String url;

    @Column(nullable = false)
    private Instant createdAt;

    protected PatientThreadAttachment() {
    }

    public PatientThreadAttachment(String fileName, String fileType, String url) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.url = url;
    }

    void attachTo(PatientThreadComment comment) {
        this.comment = comment;
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

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getUrl() {
        return url;
    }
}
