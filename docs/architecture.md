# Cohort architecture

Reference for where each part of the design lives in the code. The reasoning behind the design is in `docs/plan/PROJECT_PLAN.md` and `docs/plan/ARCHITECTURE_REVISION_JAVA.md`; the build order is in `docs/plan/BUILD_PHASES_JAVA.md` (110 phases across nine sprints).

## 1. System overview

```
React + TypeScript SPA (frontend/)
        | REST / JSON, Bearer JWT
        v
Spring Boot 3 / Java 21 API (api/)  ---- HTTP, circuit breaker ---->  fl-service (Python, Flower + scikit-learn)
        | JDBC / JPA
        v
PostgreSQL 16 (Flyway-versioned schema)
```

The API reaches `fl-service` only through `FederatedLearningGateway` (Adapter, wrapped by a circuit-breaker decorator). `fl-service` holds no credentials for any user-content table; see [ADR-007](adr/ADR-007-no-fl-update-endpoint.md) and [ADR-010](adr/ADR-010-java-spring-boot-main-api.md).

## 2. Technology stack

| Layer | Choice |
|---|---|
| Backend | Spring Boot 3.5, Java 21, Spring Data JPA, Flyway (`ddl-auto: validate`) |
| Auth | Spring Security, self-issued JWT, Argon2id ([ADR-001](adr/ADR-001-jwt-argon2id-auth.md)); optional Google/GitHub sign-in ([ADR-012](adr/ADR-012-social-sign-in.md)) |
| Resilience | Resilience4j circuit breaker around the FL boundary |
| Reports | OpenPDF and CSV renderers |
| Frontend | React 19, TypeScript, Vite, Tailwind v4, shadcn/ui, TanStack Query, Recharts |
| FL service | FastAPI, Flower, scikit-learn |
| Backend tests | JUnit 5, MockMvc, Testcontainers (Postgres 16), JaCoCo gate in `mvn verify` |
| Frontend tests | Vitest, React Testing Library, jsdom |
| CI | GitHub Actions, `.github/workflows/ci.yml` |

## 3. Backend packages

```
api/src/main/java/com/cohort/api/
  config/ controller/ dto/ mapper/ entity/ repository/ exception/ security/
  service/
    account/ application/state/ detection/ mail/ notification/ resume/
    analytics/ preparation/ recommendation/ fl/ report/
```

Layering is controller, service, repository, entity, with DTOs and hand-written mappers at the edge. The REST contract is snake_case via a global Jackson naming strategy.

## 4. Design patterns

| Pattern | Where |
|---|---|
| State | `service/application/state/`: one class per application status, looked up through `ApplicationStatusStateRegistry` |
| Strategy | resume file storage, notification channels, recommendation tiering |
| Chain of Responsibility | resume file validation, recommendation generators |
| Decorator | file-storage validation wrapper, `FlCircuitBreakerGateway` |
| Template Method | `AnalyticsMetric` and its concrete metrics |
| Observer | application events consumed by `NotificationListener` |
| Builder | `ReportBuilder` |
| Facade | `RecommendationFacade` |
| Adapter | `FlowerServiceAdapter` behind `FederatedLearningGateway` |
| Circuit Breaker | `FlCircuitBreakerGateway` |

## 5. Database

Flyway migrations `V1` to `V24` live in `api/src/main/resources/db/migration/`. Every mutable, user-owned table has a hand-written rollback script in `db/rollback/` (see its README), because Flyway Community has no undo. Migration `V19` creates the isolated `fl` schema and `cohort_fl` role.

## 6. Isolation model

1. Schema: `user_id NOT NULL` (or `created_by_user_id` for companies) on every owned table.
2. Repository: `UserScopedRepository<T>` makes `userId` a required parameter on every finder.
3. Tests: every user-owned endpoint has a cross-user test that expects 404, never 403.

## 7. Testing

- Unit tests for pure logic (state registry, mastery engine, metrics, privacy ledger).
- Integration tests with `@SpringBootTest`, MockMvc and Testcontainers Postgres for each endpoint, plus Flyway migration and fault-injection tests for the FL boundary.
- `SprintOneJourneyIntegrationTest` walks register, login, create application, status changes and history.
- Frontend: Vitest and React Testing Library for hooks, the API client, pages and accessibility checks.
- `fl-service`: pytest for schema, client, server and control modules.
- Coverage: JaCoCo bundle gate (70% lines) bound to `mvn verify`.

## 8. Local development note

Testcontainers runs on rootless Podman locally. `api/pom.xml` sets `reuseForks=false` in Surefire because a reused JVM fork breaks the Podman socket connection. CI uses Docker and is unaffected.
