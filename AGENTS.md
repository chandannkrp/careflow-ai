# Repository Guidelines

## Project Structure & Module Organization

CareFlow AI has a Spring Boot backend in `backend/` and a Vite React frontend in `frontend/`. Backend code lives in `backend/src/main/java/com/careflowai`, grouped by domain packages such as `intake`, `assessment`, `queue`, `metrics`, `patient`, `staff`, and shared enums in `common`. DTOs sit in each domain's `dto` package. Configuration is in `backend/src/main/resources/application.yml`; migrations belong in `backend/src/main/resources/db/migration`.

Frontend code lives in `frontend/src`: `app` for the shell, `api` for HTTP clients, `features/<feature>` for feature modules, `types` for shared types, and `styles/global.css` for Tailwind/global styles.

## Build, Test, and Development Commands

- `cd backend && mvn spring-boot:run`: run the API on `PORT` or `8080`.
- `cd backend && mvn test`: run Spring Boot/JUnit tests.
- `cd backend && mvn package`: compile, test, and package the backend.
- `cd frontend && npm install`: install frontend dependencies.
- `cd frontend && npm run dev`: start the Vite development server.
- `cd frontend && npm run build`: type-check and build production assets.
- `cd frontend && npm run preview`: serve the built frontend.

## Coding Style & Naming Conventions

Java uses 4-space indentation, constructor injection, domain packages, and Spring stereotypes such as `*Controller`, `*Service`, and `*Repository`. Keep DTO names request/response oriented, for example `CreateIntakeRequest` and `QueueEntryResponse`.

TypeScript uses 2-space indentation, single quotes, semicolons, React function components, and PascalCase component names. Prefer feature-level exports through `frontend/src/features/<feature>/index.ts`. Tailwind utility classes are the current styling convention.

## Testing Guidelines

Backend tests go in `backend/src/test/java`, mirroring the main package path. Use JUnit/Spring Boot support from `spring-boot-starter-test`; name unit tests `*Test` and Spring context tests `*IntegrationTest`. Cover scoring, queue ordering, validation, and repository behavior when changing backend workflows.

No frontend test runner is configured yet. Until one is added, rely on `npm run build` for type safety and manually verify main workflows in Vite.

## Commit & Pull Request Guidelines

Use concise, imperative commit subjects such as `Add queue status endpoint` or `Fix intake validation`. Keep backend and frontend changes together only when they support the same behavior.

Pull requests should include a short summary, test/build commands run, linked issue or plan item when available, and screenshots for UI changes. Call out configuration or database changes explicitly.

## Security & Configuration Tips

Do not commit real database credentials. Prefer environment variables for `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, and `PORT`.
