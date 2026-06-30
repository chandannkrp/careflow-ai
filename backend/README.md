# CareFlow AI Backend

Spring Boot backend module for the urgency queue MVP.

## Current Scope

- MVP persistence entities and enums, excluding audit for now.
- Spring Data repositories.
- Intake, assessment, queue, override, status, and metrics services.
- REST endpoints for intake, queue, override/status updates, and queue metrics.
- Empty datasource placeholders in `src/main/resources/application.yml`.

No Docker, schema migrations, audit module, seed data, tests, or logging are included yet.

## Current Endpoints

- `POST /api/intakes`
- `GET /api/intakes/{id}`
- `POST /api/intakes/{id}/assess`
- `GET /api/queue`
- `POST /api/queue/{patientId}/override`
- `POST /api/queue/{patientId}/status`
- `GET /api/metrics/queue`
- `GET /actuator/health`
- `GET /actuator/info`
