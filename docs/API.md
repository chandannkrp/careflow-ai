# CareFlow AI API

This document tracks the API surface as each slice is implemented.

## Base URL

- Local backend: `http://localhost:8080`
- Frontend dev server proxies `/api` to `http://localhost:8080`.

## Queue

### `GET /api/queue`

Returns the current treatment queue sorted by backend priority rules.

Query parameters:

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `category` | `CRITICAL` \| `HIGH` \| `MEDIUM` \| `LOW` | No | Filters by urgency category. |
| `department` | string | No | Case-insensitive exact department match. |
| `status` | `WAITING` \| `IN_TRIAGE` \| `IN_TREATMENT` \| `DISCHARGED` \| `LEFT_WITHOUT_BEING_SEEN` | No | Filters by queue status. |

Response: `200 OK`

```json
[
  {
    "patientId": "00000000-0000-0000-0000-000000000000",
    "intakeId": "00000000-0000-0000-0000-000000000000",
    "patientDisplayId": "ER-1042",
    "urgencyCategory": "HIGH",
    "urgencyScore": 82,
    "chiefComplaint": "Chest pain",
    "waitingMinutes": 18,
    "department": "Emergency",
    "status": "WAITING",
    "staffEscalated": false,
    "waitingSince": "2026-06-23T07:00:00Z"
  }
]
```

Frontend consumer:

- `frontend/src/api/client.ts` exposes `getQueueEntries()`.
- `frontend/src/features/queue/QueueTable.tsx` renders loading, error, empty, filters, sort controls, search matching, status actions, and table states.

### `POST /api/queue/{patientId}/status`

Updates the queue status for a patient. The MVP UI uses this to move a patient into treatment once care has started.

Request:

```json
{
  "status": "IN_TREATMENT",
  "actorName": "Demo Triage",
  "actorRole": "TRIAGE_NURSE"
}
```

Response: `200 OK`

Returns the updated queue entry using the same shape as `GET /api/queue`.

Frontend consumer:

- `frontend/src/api/client.ts` exposes `updateQueueStatus()`.
- `frontend/src/features/queue/QueueTable.tsx` provides a status dropdown and one-click treatment start action.

## Intake

### `POST /api/intakes`

Creates a patient intake, runs deterministic urgency scoring, and inserts or refreshes the treatment queue entry.

Request:

```json
{
  "patientDisplayId": "ER-1048",
  "ageBand": "ADULT",
  "arrivalTimestamp": "2026-06-24T09:30:00Z",
  "arrivalMode": "WALK_IN",
  "chiefComplaint": "Chest pain",
  "symptomNotes": "Pressure-like pain for 30 minutes.",
  "structuredSymptoms": ["chest pain", "nausea"],
  "painLevel": 7,
  "vitals": {
    "temperatureC": 37.1,
    "heartRate": 112,
    "systolicPressure": 138,
    "diastolicPressure": 86,
    "respiratoryRate": 22,
    "oxygenSaturation": 96
  },
  "riskFlags": {
    "chestPain": true,
    "breathingDifficulty": false,
    "alteredMentalState": false,
    "severeBleeding": false,
    "pregnancy": false,
    "pediatricRisk": false,
    "fallOrTrauma": false,
    "immunocompromised": false
  },
  "department": "Emergency",
  "currentStatus": "WAITING",
  "staffNotes": "Placed in monitored waiting area.",
  "actorName": "Demo Intake",
  "actorRole": "INTAKE_STAFF"
}
```

Response: `200 OK`

```json
{
  "intakeId": "00000000-0000-0000-0000-000000000000",
  "patientId": "00000000-0000-0000-0000-000000000000",
  "patientDisplayId": "ER-1048",
  "ageBand": "ADULT",
  "arrivalTimestamp": "2026-06-24T09:30:00Z",
  "arrivalMode": "WALK_IN",
  "chiefComplaint": "Chest pain",
  "symptomNotes": "Pressure-like pain for 30 minutes.",
  "structuredSymptoms": ["chest pain", "nausea"],
  "painLevel": 7,
  "department": "Emergency",
  "currentStatus": "WAITING",
  "assessment": {
    "finalCategory": "HIGH",
    "finalScore": 80,
    "scoreFactors": ["Baseline intake priority: +10"]
  },
  "createdAt": "2026-06-24T09:31:00Z"
}
```

Frontend consumer:

- `frontend/src/api/client.ts` exposes `createIntake()`.
- `frontend/src/features/intake/IntakeForm.tsx` renders the intake form and refreshes the queue after a successful save.
- The UI labels the persisted `painLevel` score as clinical distress and maps it to the existing deterministic scoring field.

## Departments

### `GET /api/departments`

Returns department names for intake dropdowns. The response combines default MVP care areas with distinct departments currently present in queue entries.

Response: `200 OK`

```json
["Emergency", "Pediatrics", "Orthopedics", "General", "Cardiology", "Neurology"]
```

Frontend consumer:

- `frontend/src/api/client.ts` exposes `getDepartments()`.
- `frontend/src/app/App.tsx` loads departments when the page mounts and passes them to the intake form.

## Staff

### `GET /api/staff/{staffLookup}`

Fetches staff details by UUID or memorable staff code so the workspace can populate actor name, role, and department.

Response: `200 OK`

```json
{
  "id": "88888888-8888-8888-8888-888888888888",
  "staffCode": "TRIAGE-01",
  "displayName": "Omar Reed",
  "role": "TRIAGE_NURSE",
  "department": "Emergency",
  "active": true
}
```

Frontend consumer:

- `frontend/src/api/client.ts` exposes `getStaffUser()`.
- `frontend/src/app/App.tsx` auto-fetches staff details once a staff code such as `TRIAGE-01` or a full UUID is entered.

## Metrics

### `GET /api/metrics/queue`

Returns queue load and action metrics for the dashboard.

Frontend consumer:

- `frontend/src/api/client.ts` exposes `getQueueMetrics()`.
- `frontend/src/features/metrics/MetricsDashboard.tsx` renders queue load, critical/high waiting, longest high wait, override count, radar charts, and wait-time graph charts.

## AI

### OpenAI advisory assessment

When `OPENAI_ENABLED=true` and `OPENAI_API_KEY` is configured, intake assessment attaches advisory AI fields to `UrgencyAssessment`:

- suggested category and score
- red flags
- missing or ambiguous details
- structured symptom summary
- staff-facing explanation
- confidence level

Deterministic scoring still owns `finalCategory`, `finalScore`, and queue ordering. If the OpenAI request fails or is disabled, intake creation and assessment still complete with a low-confidence fallback advisory message.

Configuration:

| Name | Default | Notes |
| --- | --- | --- |
| `OPENAI_ENABLED` | `false` | Enables outbound OpenAI calls. |
| `OPENAI_API_KEY` | empty | Required when enabled. |
| `OPENAI_MODEL` | `gpt-4.1-mini` | Model used by the Responses API client. |

### `POST /api/ai/chat`

Runs the bottom-right staff AI agent chat. The agent answers operational queue/intake questions and may return suggested UI actions such as refreshing the queue.

The frontend uses a short request timeout and falls back to local workflow guidance if the backend or OpenAI request is slow. The backend also applies outbound OpenAI timeouts and returns deterministic fallback guidance when OpenAI is disabled or unavailable.

Request:

```json
{
  "message": "Which urgent patients should I review first?",
  "actorName": "Omar Reed",
  "actorRole": "TRIAGE_NURSE"
}
```

Response: `200 OK`

```json
{
  "message": "There are 3 critical or high-priority patients waiting. Review the Critical and High filter first.",
  "suggestedActions": ["filter_critical_high"],
  "aiBacked": false,
  "createdAt": "2026-06-24T08:15:00Z"
}
```

Frontend consumer:

- `frontend/src/features/ai-chat/AiAgentChat.tsx` renders the floating AI agent chat.
- `frontend/src/api/client.ts` exposes `sendAiChatMessage()`.

Seed data:

- `backend/src/main/resources/db/migration/V2__seed_demo_queue.sql` creates six demo queue rows for local MVP checks.
- `backend/src/main/resources/db/migration/V3__seed_demo_staff.sql` creates three demo staff users for local staff lookup checks.
- `backend/src/main/resources/db/migration/V4__add_staff_codes.sql` adds memorable staff codes such as `INTAKE-01`, `TRIAGE-01`, and `CHARGE-01`.
