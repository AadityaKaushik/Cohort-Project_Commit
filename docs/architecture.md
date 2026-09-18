# Cohort — Architecture (Sprint 1 snapshot)

This is the living architecture reference for the codebase as it stands after Sprint 1 ("Foundation"). It summarises what `PROJECT_PLAN.md` and `ARCHITECTURE_REVISION_JAVA.md` specify, cross-referenced to what's actually implemented — the two planning documents remain the source of truth for *why*; this file is the map of *where*.

## 1. System overview

```
┌──────────────────────────────────────────────────────────────────┐
│  React + TypeScript SPA  (frontend/)                              │
│  Vite, Tailwind, shadcn/ui, TanStack Query, React Router          │
└───────────────────────────────┬────────────────────────────────────┘
                                │ REST / JSON (Bearer JWT)
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│  Spring Boot 3 / Java 21 API  (api/)                               │
│                                                                    │
│  Controller → Service → Repository → Entity (JPA)                 │
│  Sprint 1: auth, users, applications                              │
│  Planned (Sprint 2+): resumes, interviews, prep, analytics,       │
│  recommendations, notifications, reports, FL gateway               │
└───────────────────────────────┬────────────────────────────────────┘
                                │ JDBC / JPA
                                ▼
                    ┌────────────────────────┐
                    │ PostgreSQL 16           │
                    │ Flyway-versioned schema │
                    └────────────────────────┘
```

A Python `fl-service` (Flower + scikit-learn) joins this picture in Sprint 4, reachable from the Java API only through `FederatedLearningGateway` (Adapter pattern) and never holding credentials to any user-content table — see [ADR-007](adr/ADR-007-no-fl-update-endpoint.md) and [ADR-010](adr/ADR-010-java-spring-boot-main-api.md). It is out of scope for what's built so far.

## 2. Technology stack

| Layer | Choice | Notes |
|---|---|---|
| Backend framework | Spring Boot 3.5 | Java 21 (LTS) |
| ORM / migrations | Spring Data JPA + Hibernate / Flyway | `ddl-auto: validate` — Flyway alone owns schema changes |
| Auth | Spring Security (OAuth2 resource server) + self-issued JWT + Argon2id | [ADR-001](adr/ADR-001-jwt-argon2id-auth.md) |
| Validation | Jakarta Bean Validation | Rejects invalid/unknown fields at the controller boundary |
| Testing | JUnit 5, MockMvc, Testcontainers (Postgres 16), AssertJ, Mockito | `mvn verify` runs the full suite plus the JaCoCo gate |
| Coverage | JaCoCo, 70% line-coverage `BUNDLE` gate | Bound to the `verify` phase, not `test` — see `api/pom.xml` |
| Frontend | React 19, TypeScript, Vite, Tailwind v4, shadcn/ui (radix-nova style) | |
| Data fetching | TanStack Query | Single-flight refresh interceptor in `lib/api-client.ts` |
| Frontend testing | Vitest, React Testing Library, jsdom | `npm test` |
| CI | GitHub Actions (`.github/workflows/ci.yml`) | Separate `api` and `frontend` jobs |

## 3. Backend package structure (as built)

```
api/src/main/java/com/cohort/api/
├── ApiApplication.java
├── config/                 CorsConfig
├── controller/              AuthController, UserController, ApplicationController, HealthController
├── dto/                     Request/response records (snake_case over the wire via a global
│                            Jackson SNAKE_CASE naming strategy — DTO field names stay camelCase Java)
├── mapper/                  ApplicationMapper — hand-written DTO ↔ entity mapping (DTO + Mapper pattern)
├── entity/                  User, RefreshToken, Company, Application, ApplicationStatus,
│                            ApplicationStatusHistory, ApplicationStatusConverter
├── repository/               UserScopedRepository<T> base + UserRepository, RefreshTokenRepository,
│                            CompanyRepository, ApplicationRepository, ApplicationStatusHistoryRepository
├── security/                 JwtConfig, SecurityConfig, PasswordEncoderConfig, PasswordPolicyValidator,
│                            LoginRateLimiter, Rest*Handler
├── service/
│   ├── AuthService, TokenService, UserService, ApplicationService
│   └── application/state/    ApplicationStatusState + one class per status (State pattern),
│                            ApplicationStatusStateRegistry
└── exception/                GlobalExceptionHandler + one exception type per error case
```

Frontend mirrors this with `src/lib/*-api.ts` per resource, `src/pages/<resource>/*Page.tsx`, and shared components in `src/components/shared/`.

## 4. Design patterns landed so far

Full catalogue and rationale: `ARCHITECTURE_REVISION_JAVA.md` §4. What Sprint 1 actually implements:

| Pattern | Where | What it replaces |
|---|---|---|
| **State** | `service/application/state/` — `ApplicationStatusState` interface, ten implementations (`SavedState` … `DeclinedState`), `ApplicationStatusStateRegistry` (EnumMap lookup) | An if/else chain checking every (from, to) status pair. Each status class owns only its own legal transitions; the registry looks one up by table, never by branching. Exhaustively tested: 200 (from, to, hasNote) combinations in `ApplicationStatusStateRegistryTest`. |
| **Repository** | `UserScopedRepository<T>` | A repository base that extends bare `Repository<T, UUID>` (not `JpaRepository`), so it exposes *no* unscoped finder — every query method requires `userId`. Cross-user leakage becomes a compile error, not a runtime bug to catch in review. |
| **DTO + Mapper** | `dto/` + `mapper/ApplicationMapper` | JPA entities never cross the controller boundary. Hand-written (matching the style already established in P05/P06's `UserProfileResponse.from()`), not MapStruct — see `ARCHITECTURE_REVISION_JAVA.md` §4.1's note that either is acceptable. |

Patterns not yet landed (Specification, Chain of Responsibility, Strategy, Observer, Template Method, Adapter, Facade, Builder, Decorator, Circuit Breaker) arrive with the features that need them from Sprint 2 onward — see `BUILD_PHASES_JAVA.md`.

## 5. Database (Sprint 1 tables)

`V1`–`V6` (Flyway, `api/src/main/resources/db/migration/`):

| Table | Owner column | Notes |
|---|---|---|
| `users` | — | `fl_opt_in` defaults false (FR-87) |
| `refresh_tokens` | `user_id` | Revocable, hash-only storage |
| `companies` | `created_by_user_id` | Per-user, not global — [ADR-004](adr/ADR-004-user-scoped-companies.md) |
| `applications` | `user_id` | `status` (full domain incl. terminal) vs `current_stage` (non-terminal only, frozen once terminal) kept as two columns — see `Application.applyStatusChange()` |
| `application_status_history` | (via `application_id`) | Append-only, no `updated_at`, no update endpoint |
| `tags` / `application_tags` | `user_id` (on `tags`) | Migrated in Sprint 1, CRUD endpoints land with tagging in Sprint 5 (US-19) |

Every mutable, user-owned table has a matching rollback script in `db/rollback/` (`db/rollback/README.md` has the reverse-order replay instructions) — Flyway itself has no open-source "undo," so these are hand-written and verified by `FlywayMigrationTest`.

## 6. Isolation model (FR-94/95, NFR-05)

Three independent layers, per `PROJECT_PLAN.md` §17.4:

1. **Schema** — `user_id NOT NULL` (or, for `companies`, `created_by_user_id`) on every owned table.
2. **Repository** — `UserScopedRepository<T>` makes `userId` a required parameter on every finder; there is no unscoped alternative to accidentally call.
3. **Test** — every new user-owned-data endpoint ships with a cross-user isolation test in the same commit (`ApplicationIntegrationTest.crossUserAccessReturns404NeverExposingAnotherUsersApplication`), asserting 404 — never 403 — on foreign access.

## 7. Testing strategy

- **Unit** — JUnit 5 + Mockito for pure logic (`AuthServiceTest`, `PasswordPolicyValidatorTest`, `ApplicationStatusStateRegistryTest`).
- **Slice/integration** — `@SpringBootTest` + `MockMvc` + Testcontainers Postgres for anything touching real HTTP + real SQL (`AuthIntegrationTest`, `UserProfileIntegrationTest`, `ApplicationIntegrationTest`, `FlywayMigrationTest`, `UserScopedRepositoryIntegrationTest`).
- **Journey** — `SprintOneJourneyIntegrationTest` walks register → login → create application → status transitions → history as one continuous flow through real endpoints, complementing the narrower per-endpoint tests above.
- **Frontend** — Vitest + React Testing Library for hooks, the api-client's refresh interceptor, and page-level loading/error/empty states.
- **Coverage gate** — JaCoCo `BUNDLE` rule, ≥70% line coverage, enforced in `mvn verify` and in CI; currently ~87%.

## 8. Environment note (local dev only)

Testcontainers in this project's own sandbox runs against rootless Podman rather than Docker; `api/pom.xml`'s Surefire configuration pins `reuseForks=false` because reusing a JVM fork across Testcontainers-backed test classes was observed to break the Podman API socket connection. GitHub Actions' `ubuntu-latest` runners ship Docker already running, so this is purely a local-sandbox accommodation, not a project requirement — see the comment in `api/pom.xml` and `.github/workflows/ci.yml`.
