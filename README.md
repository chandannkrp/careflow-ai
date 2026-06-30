# CareFlow AI MVP

CareFlow AI is split into two top-level modules:

- `backend/`: Spring Boot API foundation for intake, urgency scoring, queue operations, overrides, and metrics.
- `frontend/`: React, TypeScript, Tailwind, and Vite staff workspace.

The project is being implemented phase by phase from `PROJECT_PLAN.md`. The current MVP slice includes patient intake, deterministic urgency scoring, optional OpenAI advisory assessment, a searchable and filterable treatment queue, queue status updates, staff lookup, department lookup, Recharts analytics, and a staff AI chat.

## Module Layout

```text
backend/
  pom.xml
  src/main/java/com/careflowai/
  src/main/resources/application.yml

frontend/
  package.json
  src/app/
  src/api/
  src/components/
  src/features/
  src/styles/
  src/types/
```

## Current UI

- Draggable side navigation with logo, queue search, route-style workspace buttons, and staff lookup.
- Patient queue as the primary workspace view with filters, sort controls, loading skeletons, and status actions.
- Intake form with fetched department dropdowns, clinical distress selection, vitals, risk flags, and staff actor details.
- Dashboard metrics with radar and graph charts for queue load, urgency distribution, wait times, and override count.
- Bottom-right AI agent chat for staff operations questions and suggested workspace actions.

## Demo Staff IDs

- `INTAKE-01`: Intake staff.
- `TRIAGE-01`: Triage nurse.
- `CHARGE-01`: Charge nurse.

## Optional AI Configuration

Set these before running the backend to enable OpenAI-backed advisory assessment and staff chat:

- `OPENAI_ENABLED=true`
- `OPENAI_API_KEY=<your key>`
- `OPENAI_MODEL=gpt-4.1-mini`
