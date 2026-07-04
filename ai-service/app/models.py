from datetime import datetime, timezone
from typing import Any

from pydantic import BaseModel, Field


class PatientDetails(BaseModel):
    patient_id: str | None = Field(default=None, alias='patientId')
    intake_id: str | None = Field(default=None, alias='intakeId')
    patient_display_id: str | None = Field(default=None, alias='patientDisplayId')
    age_band: str | None = Field(default=None, alias='ageBand')
    arrival_timestamp: str | None = Field(default=None, alias='arrivalTimestamp')
    arrival_mode: str | None = Field(default=None, alias='arrivalMode')
    chief_complaint: str | None = Field(default=None, alias='chiefComplaint')
    symptom_notes: str | None = Field(default=None, alias='symptomNotes')
    structured_symptoms: list[str] = Field(default_factory=list, alias='structuredSymptoms')
    pain_level: int | None = Field(default=None, alias='painLevel')
    vitals: dict[str, Any] = Field(default_factory=dict)
    risk_flags: dict[str, Any] = Field(default_factory=dict, alias='riskFlags')
    department: str | None = None
    current_status: str | None = Field(default=None, alias='currentStatus')
    staff_notes: str | None = Field(default=None, alias='staffNotes')
    assessment: dict[str, Any] | None = None
    raw: dict[str, Any] = Field(default_factory=dict)

    model_config = {
        'populate_by_name': True,
        'extra': 'allow',
    }

    @property
    def document_id(self) -> str:
        return self.patient_display_id or self.patient_id or self.intake_id or 'unknown-patient'

    def searchable_text(self) -> str:
        vitals = ', '.join(f'{key}: {value}' for key, value in self.vitals.items() if value not in (None, ''))
        risks = ', '.join(f'{key}: {value}' for key, value in self.risk_flags.items() if value is True)
        assessment = self.assessment or {}
        score_factors = assessment.get('scoreFactors') or assessment.get('score_factors') or []
        return '\n'.join(
            item for item in [
                f'Patient: {self.patient_display_id or self.patient_id or "unknown"}',
                f'Age band: {self.age_band}',
                f'Department: {self.department}',
                f'Status: {self.current_status}',
                f'Arrival: {self.arrival_timestamp} via {self.arrival_mode}',
                f'Chief complaint: {self.chief_complaint}',
                f'Symptoms: {", ".join(self.structured_symptoms) if self.structured_symptoms else "none recorded"}',
                f'Notes: {self.symptom_notes}',
                f'Pain level: {self.pain_level}',
                f'Vitals: {vitals or "none recorded"}',
                f'Risk flags: {risks or "none recorded"}',
                f'Urgency: {assessment.get("finalCategory") or assessment.get("final_category")}',
                f'Urgency score: {assessment.get("finalScore") or assessment.get("final_score")}',
                f'Score factors: {", ".join(score_factors)}',
                f'Staff notes: {self.staff_notes}',
            ]
            if item and not item.endswith(': None')
        )


class PatientIngestRequest(BaseModel):
    patient: PatientDetails


class PatientMatch(BaseModel):
    patient_display_id: str | None = Field(default=None, alias='patientDisplayId')
    patient_id: str | None = Field(default=None, alias='patientId')
    intake_id: str | None = Field(default=None, alias='intakeId')
    score: float
    summary: str
    details: dict[str, Any]

    model_config = {
        'populate_by_name': True,
    }


class ChatRequest(BaseModel):
    message: str
    actor_name: str | None = Field(default=None, alias='actorName')
    actor_role: str | None = Field(default=None, alias='actorRole')
    context: dict[str, Any] = Field(default_factory=dict)

    model_config = {
        'populate_by_name': True,
    }


class ChatResponse(BaseModel):
    message: str
    suggested_actions: list[str] = Field(default_factory=list, alias='suggestedActions')
    ai_backed: bool = Field(alias='aiBacked')
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc), alias='createdAt')
    matched_patients: list[PatientMatch] = Field(default_factory=list, alias='matchedPatients')

    model_config = {
        'populate_by_name': True,
    }


class HealthResponse(BaseModel):
    status: str
    openai_enabled: bool = Field(alias='openaiEnabled')
    amqp_enabled: bool = Field(alias='amqpEnabled')
    indexed_patients: int = Field(alias='indexedPatients')

    model_config = {
        'populate_by_name': True,
    }
