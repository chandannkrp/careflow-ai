# CareFlow AI MVP Project Plan

## Summary

CareFlow AI MVP is a hospital operations tool that sorts patients into a treatment queue based on urgency. The goal is to help hospital staff see which patients need attention first, reduce waiting-room bottlenecks, and make prioritization more consistent and auditable.

This MVP does not attempt to automate the full patient journey yet. Referral management, insurance pre-authorization, smart scheduling, and follow-up reminders are future phases. The first build focuses only on patient intake, urgency assessment, queue ordering, staff review, and operational visibility.

The system uses React, TypeScript, and Tailwind for the frontend, Spring Boot for the backend, and a modular monolith architecture. OpenAI APIs can be used to convert patient symptoms and intake notes into structured urgency signals, but final queue rules and clinical overrides stay under backend and staff control.

## MVP Goal

Build a working patient urgency queue where hospital staff can:

- Register or import a patient intake.
- Capture symptoms, vitals, demographics, arrival time, and relevant risk factors.
- Generate an urgency score and priority category.
- Sort the treatment queue by urgency, waiting time, and escalation status.
- Review, override, and audit queue priority decisions.
- Track how long patients wait at each urgency level.

## Phase 0 MVP Definition

Phase 0 locks the emergency/triage MVP rules before implementation starts. These definitions are the source of truth for Phase 1 backend scaffolding and Phase 2 frontend workflows.

### Required Intake Fields

- Patient display ID
- Age band: child, adult, older adult
- Arrival timestamp
- Arrival mode: walk-in, ambulance, transfer, referral
- Chief complaint
- Free-text symptom notes
- Structured symptoms
- Clinical distress level: none, mild, moderate, or severe
- Vitals: temperature, heart rate, blood pressure, respiratory rate, oxygen saturation
- Risk flags: chest pain, breathing difficulty, altered mental state, severe bleeding, pregnancy, pediatric risk, fall/trauma, immunocompromised
- Department or care area
- Current queue status
- Staff notes

### AI-Assisted Assessment Output

The AI assessment is advisory and must be stored separately from final queue state.

- Suggested urgency category: Critical, High, Medium, or Low
- Suggested score: 0-100
- Red-flag indicators
- Missing or ambiguous intake details
- Structured symptom summary
- Staff-facing explanation
- Confidence level: low, medium, or high

### First Urgency Score

The first score is AI-assisted, but final queue ordering remains deterministic. The backend stores the AI suggestion, applies queue rules, and allows staff review.

- Critical: 90-100
- High: 70-89
- Medium: 40-69
- Low: 0-39

If AI assessment fails, staff can still create the intake and place the patient manually. Failed AI assessment must not block queue entry.

### Staff Roles And Permissions

- Intake Staff: create patient intake, edit incomplete intake fields, view queue.
- Triage Nurse: run urgency assessment, review AI output, override priority with reason, update queue status.
- Charge Nurse: all triage nurse permissions plus view metrics and audit history.
- Admin: manage demo users, view audit history, and configure demo scoring thresholds.

### Override Policy

Every priority override requires a reason and an audit record.

Required audit fields:

- Staff user
- Timestamp
- Patient display ID
- Previous urgency category and score
- New urgency category and score
- Override reason
- Optional note

Valid override reasons:

- New symptom information
- Updated vitals
- Staff clinical judgment
- Patient deterioration
- Patient condition improved
- Data entry correction

## Architecture

### Tech Stack

- Frontend: React, TypeScript, Tailwind CSS
- Backend: Spring Boot
- Architecture: Modular monolith
- Database: PostgreSQL
- AI platform: OpenAI APIs for structured symptom extraction and urgency signal summarization

### Core Backend Modules

- Patient Intake: captures patient arrival details, symptoms, vitals, and risk factors.
- Urgency Assessment: converts intake data into a priority score and urgency category.
- Queue Management: orders patients by urgency, wait time, and staff-defined escalation rules.
- Staff Review: allows nurses or authorized staff to confirm, override, or escalate priority.
- Audit and Compliance: records intake changes, AI outputs, score changes, overrides, and queue movements.
- Analytics: tracks wait time, queue volume, priority distribution, and operational bottlenecks.
- Identity and RBAC: restricts queue actions by staff role.

### Urgency Categories

- Critical: immediate attention required; highest queue priority.
- High: urgent care needed soon; should remain near the top of the queue.
- Medium: stable but should remain actively monitored.
- Low: non-urgent and can wait behind higher-priority cases.

The exact scoring rules should be configurable by hospital policy. The MVP should ship with a simple default score that combines symptoms, vitals, risk factors, arrival time, and staff escalation.

### AI Role

The AI layer supports the queue but does not make final clinical decisions.

- Symptom Structuring Agent: converts free-text symptoms into structured fields.
- Urgency Signal Agent: highlights red-flag symptoms, missing intake details, and possible escalation reasons.
- Queue Explanation Agent: explains why a patient appears at a specific priority level in staff-friendly language.

The backend remains the source of truth for urgency scoring, queue ordering, approvals, and audit logs. Any AI-generated priority recommendation must be reviewable and overrideable by authorized staff.

## Product Scope

### In Scope For MVP

- Patient intake form.
- Treatment queue sorted by urgency.
- Priority score and urgency category.
- Manual staff override with required reason.
- Queue filters by urgency, department, status, and waiting time.
- Patient detail panel with intake summary, score factors, and audit history.
- Dashboard metrics for wait times and queue load.
- Fake/demo patient dataset for testing.

### Out Of Scope For MVP

- Insurance pre-authorization.
- Referral coordination.
- Automated appointment scheduling.
- Automated patient messaging.
- EHR write-back.
- Real-time hospital device integration.
- Autonomous clinical diagnosis or treatment recommendation.

## Key Product Views

### Intake Screen

Staff can enter patient details, symptoms, vitals, arrival mode, risk flags, and notes. The screen should support quick entry because intake staff may be operating under time pressure.

### Urgency Queue

The main screen shows patients ordered by priority. Each row should show patient identifier, urgency category, score, chief complaint, waiting time, assigned department, status, and escalation indicator.

### Patient Detail Panel

Selecting a patient opens a detail view with structured intake, score breakdown, AI-generated summary, queue movement history, and staff notes.

### Override Workflow

Authorized staff can raise or lower priority with a required reason. The system records who changed it, when it changed, the old priority, the new priority, and the reason.

### Metrics Dashboard

Operations staff can view average wait time, patients by urgency category, longest waiting patients, queue throughput, and override frequency.

## Initial APIs

- `POST /api/intakes`: create a patient intake.
- `GET /api/intakes/{id}`: retrieve intake details.
- `POST /api/intakes/{id}/assess`: generate or refresh urgency assessment.
- `GET /api/queue`: return the sorted treatment queue.
- `POST /api/queue/{patientId}/override`: manually override urgency priority.
- `POST /api/queue/{patientId}/status`: update queue status.
- `GET /api/audit/patients/{patientId}`: return audit history.
- `GET /api/metrics/queue`: return queue performance metrics.

## Data Model

- Patient: identity, contact metadata, age band, and basic demographic fields.
- Intake: chief complaint, symptoms, vitals, arrival mode, notes, and intake timestamp.
- UrgencyAssessment: score, category, score factors, AI summary, and assessment timestamp.
- QueueEntry: patient, urgency category, score, waiting time, department, status, and escalation flag.
- PriorityOverride: old priority, new priority, reason, staff member, and timestamp.
- AuditEvent: actor, action, entity, before/after values, and timestamp.
- StaffUser: role, department, permissions, and status.

## Sorting Logic

The queue should sort patients using a deterministic backend rule:

1. Critical patients first.
2. Then High, Medium, and Low priority.
3. Within the same category, patients with staff escalation appear first.
4. Then sort by urgency score from highest to lowest.
5. Then sort by waiting time from longest to shortest.
6. Then sort by intake creation time.

AI output should never be the only sorting input. The backend should store score factors and apply explicit sorting rules so the result is explainable and testable.

## Fake Demo Patient Scenarios

The MVP demo dataset should include at least six fake patients:

- Critical: unstable vitals or severe distress; appears first in the queue.
- High: concerning symptoms such as chest pain or breathing difficulty; appears below Critical.
- Medium: stable patient with moderate symptoms and no immediate red flags.
- Low: minor complaint with normal vitals.
- Override Case: starts as Medium, then staff raises to High after updated vitals or new symptom information.
- AI Fallback Case: AI assessment fails, but staff manually assigns a category and the queue still works.

## Implementation Roadmap

### Current Implementation Status

- Patient intake creation is implemented through `POST /api/intakes` and the frontend intake form.
- New intakes automatically run deterministic urgency scoring and appear in the treatment queue.
- The queue table supports urgency, status, and department filters.
- The queue table supports staff-facing sort controls for score, wait time, patient ID, department, and status while preserving backend queue-rule order by default.
- Staff can change queue status from the table, including moving a patient to `In treatment` when treatment starts.
- Department options are fetched for intake dropdowns through `GET /api/departments`.
- Staff details are fetched by staff code or UUID through `GET /api/staff/{staffLookup}` and reused as the actor for intake, queue actions, and AI chat.
- Staff lookup now supports memorable staff codes such as `TRIAGE-01`.
- The frontend shell now includes draggable side navigation, route-style workspace views, queue search, and Recharts dashboard analytics.
- Phase 3 has started: OpenAI advisory assessment and a staff AI chat endpoint are implemented behind `OPENAI_ENABLED` and `OPENAI_API_KEY`.
- Phase 4 has started: the staff AI chat now has visible pending states, browser request timeouts, local workflow fallback, and backend OpenAI call timeouts.

### Phase 0: MVP Definition - Days 1-2

- Lock emergency/triage as the first care area.
- Lock AI-assisted scoring as advisory decision support.
- Lock required intake fields.
- Lock urgency categories and score ranges.
- Lock staff roles and override permissions.
- Lock deterministic queue sorting rules.
- Lock fake demo scenarios across all urgency levels.

### Phase 1: Backend Foundation - Days 3-7

- Scaffold the Spring Boot modular monolith.
- Add PostgreSQL schema for patients, intakes, urgency assessments, queue entries, overrides, audit events, and staff users.
- Implement intake creation and retrieval APIs.
- Implement deterministic urgency scoring.
- Implement sorted queue retrieval.
- Implement audit logging for intake, assessment, status, and override actions.

### Phase 2: Frontend MVP - Days 8-13

- Scaffold React, TypeScript, and Tailwind.
- Build intake form. [Implemented]
- Build urgency queue table with filters, search, sort, and status controls. [Implemented]
- Build patient detail panel.
- Build override workflow with required reason.
- Build basic queue metrics dashboard. [Implemented]

### Phase 3: OpenAI-Assisted Assessment - Days 14-18

- Add OpenAI API integration for symptom structuring.
- Add AI-generated urgency signal summary. [Implemented]
- Store AI outputs separately from deterministic score fields. [Implemented]
- Add guardrails so AI cannot directly change final priority without backend scoring and staff visibility. [Implemented]
- Add fallback behavior when the AI request fails. [Implemented]

### Phase 4: Hardening And Demo - Days 19-21

- Add validation, error states, loading states, and empty states. [In progress]
- Add seed/demo patient data.
- Prepare an MVP demo script showing patients entering the queue, priority ordering, AI summary, staff override, and metrics.

## Compliance, Controls, And Safety

- Use fake patient data during development and demos.
- Do not process real PHI until compliance, security, and legal approvals are complete.
- The system must not diagnose patients or recommend treatment plans.
- AI output must be framed as administrative decision support, not clinical authority.
- Staff must be able to review and override every urgency category.
- Every score, priority change, and override must be auditable.
- Use minimum necessary patient information in OpenAI API calls.
- Do not use live web search in patient workflows.
- Keep secrets out of frontend code and logs.

## Measurement KPIs

- Average patient wait time by urgency category.
- Longest current wait time by urgency category.
- Number of patients currently waiting.
- Number of Critical and High priority patients waiting.
- Time from intake creation to urgency assessment.
- Staff override rate.
- Percentage of patients reassessed after waiting threshold.
- Queue throughput per hour.
- Reduction in patients waiting beyond target threshold.

## Test Plan

### Functional Tests

- Staff can create an intake.
- The system generates an urgency assessment.
- The queue sorts patients in the expected priority order.
- Staff can update patient queue status.
- Staff can override priority with a required reason.
- Patient detail view shows intake, score factors, summary, and audit history.
- Metrics dashboard reflects queue state.

### Sorting Tests

- Critical patients always appear before High, Medium, and Low.
- High patients appear before Medium and Low.
- Staff-escalated patients appear before non-escalated patients in the same category.
- Higher score wins within the same category and escalation state.
- Longer waiting time wins when category and score are equal.
- Creation time is used as the final tie breaker.

### AI Workflow Tests

- Free-text symptoms are converted into structured fields.
- Missing or ambiguous symptoms are flagged for staff review.
- AI summary is stored but does not directly override deterministic scoring.
- AI failure does not block manual intake or queue placement.
- Prompt injection text does not bypass priority rules or staff review.

### Security And Compliance Tests

- Only authorized roles can override priority.
- Every override requires a reason.
- Audit events are recorded for intake creation, assessment, override, and status update.
- Secrets are not exposed in logs or frontend code.
- Fake data can be seeded without external integrations.

## Acceptance Criteria

- A staff user can create patients through intake and see them placed in a sorted treatment queue.
- Queue ordering is deterministic, explainable, and covered by tests.
- Staff can review and override urgency with an audit trail.
- AI can assist with symptom structuring and summaries without making final clinical decisions.
- The MVP can be demonstrated end to end using fake patient data.
- The plan remains compatible with future expansion into referrals, scheduling, pre-auth, and follow-up reminders.

## Assumptions

- The immediate MVP is only the urgency-based treatment queue.
- Real hospital integration is not required for the first demo.
- Fake patient data is acceptable for development.
- Staff review remains mandatory for clinical safety.
- The backend owns queue ordering and audit state.
- Future multi-agent care coordination features will build on this queue foundation.
