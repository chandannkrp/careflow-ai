# CareFlow AI Architecture

This document records the implemented architecture as the MVP grows one slice at a time.

## Backend

The backend is a Spring Boot modular monolith in `backend/src/main/java/com/careflowai`.

Current modules:

| Module | Responsibility |
| --- | --- |
| `intake` | Patient intake data, vitals, risk flags, and intake creation flow. |
| `assessment` | Deterministic urgency scoring and assessment persistence. |
| `queue` | Queue ordering, status updates, and priority overrides. |
| `metrics` | Queue metrics responses. |
| `patient` | Patient identity records. |
| `staff` | Staff actor resolution and role-aware permissions. |
| `common` | Shared enums for roles, statuses, categories, and intake values. |

Queue ordering is owned by `QueueService.queueComparator(...)`. The frontend only displays the ordered API response.

Database schema is managed by Flyway migrations in `backend/src/main/resources/db/migration`.

## Frontend

The frontend is a Vite React app in `frontend/src`.

Current structure:

| Path | Responsibility |
| --- | --- |
| `app/` | Application shell and top-level screen composition. |
| `api/` | HTTP client helpers and backend API calls. |
| `features/queue/` | Queue table, filters, sort controls, and queue status actions. |
| `features/intake/` | Patient intake form and create-intake workflow. |
| `features/ai-chat/` | Floating staff AI agent chat. |
| `features/patient-detail/` | Placeholder for patient detail and audit views. |
| `features/override/` | Placeholder for priority override workflow. |
| `features/metrics/` | Recharts-based queue analytics dashboard. |
| `types/` | Shared TypeScript types matching API contracts. |
| `styles/` | Tailwind and global CSS. |

## Implemented Slice: Intake And Queue Operations

The current UI slice supports patient intake, queue operations, and operational analytics:

1. `IntakeForm` calls `createIntake()` with required intake fields, optional vitals, risk flags, and staff notes.
2. `App` uses hash routes (`#/queue`, `#/intake`, `#/dashboard`) to switch workspace views without scroll-section navigation.
3. `App` fetches `/api/departments` on load and passes the result to `IntakeForm` for the department dropdown.
4. `App` fetches `/api/staff/{staffLookup}` when a staff code or UUID is entered, then passes the active staff actor to intake, queue actions, and AI chat.
5. `POST /api/intakes` creates the patient, persists intake details, runs deterministic scoring, and creates the queue entry.
6. `AiAssessmentService` optionally attaches OpenAI advisory output to assessments while deterministic scoring stays authoritative for final queue state.
7. `QueueTable` calls `getQueueEntries(filters)` for backend category, department, and status filters.
8. `QueueTable` applies local search and sort options for score, wait time, patient ID, department, or status while keeping backend queue-rule order as the default.
9. Staff can change queue status from the table, including a one-click move to `IN_TREATMENT` when treatment starts.
10. `MetricsDashboard` calls `getQueueMetrics()` and renders Recharts radar and line charts.
11. `AiAgentChat` calls `POST /api/ai/chat` for staff operations questions and suggested UI actions.
12. Flyway creates the core schema and seeds demo queue and staff data.
13. Vite proxies `/api` to the local backend during development.

The top-level frontend shell uses a draggable side navigation with logo, queue search, route buttons, and staff lookup. The main workspace renders one route at a time, with the patient queue as the default route.

The next slice should stay similarly small: patient detail, priority override UI, or audit history.
