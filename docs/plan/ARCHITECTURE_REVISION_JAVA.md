# Architecture Revision — Java Spring Boot Edition

**Supplements, not replaces:** `PROJECT_PLAN.md`. Every requirement (FR/NFR/DR), the backlog, the database entities, the UI/UX plan, the privacy design, and the testing strategy from that document **still apply unchanged** — this document covers only what changes because of the Java/Python split we just agreed on, plus two deliberate additions, plus a full design-pattern pass across the codebase. Nothing here contradicts the original plan; it re-implements the same product on a different backend stack.

---

## 1. What changes, what doesn't

**Read this before reading anything else in this file.** `PROJECT_PLAN.md` was written before the Java decision and still describes a Python/FastAPI backend in several sections. Those sections are **superseded by this document**, specifically:

| `PROJECT_PLAN.md` section | Status | Superseded by |
|---|---|---|
| §13 Sprint 1 Detailed Plan (FastAPI endpoints, Alembic, pytest) | Superseded — process/scope is unchanged, tooling is not | §2, §7 here, and Sprint 1 of `BUILD_PHASES_JAVA.md` |
| §15 Technology Stack | Superseded | §2 here |
| §18 API Design (endpoint list, request/response shapes) | **Not superseded** — the contract stands exactly as written; only its Java implementation differs | — |
| §24 Testing Strategy (pytest, testcontainers-python) | Superseded in tooling only — the testing *philosophy* (unit/API/integration/security/FL layers, coverage gates) is unchanged | §2 here |
| §25 GitHub/SCRUM Workflow, §26 Folder Structure | Superseded | §7 here |
| §6, §7, §8 (FR/NFR/DR), §9–§14 (backlog, EPICs, sprints, conflicts) | **Not superseded** — requirements and process describe behaviour, not implementation language | — |
| §16, §17, §20–§23 (architecture diagram, database, UI/UX, ML/FL design, privacy, security) | **Not superseded in substance** — §16's diagram is redrawn in §3 here to show the language boundary; the database, UI, ML task, and privacy guarantees are identical | §3 here (architecture only) |

If your professor opens both documents, this table is the answer to "why does the stack table on page X contradict page Y" — it doesn't contradict, it's dated, and this table says so explicitly.

| Area | Status |
|---|---|
| Product goals, FR/NFR/DR, backlog, EPICs 1–9, sprint scope | **Unchanged** — same requirements, same IDs, same priorities |
| Database schema (entities, relationships, constraints) | **Unchanged in substance** — same tables, same columns; only the ORM annotations change (SQLAlchemy → JPA) |
| API contract (endpoints, request/response shapes) | **Unchanged** — a frontend developer sees no difference |
| Frontend (React/TS/Tailwind/shadcn/Recharts) | **Unchanged** — was never coupled to the backend language |
| Privacy design (clip → noise → min-cohort → filter, no FL server credentials) | **Unchanged in guarantee**, changed in *who enforces the boundary* (see §3) |
| Backend implementation language | **Changed** — Java (Spring Boot) for the whole application, Python retained only where a library leaves no alternative (Flower, scikit-learn) |
| Internal code structure | **Upgraded** — this revision adds an explicit, justified design-pattern layer (§4), which the original plan named informally ("service layer," "repository layer") but didn't formalise |
| Scope | **Slightly expanded** — two new features added deliberately (§6), both natural extensions of goals already in the plan, not scope drift |

---

## 2. Revised Technology Stack

| Layer | Was | Now | Why |
|---|---|---|---|
| Backend framework | FastAPI | **Spring Boot 3.x** | Team's stated preference; mature DI container, first-class support for every pattern in §4 |
| Language | Python 3.11+ | **Java 21 (LTS)** | Records, pattern matching, and virtual threads make modern Java noticeably less verbose than the Java of five years ago |
| ORM | SQLAlchemy 2.0 | **Spring Data JPA + Hibernate** | Same relational mapping job; `Specification` API (§4) directly replaces the dynamic-filter logic FR-16 needs |
| Migrations | Alembic | **Flyway** | Versioned, reversible SQL migrations — same guarantee as NFR-27, Java-native tooling |
| Validation | Pydantic | **Jakarta Bean Validation** (`@NotNull`, `@Size`, custom validators) | Same "reject unknown/invalid fields at the boundary" guarantee as FR-96 |
| Auth | python-jose + argon2-cffi | **Spring Security** + `Argon2PasswordEncoder` (built in) | Less code than the Python version — Spring Security ships Argon2id support directly |
| API docs | Auto OpenAPI (FastAPI) | **springdoc-openapi** | Same generated OpenAPI contract; frontend type generation (`types/api.ts`) is unaffected |
| Testing | Pytest + httpx + testcontainers-python | **JUnit 5 + MockMvc + Testcontainers** (Testcontainers is Java-native — this is a smoother fit, not a downgrade) | |
| Build tool | pip / pyproject.toml | **Maven** (or Gradle — team's choice, examples below assume Maven) | |
| Resilience | — (not previously specified) | **Resilience4j** (circuit breaker, timeout, fallback) | New — needed specifically for the Java→Python call at the FL boundary; see §3 |
| Object mapping | — | **MapStruct** | Compile-time DTO↔Entity mapping, keeps controllers from ever leaking JPA entities to the client |

**Unchanged:** PostgreSQL 16, Docker/Compose, Git/GitHub, React/TypeScript/Tailwind/shadcn/Recharts, and — inside the one remaining Python service — Flower and scikit-learn.

---

## 3. Revised System Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│  React + TypeScript SPA  — unchanged                              │
└───────────────────────────────┬────────────────────────────────────┘
                                │ REST / JSON (Bearer JWT)
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│  Spring Boot API  (container: api)                                │
│                                                                    │
│  Controllers → Services → Repositories → Entities (JPA)           │
│  Owns: auth, applications, resumes, interviews, prep, analytics,   │
│  recommendations, notifications, reports — everything user-facing │
│                                                                    │
│  FederatedLearningGateway (interface)                             │
│      └── FlowerServiceAdapter (impl) ──── wrapped in ────┐        │
│                                                    Resilience4j    │
│                                                  CircuitBreaker    │
└───────────────────────────────┬───────────────┬───────────────────┘
                                │ SQL (JPA)      │ REST (control-plane
                                ▼                │  only — sanitised
┌────────────────────────┐                       │  numeric export in,
│ PostgreSQL              │                       │  round metadata out)
│ schema: app             │                       ▼
│ (everything except      │           ┌──────────────────────────┐
│  fl_* tables)           │           │ fl-service (Python)       │
└────────────────────────┘           │ Flower client + server    │
          ▲                          │ scikit-learn training     │
          │ INSERT-only role,        │ clip + noise               │
          │ 3 tables only:           │                            │
          │ fl_rounds, fl_models,    │ NO credentials to read     │
          │ privacy_ledger_entries   │ applications/resumes/      │
          └──────────────────────────│ interviews/notes           │
                                     └──────────────────────────┘
```

**The one architectural addition this split requires: a Circuit Breaker.** In the original single-language design, the FL "server" was just another internal call. Now it's a network hop to a different process in a different language, which means it can be slow, or down, or mid-deploy — and the Recommendations page must never hang or 500 because of that. `FederatedLearningGateway` is called through a Resilience4j circuit breaker with a documented fallback: **if the FL service is unreachable, the API silently serves local-only recommendations** (exactly the "local" side of the Federated Lift toggle, US-65 — a feature the plan already had, now doing double duty as a genuine resilience fallback rather than only a demo toggle).

**Data flow into the FL boundary changed in one way, and it's an improvement.** In the all-Python version, each FL client read its own isolated schema. Now Java is the only thing that legitimately touches user content, so Java exports an already-sanitised numeric snapshot (the same 24-feature schema from the original plan, §22.2) into the FL service's own storage before a round starts. The privacy guarantee is identical — no free text, no identifiers, ever — but it's now enforced at **two** independent points instead of one: once by Java's export whitelist, again by Python's clip-and-noise step. NFR-10 and FR-92 hold exactly as written.

---

## 4. Design patterns — where and why

This is the heart of this revision. Every pattern below is tied to a real, already-planned piece of functionality — nothing is added just to demonstrate a pattern for its own sake, and a few parts of the system are called out explicitly as **deliberately plain**, because forcing a pattern where a plain service method already reads clearly is its own kind of bad design.

### 4.1 Pattern catalogue

| Pattern | Applied to | Problem it solves |
|---|---|---|
| **State** | `ApplicationStatus` and its transition rules (§17.5 of the original plan) | Replaces a sprawling if/else transition check with a class per status, each knowing its own legal next-states — makes FR-21 ("reject invalid transitions") a compile-time-checked property instead of a runtime string comparison |
| **Chain of Responsibility** | (a) The recommendation pipeline — candidate generation → tiering → scoring → federated re-rank → diversity filter (§21.2); (b) resume upload validation — extension check → MIME check → magic-byte check (US-21) | Each stage is independently testable and reorderable without touching the others; adding a new validation step or a new recommendation stage never means editing existing stages |
| **Strategy** | (a) `NotificationChannel` (in-app now; email/push are drop-in future strategies, §6.1); (b) `FileStorageStrategy` (local disk now, S3 later — this was already called "storage adapter" in the original plan; Strategy is the formal name for that shape); (c) evidence-tier scoring per tier (A/B/C/D use different weight formulas, §21.6) | Swapping the algorithm/implementation never touches the calling code |
| **Observer** | `ApplicationEventPublisher` (Spring's built-in event bus) — a status change, a stalled-application detection, or an interview date passing **publishes an event**; the notification service, the analytics cache invalidator, and (later) an email service all **subscribe** independently | The thing that detects "this application is stalled" doesn't need to know who cares about that fact — today it's an in-app notification, tomorrow it could also be an email, with zero change to the detector |
| **Template Method** | `AnalyticsMetric` abstract base class — defines the fixed skeleton (compute → check sample size → attach `sufficient_data` flag → format) once; each concrete metric (funnel, conversion rate, dwell time) only implements the one formula-specific step | This directly enforces FR-67 ("every computed rate shows its sample size") **structurally** — a new metric literally cannot skip the sample-size gate, because it's not theirs to skip |
| **Specification** | Spring Data JPA `Specification<Application>` for combinable filters (status, priority, tag, date range — FR-16) | Filters compose with `.and()`/`.or()` instead of a hand-rolled dynamic-query builder; adding a new filterable field is one new `Specification`, not a rewrite of a query method |
| **Adapter** | `FlowerServiceAdapter implements FederatedLearningGateway` | Isolates the *one* place that knows the Python service's REST contract; if that contract changes, or if the FL service is ever swapped for something else, exactly one class changes |
| **Facade** | `RecommendationFacade` (built in Sprint 4, phase P42) | Controllers call one method (`generateRecommendations(userId)`); the multi-step pipeline behind it (§21.2) stays internal, so a controller never becomes a place where business logic accumulates. *A `FederatedRoundFacade` is a natural future extraction if FL round orchestration grows more complex — not built in the current scope, and not claimed as built; today that orchestration lives inside `FlowerServiceAdapter` (§4.1, Adapter row).* |
| **Builder** | `ReportBuilder` (new — see §6.2), and `RecommendationCard` construction (many optional fields: evidence tier, observation count, explanation, target topic) | Avoids telescoping constructors for objects with many optional parts; a `ReportBuilder` in particular reads naturally: `.withDateRange(...).withApplications(true).withAnalytics(true).build()` |
| **Decorator** | `ValidatingFileStorage` wraps the base `FileStorageStrategy` implementation | Validation logic (size, type, signature — US-21) can be layered onto storage without the base storage class ever needing to know validation exists |
| **Repository** | Spring Data JPA repositories, each extending a shared `UserScopedRepository<T>` base that makes `userId` a required parameter on every finder method | This is the same enforcement mechanism the original plan described in §17.4 ("impossible to write a query that forgets it"); Spring Data just gives it a name and first-class framework support |
| **DTO + Mapper** | MapStruct-generated mappers between JPA entities and API DTOs | JPA entities never cross the controller boundary — prevents lazy-loading exceptions and accidental over-exposure of internal fields |
| **Circuit Breaker** *(resilience pattern, not GoF)* | Resilience4j around `FlowerServiceAdapter` | Covered in §3 — the FL service being slow or down must never break the Recommendations page |

### 4.2 Deliberately *not* patterned

Worth stating explicitly, because restraint is part of good design: `ApplicationController`, `ResumeController`, `InterviewController` and their services stay **plain CRUD** — a service method that does "validate, save, return" in five lines does not need a Factory, a Strategy, or a Command wrapping it. Over-applying patterns to simple operations is a real anti-pattern (sometimes called "pattern-itis"), and a marker who knows design patterns will notice restraint as much as they notice correct application.

### 4.3 One worked example, end to end

To make §4.1 concrete rather than a list of names, here's how three patterns cooperate for a single real feature — **"flag a stalled application and notify the user"** (FR-26 + the new notification feature, §6.1):

1. A scheduled job (or a status-change event) triggers `StalledApplicationDetector`.
2. The detector uses the `AnalyticsMetric` **Template Method** base to compute the user's own historical median dwell time for that stage, with the sample-size gate already built in for free.
3. If an application exceeds that threshold, the detector publishes a `StalledApplicationEvent` (Spring's **Observer** mechanism) — it does not know or care who's listening.
4. `NotificationListener` receives the event and hands it to `NotificationService`, which picks a `NotificationChannel` **Strategy** (in-app today) and creates the notification.
5. The whole call from the controller that triggered the check down to "notification created" is fronted by a **Facade** method, so nothing about this five-step chain ever leaks into a controller.

Five patterns, one feature, and every one of them is there because it solves an actual coupling problem in that feature — not because a pattern catalogue was gone through top-to-bottom looking for a place to use each entry.

---

## 5. Database changes (delta only)

Everything in the original §17 stands. Two additions, both required by the two new features in §6:

**`notifications`** — `id`, `user_id FK`, `type` (`stalled_application`|`upcoming_interview`|`missing_debrief`|`new_recommendation`|`fl_round_result`), `title`, `body`, `related_entity_type`, `related_entity_id`, `channel` (`in_app` for now), `is_read`, `created_at`. Indexed on `(user_id, is_read, created_at DESC)` for the notification bell's "unread first" query.

**`report_exports`** — `id`, `user_id FK`, `report_type` (`applications`|`analytics_summary`), `format` (`pdf`|`csv`), `date_range_from`, `date_range_to`, `file_path`, `generated_at`. Exported files follow the exact same storage rules as resumes (US-21/22): outside the web root, served only through an authenticated, expiring, per-user-scoped endpoint.

Both tables carry `user_id NOT NULL` and go through the same `UserScopedRepository` base as everything else — isolation is not a special case for the new features, it's inherited automatically.

---

## 6. Two deliberate additions

Both extend goals already in the original plan (G1 "single source of truth," G4 "make performance legible") rather than introducing anything off-topic. Both are scoped as **Should**, not **Must** — they do not compete with the FL work for priority.

### 6.1 In-app notifications — EPIC 10

**User stories** (points on the same scale as the original backlog):

- **US-76 — Notify me when an application stalls** · Should · 3 · Deps: FR-26
  As a job seeker, I want to be notified when an application is stalled, so that I don't have to keep checking the dashboard manually.
- **US-77 — Notify me before an interview** · Should · 3
  As a job seeker, I want a reminder ahead of a scheduled interview, so that I remember to prepare.
- **US-78 — Notify me about a missing debrief** · Should · 2
  As a job seeker, I want a nudge if I haven't logged feedback for a past interview, so that I don't lose the details while they're still fresh.
- **US-79 — Notification bell + mark as read** · Should · 3
  As a job seeker, I want a notification centre, so that I have one place to review what the app has flagged for me.

**New requirements:**

| ID | Requirement | Priority |
|---|---|---|
| FR-99 | The system shall publish a domain event whenever an application is flagged as stalled, an interview date passes without a debrief, or an interview is due within 24 hours. | Should |
| FR-100 | The system shall create an in-app notification from any subscribed event, scoped to the affected user only. | Should |
| FR-101 | The system shall allow a user to mark a notification as read, and to view unread notifications separately from read ones. | Should |
| NFR-53 | The notification channel mechanism shall be pluggable (Strategy pattern) so that email or push channels can be added without modifying event-detection logic. | Should |

This is intentionally the same "action queue" idea already in the plan (US-49) surfaced as a persistent, dismissible list rather than only a dashboard widget — same detection logic, reused via the Observer pattern in §4, not duplicated.

### 6.2 Report export (PDF/CSV) — extends EPIC 6

**User story:**

- **US-80 — Export my applications and analytics** · Should · 5 · Deps: EPIC 2, EPIC 6
  As a job seeker, I want to export my application list and summary analytics as a PDF or CSV, so that I can share my progress outside the app (e.g., with a mentor or career advisor) or keep an offline record.

**New requirements:**

| ID | Requirement | Priority |
|---|---|---|
| FR-102 | The system shall generate a PDF or CSV export of a user's applications, optionally filtered by date range. | Should |
| FR-103 | The system shall generate a PDF export of a user's analytics summary (funnel, conversion rates, sample sizes), reusing the exact same formulas and sample-size gating as the in-app analytics (§20.7 of the original plan). | Should |
| FR-104 | Exported files shall be retrievable only by their owner, through an authenticated, expiring download endpoint, and never publicly served. | Must |

FR-104 deliberately mirrors FR-32 (resume download security) word for word — the same rule, applied to a new file type, not a new rule invented for it.

**Why these two and not something flashier:** both are small, self-contained, additive, and — importantly — neither touches the Federated Learning boundary, so neither adds risk to the highest-risk sprint. They're the kind of addition that rounds out a submission without competing for attention with the actual innovation.

---

## 7. Revised folder / package structure

```
smart-job-tracker/
├── api/                                  # Spring Boot application (was backend/)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/cohort/api/
│       │   ├── ApiApplication.java
│       │   ├── config/                   # SecurityConfig, OpenApiConfig, Resilience4jConfig
│       │   ├── controller/                # thin — delegates to services/facades only
│       │   ├── dto/                       # request/response records
│       │   ├── mapper/                    # MapStruct interfaces
│       │   ├── entity/                    # JPA entities
│       │   ├── repository/
│       │   │   └── UserScopedRepository.java     # shared base, §4.1
│       │   ├── service/
│       │   │   ├── application/           # state machine (State pattern)
│       │   │   ├── resume/
│       │   │   │   └── storage/           # FileStorageStrategy + ValidatingFileStorage decorator
│       │   │   ├── interview/
│       │   │   ├── preparation/
│       │   │   │   └── analytics/AnalyticsMetric.java   # Template Method base
│       │   │   ├── recommendation/
│       │   │   │   ├── generator/         # Chain of Responsibility stages
│       │   │   │   ├── tiering/
│       │   │   │   └── RecommendationFacade.java
│       │   │   ├── notification/
│       │   │   │   ├── NotificationChannel.java        # Strategy interface
│       │   │   │   └── InAppNotificationChannel.java
│       │   │   ├── report/
│       │   │   │   └── ReportBuilder.java
│       │   │   └── federated/
│       │   │       ├── FederatedLearningGateway.java   # Adapter target interface
│       │   │       └── FlowerServiceAdapter.java        # wrapped in circuit breaker
│       │   └── event/                     # domain events (Observer)
│       └── test/java/com/cohort/api/      # JUnit 5, MockMvc, Testcontainers
│
├── fl-service/                            # unchanged in spirit from the original plan,
│   │                                       # smaller in scope — Flower + scikit-learn only
│   ├── Dockerfile
│   ├── requirements.txt
│   └── app/
│       ├── main.py                        # thin FastAPI control-plane wrapper
│       ├── client/                        # Flower NumPyClient, local_trainer, privacy (clip+noise)
│       ├── server/                        # Flower server, FedAvg strategy, min-cohort + norm filter
│       └── common/schema.py               # FEATURE_SCHEMA_V1 — single source of truth, as before
│
├── frontend/                              # unchanged
├── docs/                                  # unchanged, plus this file and new ADRs
└── docker-compose.yml                     # api (Java) + fl-service (Python) + db + web
```

---

## 8. Backlog and sprint impact

- **EPIC 10 (Notifications, §6.1)** slots into **Sprint 5**, alongside the other *Should*-priority hardening work — it does not displace anything already committed.
- **Report export (§6.2)** also slots into **Sprint 5**, immediately after analytics is stable, since it deliberately reuses analytics formulas rather than inventing new ones.
- **Sprint 1** gains one extra platform story: **"Spring Boot + Flyway + Testcontainers bootstrap"** replacing the old FastAPI/Alembic/pytest bootstrap story — same points (5), same owner role (Platform/DevOps).
- **Sprint 4 (the FL sprint)** gains one story: **"FederatedLearningGateway + Resilience4j circuit breaker + fallback to local-only ranking"** (5 points) — this is new work created directly by the language split, and it belongs in the same sprint as the rest of the FL integration since it's testing the same boundary.
- Total backlog: **+16 points** across the two new features (11 for notifications: US-76 3 + US-77 3 + US-78 2 + US-79 3; 5 for report export: US-80), **+5 points** for the circuit breaker story — a total addition of **+21 points**. Recall from the original plan (§10.2) that a 10-person team has substantial headroom (~120–150 pts/sprint capacity against sprints sized 43–90) — this addition fits comfortably without triggering the cut list.

---

## 9. New and updated risks

| ID | Risk | Prob. | Impact | Mitigation |
|---|---|---|---|---|
| R14 | Java/Python interop friction at the FL boundary (serialization mismatches, contract drift) | Medium | Medium | `FederatedLearningGateway` is the single point of contact; contract defined once via OpenAPI/JSON Schema shared between both services and tested on both sides |
| R15 | Team has stronger Python instincts than Java, given the original plan's stack | Medium | Medium | Front-load Spring Boot fundamentals in Sprint 1; keep the fl-service intentionally small (Flower + scikit-learn only) so Python depth needed is minimal |
| R16 | Pattern over-application ("pattern-itis") drifting the codebase toward unnecessary complexity | Low–medium | Low | §4.2's explicit "deliberately not patterned" list is a standing review checklist, not just a one-time note |
| R17 | Circuit breaker fallback silently hides a genuinely broken FL service instead of surfacing it | Low | Medium | Fallback triggers a logged, monitorable event even though the user-facing response degrades gracefully — "graceful" is not the same as "silent" |

---

## 10. Definition of Done — additions

Every PR touching a pattern-bearing class (§4.1) must state, in the PR description, **which pattern it implements or extends and why** — one sentence is enough, but it must exist, otherwise reviewers cannot tell deliberate design from accidental complexity. The Sprint 4 DoD additionally requires: a fault-injection test that stops the `fl-service` container mid-request and asserts the Recommendations endpoint still returns a 200 with local-only results, not a 500.
