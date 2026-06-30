package com.careflowai.intake;

import com.careflowai.common.ArrivalMode;
import com.careflowai.common.QueueStatus;
import com.careflowai.patient.Patient;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Intake {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Patient patient;

    @Column(nullable = false)
    private Instant arrivalTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArrivalMode arrivalMode;

    @Column(nullable = false)
    private String chiefComplaint;

    @Column(columnDefinition = "text")
    private String symptomNotes;

    @ElementCollection
    @CollectionTable(name = "intake_structured_symptoms", joinColumns = @JoinColumn(name = "intake_id"))
    @Column(name = "symptom", nullable = false)
    private List<String> structuredSymptoms = new ArrayList<>();

    @Column(nullable = false)
    private int painLevel;

    @Embedded
    private Vitals vitals = new Vitals();

    @Embedded
    private RiskFlags riskFlags = new RiskFlags();

    @Column(nullable = false)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus currentStatus;

    @Column(columnDefinition = "text")
    private String staffNotes;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Intake() {
    }

    public Intake(Patient patient, Instant arrivalTimestamp, ArrivalMode arrivalMode, String chiefComplaint,
                  String symptomNotes, List<String> structuredSymptoms, int painLevel, Vitals vitals,
                  RiskFlags riskFlags, String department, QueueStatus currentStatus, String staffNotes) {
        this.patient = patient;
        this.arrivalTimestamp = arrivalTimestamp;
        this.arrivalMode = arrivalMode;
        this.chiefComplaint = chiefComplaint;
        this.symptomNotes = symptomNotes;
        this.structuredSymptoms = structuredSymptoms == null ? new ArrayList<>() : new ArrayList<>(structuredSymptoms);
        this.painLevel = painLevel;
        this.vitals = vitals == null ? new Vitals() : vitals;
        this.riskFlags = riskFlags == null ? new RiskFlags() : riskFlags;
        this.department = department;
        this.currentStatus = currentStatus;
        this.staffNotes = staffNotes;
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

    public Instant getArrivalTimestamp() {
        return arrivalTimestamp;
    }

    public ArrivalMode getArrivalMode() {
        return arrivalMode;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public String getSymptomNotes() {
        return symptomNotes;
    }

    public List<String> getStructuredSymptoms() {
        return structuredSymptoms;
    }

    public int getPainLevel() {
        return painLevel;
    }

    public Vitals getVitals() {
        return vitals;
    }

    public RiskFlags getRiskFlags() {
        return riskFlags;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public QueueStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(QueueStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public void replaceStructuredSymptoms(List<String> structuredSymptoms) {
        this.structuredSymptoms.clear();
        if (structuredSymptoms != null) {
            this.structuredSymptoms.addAll(structuredSymptoms);
        }
    }

    public String getStaffNotes() {
        return staffNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
