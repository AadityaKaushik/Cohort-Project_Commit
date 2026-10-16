# Cohort: Architecture and Design Patterns

This document describes how Cohort is built: the technology stack, the split between the Spring Boot API and the Python federated-learning service, the design patterns used across the codebase, the tables and requirements for notifications and report export, and the package structure. Requirements (FR/NFR/DR), user stories, the database design, the UI/UX plan and the ML/FL design are in `PROJECT_PLAN.md`; the build sequence is in `BUILD_PHASES_JAVA.md`.

---

## 1. Scope of this document

| Topic | Where it lives |
|---|---|
| Technology stack and why each choice was made | §2 here (summary in `PROJECT_PLAN.md` §15) |
| System architecture and the Java/Python boundary | §3 here (context and data flow in `PROJECT_PLAN.md` §16) |
| Design patterns, where and why | §4 here |
| Notifications and report export tables | §5 here (all other tables in `PROJECT_PLAN.md` §17) |
| Notifications and report export stories and requirements | §6 here |
| Package and folder structure | §7 here |
| Sprint placement of the pattern-bearing work | §8 here |
| Architecture-specific risks | §9 here (project risks in `PROJECT_PLAN.md` §28) |
| Pattern documentation rule | §10 here |

The API contract in `PROJECT_PLAN.md` §18 is implemented exactly as written by the Spring Boot API.

---

## 2. Technology stack

| Layer | Technology | Why |
|---|---|---|
| Backend framework | Spring Boot 3.x | Mature dependency-injection container with first-class support for every pattern in §4 |
| Language | Java 21 (LTS) | Records, pattern matching and virtual threads keep modern Java concise |
| ORM | Spring Data JPA + Hibernate | Relational mapping; the `Specification` API (§4) provides the combinable filters FR-16 needs |
| Migrations | Flyway | Versioned SQL migrations with a rollback script per migration (NFR-27) |
| Validation | Jakarta Bean Validation (`@NotNull`, `@Size`, custom validators) | Rejects unknown and invalid fields at the boundary (FR-96) |
| Auth | Spring Security + `Argon2PasswordEncoder` + JWT; OAuth2 client for Google and GitHub sign-in | Argon2id support ships with Spring Security; social sign-in issues the app's own JWTs |
| API docs | springdoc-openapi | Generated OpenAPI contract; feeds frontend types and the security suite's endpoint enumeration |
| Testing | JUnit 5 + MockMvc + Testcontainers | Unit, controller and real-Postgres integration tests |
| Build tool | Maven | |
| Resilience | Resilience4j (circuit breaker, timeout, fallback) | Protects the Java to Python call at the FL boundary (§3) |
| Object mapping | DTO records with mappers | JPA entities never leave the controller boundary |
| Federated learning | Python, Flower, scikit-learn, with a thin FastAPI control-plane wrapper (`fl-service/`) | Flower has no Java equivalent; the Python service is limited to FL |
| AI | Ollama (local model `llama3.2:3b`, embeddings `nomic-embed-text`) by default; Google Gemini optional | User data stays on the server unless the operator enables the hosted provider, which the UI discloses |
| Frontend | React 19 + TypeScript + Vite + Tailwind + shadcn/ui + TanStack Query + Recharts | See `PROJECT_PLAN.md` §15 |
| Database and runtime | PostgreSQL 16, Docker Compose, Git and GitHub Actions | |

---

## 3. System architecture

```
┌──────────────────────────────────────────────────────────────────┐
│  React + TypeScript SPA                                           │
└───────────────────────────────┬────────────────────────────────────┘
                                │ REST / JSON (Bearer JWT)
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│  Spring Boot API  (container: api)                                │
│                                                                    │
│  Controllers → Services → Repositories → Entities (JPA)           │
│  Owns: auth, applications, resumes, interviews, prep, analytics,   │
│  recommendations, notifications, reports, AI features              │
│                                                                    │
│  FederatedLearningGateway (interface)                             │
│      └── FlowerServiceAdapter (impl) ──── wrapped in ────┐        │
│                                                    Resilience4j    │
│                                                  CircuitBreaker    │
└───────────────────────────────┬───────────────┬───────────────────┘
                                │ SQL (JPA)      │ REST (control-plane
                                ▼                │  only, sanitised
┌────────────────────────┐                       │  numeric export in,
│ PostgreSQL              │                       │  round metadata out)
│ schema: app             │                       ▼
│ (everything except      │           ┌──────────────────────────┐
│  fl_* tables)           │           │ fl-service (Python)       │
└────────────────────────┘           │ Flower client + server    │
          ▲                          │ scikit-learn training     │
          │ INSERT-only role,        │ clip + noise               │
          │ 3 tables only:           │                            │
          │ fl_rounds, fl_models,    │ No credentials to read     │
          │ privacy_ledger_entries   │ applications/resumes/      │
          └──────────────────────────│ interviews/notes           │
                                     └──────────────────────────┘
```

Circuit breaker at the FL boundary. The FL service is a network hop to a different process in a different language, so it can be slow, down or mid-deploy, and the Recommendations page must never hang or return 500 because of that. `FederatedLearningGateway` is called through a Resilience4j circuit breaker with a documented fallback: if the FL service is unreachable, the API serves local-only recommendations, exactly the "local" side of the Federated Lift toggle (US-65).

Data flow into the FL boundary. Java is the only component that touches user content. Before a round starts, Java exports a sanitised numeric snapshot (the 24-feature schema in `PROJECT_PLAN.md` §22.2) into the FL service's own storage. The privacy guarantee is enforced at two independent points: Java's export allow-list and Python's clip-and-noise step. NFR-10 and FR-92 hold exactly as written.

AI boundary. AI features call an `AiProvider` interface (§4.1, Strategy). The default implementation talks to a local Ollama instance; the optional Gemini implementation is enabled only by configuration and is disclosed in the UI. The API assembles every prompt from the requesting user's own data only.

---

## 4. Design patterns

Each pattern below is tied to a concrete piece of functionality. Some parts of the system are left plain (§4.2), because a pattern on a simple service method only adds complexity.

### 4.1 Pattern catalogue

| Pattern | Applied to | Problem it solves |
|---|---|---|
| State | `ApplicationStatus` and its transition rules (`PROJECT_PLAN.md` §17.5) | A class per status, each knowing its own legal next states, replaces a sprawling if/else check and makes FR-21 ("reject invalid transitions") a structural property; the Kanban board (P73) relies on it to refuse illegal moves |
| Chain of Responsibility | (a) The recommendation pipeline: candidate generation → tiering → scoring → federated re-rank → diversity filter (`PROJECT_PLAN.md` §21.2); (b) resume upload validation: extension check → MIME check → magic-byte check (US-21) | Each stage is independently testable and reorderable; adding a stage never means editing existing stages |
| Strategy | (a) `NotificationChannel` (in-app channel; further channels are drop-in implementations, §6.1); (b) `FileStorageStrategy` (local disk; S3 is a drop-in implementation); (c) evidence-tier scoring per tier A/B/C/D (`PROJECT_PLAN.md` §21.6); (d) `MailSender` for password reset and verification; (e) `AiProvider` (local Ollama default, optional Gemini) | Swapping the implementation never touches the calling code |
| Observer | `ApplicationEventPublisher` (Spring's event bus): a status change, a stalled application, an interview date passing or a cold contact publishes an event; the notification service and other listeners subscribe independently | The detector does not need to know who cares about the fact it detected |
| Template Method | `AnalyticsMetric` abstract base class: defines the fixed skeleton (compute → check sample size → attach `sufficient_data` flag → format) once; each concrete metric (funnel, conversion rate, dwell time, readiness, resume performance) implements only its formula step | Enforces FR-67 ("every computed rate shows its sample size") structurally; a new metric cannot skip the sample-size gate |
| Specification | Spring Data JPA `Specification<Application>` for combinable filters (status, priority, tag, date range, FR-16) | Filters compose with `.and()`/`.or()`; a new filterable field is one new `Specification` |
| Adapter | `FlowerServiceAdapter implements FederatedLearningGateway` | Isolates the one class that knows the Python service's REST contract |
| Facade | `RecommendationFacade` (P43) | Controllers call one method (`generateRecommendations(userId)`); the multi-step pipeline stays internal. FL round orchestration lives inside `FlowerServiceAdapter`; a separate `FederatedRoundFacade` is not part of the design |
| Builder | `ReportBuilder` (§6.2) and `RecommendationCard` construction (many optional fields: evidence tier, observation count, explanation, target topic) | Avoids telescoping constructors; `.withDateRange(...).withApplications(true).withAnalytics(true).build()` reads naturally |
| Decorator | `ValidatingFileStorage` wraps the base `FileStorageStrategy` implementation | Validation (size, type, signature, US-21) layers onto storage without the base storage class knowing validation exists |
| Repository | Spring Data JPA repositories, each extending a shared `UserScopedRepository<T>` base that makes `userId` a required parameter on every finder | The enforcement mechanism described in `PROJECT_PLAN.md` §17.4: a query cannot forget the user scope |
| DTO + Mapper | Mappers between JPA entities and API DTOs | JPA entities never cross the controller boundary, preventing lazy-loading exceptions and accidental over-exposure of internal fields |
| Circuit Breaker *(resilience pattern, not GoF)* | Resilience4j around `FlowerServiceAdapter` | Covered in §3: the FL service being slow or down never breaks the Recommendations page |

### 4.2 Not patterned

`ApplicationController`, `ResumeController`, `InterviewController` and their services stay plain CRUD. A service method that does "validate, save, return" in five lines does not need a Factory, a Strategy or a Command wrapping it. Applying patterns to simple operations adds complexity without benefit.

### 4.3 One worked example, end to end

How several patterns cooperate for a single feature, "flag a stalled application and notify the user" (FR-26 and FR-99):

1. A scheduled job (or a status-change event) triggers `StalledApplicationDetector`.
2. The detector uses the `AnalyticsMetric` Template Method base to compute the user's own median dwell time for that stage, with the sample-size gate built in.
3. If an application exceeds that threshold, the detector publishes a `StalledApplicationEvent` (Spring's Observer mechanism); it does not know who is listening.
4. `NotificationListener` receives the event and hands it to `NotificationService`, which picks a `NotificationChannel` Strategy (in-app) and creates the notification.
5. The call from the triggering controller down to "notification created" is fronted by a Facade method, so nothing about this chain leaks into a controller.

Every pattern in the chain solves an actual coupling problem in that feature.

---

## 5. Notifications and report export tables

These two tables complement the schema in `PROJECT_PLAN.md` §17:

`notifications`: `id`, `user_id FK`, `type` (`stalled_application`|`upcoming_interview`|`missing_debrief`|`new_recommendation`|`fl_round_result`), `title`, `body`, `related_entity_type`, `related_entity_id`, `channel` (`in_app`), `is_read`, `created_at`. Indexed on `(user_id, is_read, created_at DESC)` for the notification bell's "unread first" query.

`report_exports`: `id`, `user_id FK`, `report_type` (`applications`|`analytics_summary`), `format` (`pdf`|`csv`), `date_range_from`, `date_range_to`, `file_path`, `generated_at`. Exported files follow the same storage rules as resumes (US-21/22): outside the web root, served only through an authenticated, expiring, user-scoped endpoint.

Both tables carry `user_id NOT NULL` and go through the same `UserScopedRepository` base as everything else, so isolation is inherited rather than special-cased.

---

## 6. Notifications and report export

Both features serve goals G1 ("single source of truth") and G4 ("make performance legible") and are scoped as Should. Neither touches the federated-learning boundary.

### 6.1 In-app notifications, EPIC 10

User stories:

- US-76, Notify me when an application stalls · Should · 3 · Deps: FR-26
  As a job seeker, I want to be notified when an application is stalled, so that I don't have to keep checking the dashboard manually.
- US-77, Notify me before an interview · Should · 3
  As a job seeker, I want a reminder ahead of a scheduled interview, so that I remember to prepare.
- US-78, Notify me about a missing debrief · Should · 2
  As a job seeker, I want a nudge if I haven't logged feedback for a past interview, so that I don't lose the details while they're still fresh.
- US-79, Notification bell + mark as read · Should · 3
  As a job seeker, I want a notification centre, so that I have one place to review what the app has flagged for me.

Requirements:

| ID | Requirement | Priority |
|---|---|---|
| FR-99 | The system shall publish a domain event whenever an application is flagged as stalled, an interview date passes without a debrief, or an interview is due within 24 hours. | Should |
| FR-100 | The system shall create an in-app notification from any subscribed event, scoped to the affected user only. | Should |
| FR-101 | The system shall allow a user to mark a notification as read, and to view unread notifications separately from read ones. | Should |
| NFR-53 | The notification channel mechanism shall be pluggable (Strategy pattern) so that further channels can be added without modifying event-detection logic. | Should |

Notifications surface the same detection logic as the dashboard action queue (US-49) as a persistent list, reused through the Observer pattern rather than duplicated.

### 6.2 Report export (PDF/CSV), EPIC 6

User story:

- US-80, Export my applications and analytics · Should · 5 · Deps: EPIC 2, EPIC 6
  As a job seeker, I want to export my application list and summary analytics as a PDF or CSV, so that I can share my progress outside the app (for example with a mentor or career advisor) or keep an offline record.

Requirements:

| ID | Requirement | Priority |
|---|---|---|
| FR-102 | The system shall generate a PDF or CSV export of a user's applications, optionally filtered by date range. | Should |
| FR-103 | The system shall generate a PDF export of a user's analytics summary (funnel, conversion rates, sample sizes), reusing the exact formulas and sample-size gating of the in-app analytics (`PROJECT_PLAN.md` §20.7). | Should |
| FR-104 | Exported files shall be retrievable only by their owner, through an authenticated, expiring download endpoint, and never publicly served. | Must |

FR-104 mirrors FR-32 (resume download security): the same rule applied to a second file type.

---

## 7. Package and folder structure

```
cohort/
├── api/                                  # Spring Boot application
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/cohort/api/
│       │   ├── ApiApplication.java
│       │   ├── config/                   # SecurityConfig, OpenApiConfig, Resilience4jConfig
│       │   ├── controller/               # thin, delegates to services/facades only
│       │   ├── dto/                      # request/response records
│       │   ├── mapper/                   # entity ↔ DTO mappers
│       │   ├── entity/                   # JPA entities
│       │   ├── event/                    # domain events (Observer)
│       │   ├── security/                 # JWT, Argon2id, OAuth2 sign-in
│       │   ├── repository/
│       │   │   └── UserScopedRepository.java     # shared base, §4.1
│       │   └── service/
│       │       ├── application/          # state machine (State pattern)
│       │       ├── resume/storage/       # FileStorageStrategy + ValidatingFileStorage decorator
│       │       ├── preparation/          # question bank, practice, mastery
│       │       ├── analytics/            # AnalyticsMetric Template Method base and metrics
│       │       ├── detection/            # stall, upcoming-interview and missing-debrief detectors
│       │       ├── recommendation/       # generator chain, tiering, RecommendationFacade
│       │       ├── notification/         # NotificationChannel strategy, InAppNotificationChannel
│       │       ├── report/               # ReportBuilder
│       │       ├── mail/                 # MailSender strategy
│       │       ├── ai/                   # AiProvider strategy and AI features
│       │       └── fl/                   # FederatedLearningGateway, FlowerServiceAdapter
│       ├── main/resources/db/migration/  # Flyway V__ migrations and rollback scripts
│       └── test/java/com/cohort/api/     # JUnit 5, MockMvc, Testcontainers
│
├── fl-service/                           # Python, Flower + scikit-learn only
│   ├── Dockerfile
│   ├── requirements.txt
│   └── app/
│       ├── main.py                       # thin FastAPI control-plane wrapper
│       ├── client/                       # feature extractor, local trainer, privacy (clip + noise)
│       ├── server/                       # Flower server, FedAvg strategy, min-cohort + norm filter
│       └── common/schema.py              # FEATURE_SCHEMA_V1, single source of truth
│
├── frontend/                             # React + TypeScript SPA
├── docs/                                 # architecture.md, ADRs, plan/
├── docker-compose.yml                    # db + api + web
└── docker-compose.fl.yml                 # fl profile: fl-service with isolated roles
```

`fl-service/` sits outside `api/` because it is a different trust zone: separate image, separate credentials, no shared imports.

---

## 8. Sprint placement

| Work | Sprint | Phases |
|---|---|---|
| State, Repository, DTO + Mapper | 1 | P10, P11 |
| Specification, Strategy and Decorator + Chain of Responsibility for file storage | 2 | P16, P18, P19 |
| Template Method for analytics | 3 | P30, P34, P35, P37 |
| Recommendation chain, tier Strategy, Facade + Builder | 4 | P40, P42, P43 |
| Adapter and Circuit Breaker at the FL boundary | 4 | P53, P54 |
| Observer events and NotificationChannel Strategy (EPIC 10) | 5 | P60, P61, P62 |
| ReportBuilder and report export (US-80) | 5 | P63, P64, P65 |
| MailSender Strategy | 5 | P68 |
| AiProvider Strategy | 8 | P89 |

The Sprint 4 circuit breaker belongs with the rest of the FL integration because it tests the same boundary. Notifications and report export sit in Sprint 5, after analytics is stable, because report export reuses the analytics formulas rather than defining new ones.

---

## 9. Architecture risks

| ID | Risk | Prob. | Impact | Mitigation |
|---|---|---|---|---|
| R14 | Java/Python interop friction at the FL boundary (serialisation mismatches, contract drift) | Medium | Medium | `FederatedLearningGateway` is the single point of contact; the contract is defined once and tested on both sides |
| R15 | Uneven Java and Python depth across the team | Medium | Medium | Spring Boot fundamentals front-loaded in Sprint 1; the fl-service is kept small (Flower + scikit-learn only) |
| R16 | Pattern over-application drifting the codebase toward unnecessary complexity | Low-medium | Low | §4.2's "not patterned" list is used as a review checklist |
| R17 | Circuit breaker fallback hiding a broken FL service | Low | Medium | The fallback emits a logged event while the user-facing response degrades gracefully |
| R18 | Local model too slow or unavailable on a team machine | Medium | Medium | Every AI feature has a timeout and a graceful "AI unavailable" state; the rest of the product works without AI |

---

## 10. Pattern documentation rule

Every PR touching a pattern-bearing class (§4.1) must state, in the PR description, which pattern it implements or extends and why. One sentence is enough. Sprint 4 additionally requires a fault-injection test that stops the `fl-service` container mid-request and asserts that the Recommendations endpoint still returns 200 with local-only results, not 500.
