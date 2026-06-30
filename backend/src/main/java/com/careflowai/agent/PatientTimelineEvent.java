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
public class PatientTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Patient patient;

    @ManyToOne(optional = false)
    private Intake intake;

    @ManyToOne
    private StaffUser actorStaffUser;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private Instant createdAt;

    protected PatientTimelineEvent() {
    }

    public PatientTimelineEvent(Patient patient, Intake intake, StaffUser actorStaffUser, String eventType,
                                String title, String description, String source) {
        this.patient = patient;
        this.intake = intake;
        this.actorStaffUser = actorStaffUser;
        this.department = intake.getDepartment();
        this.eventType = eventType;
        this.title = title;
        this.description = description;
        this.source = source;
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

    public StaffUser getActorStaffUser() {
        return actorStaffUser;
    }

    public String getDepartment() {
        return department;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
