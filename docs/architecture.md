# Cohort architecture

How Cohort is built: the parts of the system, the technology, the code structure, the design patterns, the data model, and the reasons behind the main design choices. The recommendation engine and the federated learning service are described in [`recommendations-and-fl.md`](recommendations-and-fl.md). The interface design is in [`design.md`](design.md).

## 1. System overview

### 1.1 Context

```
   Job seeker  ---- HTTPS ---->  Cohort

   Outside calls Cohort makes:
   - Google, GitHub or LinkedIn, for sign-in only (identity, never job data)
   - a hosted AI provider (Gemini), only when the operator turns it on, and shown to users when active
```

Cohort shares no user data with third parties.

### 1.2 Containers

```
+------------------------------------------------------------------+
|  React + TypeScript single-page app (frontend/)                  |
+-------------------------------+----------------------------------+
                                | REST / JSON, Bearer JWT
                                v
+------------------------------------------------------------------+
|  Spring Boot API (api/)                                          |
|  controller -> service -> repository -> entity                   |
|  auth, applications, resumes, interviews, prep, analytics,       |
|  recommendations, notifications, reports, AI features            |
|                                                                  |
|  FederatedLearningGateway                                        |
|    FlowerServiceAdapter, called through a circuit breaker        |
+---------------+---------------------------+----------------------+
                | JDBC / JPA                | HTTP: sanitised numeric
                v                           | features in, round results
+--------------------------+                | and model out
| PostgreSQL 16            |                v
| public schema: app data  |   +-------------------------------+
| fl schema: rounds and    |   | fl-service (Python)           |
| Privacy Ledger           |   | Flower FedAvg strategy,       |
+--------------------------+   | scikit-learn, clip + noise,   |
                               | FastAPI control endpoints     |
                               | no database credentials       |
                               +-------------------------------+
```

The Spring Boot API is the only process that can read user content. The fl-service receives only a fixed list of 24 bounded numbers per opted-in user and returns round results. Model updates never pass through `/api/v1`.

## 2. Technology stack

| Layer | Choice | Reason |
|---|---|---|
| API | Java 21, Spring Boot 3.5 | Dependency injection, security, validation and data access in one framework, and a good fit for the design patterns used |
| Data access | Spring Data JPA, Hibernate (`ddl-auto: validate`) | Parameterised queries by default, one repository layer where user scoping is enforced, and the `Specification` API for combinable filters |
| Database | PostgreSQL 16 | The data is relational; JSONB holds resume section metadata; roles make the FL schema isolation enforceable |
| Migrations | Flyway | Versioned SQL; Hibernate only validates the schema, it never changes it |
| Validation | Jakarta Bean Validation plus custom validators | Bad input is rejected at the API edge with field errors |
| Security | Spring Security, self-issued JWT, Argon2id, OAuth2 client | Stateless API with revocable refresh tokens; Argon2id is memory-hard, which makes cracking a stolen database slow |
| Resilience | Resilience4j | Circuit breaker around the call to the FL service |
| Reports | OpenPDF and a CSV writer | PDF and CSV exports without an external service |
| API description | springdoc-openapi | The OpenAPI document lists every endpoint for the security test suite; it is not served in a running app |
| Frontend | React 19, TypeScript, Vite | Typed components and fast builds |
| Styling | Tailwind CSS 4, shadcn/ui (Radix) | Shared design tokens and accessible components owned as source |
| Server state | TanStack Query | Caching, loading and error states, and one place for the token refresh |
| Charts | Recharts | Funnel, conversion and activity charts |
| Federated learning | Python, Flower, scikit-learn, NumPy, FastAPI | Flower has no Java equivalent, so Python is limited to this one service |
| AI | Ollama (`llama3.2:3b`) by default, Google Gemini optional | User text stays on the server unless the operator opts into the hosted provider |
| Tests | JUnit 5, MockMvc, Testcontainers, Vitest, React Testing Library, pytest | Backend tests run against a real PostgreSQL |
| Runtime and CI | Docker Compose, GitHub Actions | The same containers on every machine; CI builds, tests and checks security gates |

Redis, a message broker, object storage, Next.js and deep learning frameworks are not used. Nothing at this scale needs a cache or a queue, storage sits behind an interface, server rendering brings nothing to a signed-in dashboard, and the model is a small logistic regression.

## 3. Backend structure

```
api/src/main/java/com/cohort/api/
  config/          CORS, clock, report storage, FL service client
  controller/      thin REST controllers, one per resource
  dto/             request and response records
  mapper/          hand-written entity <-> DTO mappers
  entity/          JPA entities
  event/           domain events (stalled application, upcoming interview, missing debrief, cold contact)
  exception/       typed exceptions mapped to the error envelope
  repository/      Spring Data repositories on a UserScopedRepository base
    specification/ combinable application filters
  security/        JWT, Argon2id, login rate limit, security headers
    oauth/         Google, GitHub and LinkedIn sign-in
  validation/      custom validators (clean names, URLs, plausible dates, salary range, text quality)
  service/
    account/       password change, data export, account deletion
    application/   CRUD, stall detection, CSV import
      state/       one class per application status
    resume/storage/ storage strategy and validating decorator
    preparation/   question bank, mastery engine, readiness, review queue, mock interview
      analytics/   AnalyticsMetric base class
    analytics/     funnel, conversions, timeline, rejections, dwell times
    detection/     stall, interview, debrief and cold-contact detectors
    notification/  channel strategy and listener
    recommendation/ generator chain, tiering, templates, facade
    report/        ReportBuilder and renderers
    mail/          mail sender strategy
    calendar/      .ics export
    offer/         offer comparison and scoring
    ai/            AI provider strategy and AI features
    fl/            FL gateway, adapter, circuit breaker, export and ledger
    *.java         one service per plain resource (auth, tokens, interviews, contacts, goals, stories, search, ...)
```

Layering is strict: controllers handle HTTP only and call services; services hold the rules and call repositories; repositories return entities; mappers turn entities into DTOs before anything leaves a controller. Entities never appear in an API response. The JSON contract uses snake_case through a global Jackson naming strategy.

## 4. Design patterns

### 4.1 Catalogue

| Pattern | Where | Why |
|---|---|---|
| State | `service/application/state/`: one class per status, looked up through `ApplicationStatusStateRegistry` | Each status knows its own legal next statuses, so an invalid transition cannot be written by accident. The board uses the same rules to refuse illegal drags |
| Repository | `UserScopedRepository<T>`, extended by every user-owned repository | The owner id is a required argument on every finder, so a query cannot forget the user scope |
| DTO and Mapper | `dto/` and `mapper/` | Entities never cross the controller boundary, which avoids lazy-loading errors and leaking internal fields |
| Specification | `repository/specification/ApplicationSpecifications` | Status, priority, tag, date and text filters combine with `.and()`; a new filter is one new specification |
| Strategy | `FileStorageStrategy`, `NotificationChannel`, evidence-tier scoring, `MailSender`, `AiProvider` | The implementation can change (local disk to object storage, Ollama to Gemini) without touching the calling code |
| Decorator | `ValidatingFileStorage` around the storage strategy; `FlCircuitBreakerGateway` around the FL gateway | Validation and fault handling are added without the wrapped class knowing |
| Chain of Responsibility | Upload checks (extension, MIME type, magic bytes); recommendation generators | Each step is tested on its own and steps can be added without editing the others |
| Template Method | `AnalyticsMetric`: compute, then the sample-size gate, then format | A new metric implements only its formula and cannot skip the sample-size rule |
| Observer | Detectors publish domain events through Spring's `ApplicationEventPublisher`; `NotificationListener` subscribes | A detector does not know who reacts to what it found |
| Builder | `ReportBuilder`; `RecommendationCard` | Objects with many optional parts are assembled step by step instead of through long constructors |
| Facade | `RecommendationFacade` | The controller calls one method; the multi-step pipeline stays inside |
| Adapter | `FlowerServiceAdapter` implements `FederatedLearningGateway` | Only one class knows the fl-service REST contract |
| Circuit Breaker | Resilience4j in `FlCircuitBreakerGateway` | A slow or stopped FL service never breaks the Recommendations page; it falls back to local-only ranking |

Plain CRUD services (applications, resumes, interviews, contacts, offers) stay plain. A five-line "validate, save, return" method does not get a pattern around it.

### 4.2 Worked example: flag a stalled application and notify the user

1. `NotificationDetectionJob` runs every 15 minutes and calls `StalledApplicationDetector`.
2. The detector uses the stall metric, built on the `AnalyticsMetric` Template Method base, to get the user's own median time in each stage, with the sample-size gate built in.
3. For an application past that threshold it publishes a `StalledApplicationEvent` (Observer). It does not know who is listening.
4. `NotificationListener` receives the event and hands it to `NotificationService`, which sends it through each configured `NotificationChannel` (Strategy). The in-app channel stores the notification for that user only.
5. The same detection result feeds the dashboard action queue, so the logic is not repeated.

## 5. Data model

### 5.1 Tables

| Area | Tables |
|---|---|
| Accounts | `users`, `refresh_tokens`, `auth_tokens` (password reset and email verification), `oauth_identities` |
| Applications | `companies`, `applications`, `application_status_history`, `tags`, `application_tags` |
| Resumes | `resumes`, `resume_sections` |
| Interviews | `interviews`, `interview_questions`, `interview_feedback` |
| Preparation | `topics`, `topic_prerequisites`, `questions`, `practice_attempts`, `preparation_tasks` |
| Recommendations | `recommendations`, `recommendation_evidence` |
| Notifications and reports | `notifications`, `report_exports` |
| Job-search workflow | `contacts`, `company_notes`, `offers` and the offer scoring tables, `goals` |
| Behavioural prep | `stories` |
| Company directory | directory profiles and role patterns, `experience_contributions` |
| AI | `ai_usage_log` (sizes and timings only, no prompt text) |
| Federated learning | `fl.fl_rounds`, `fl.privacy_ledger_entries` |

Every table has a UUID primary key and `created_at`; mutable tables also have `updated_at`.

### 5.2 Relationships

```
users 1---* applications *---1 companies
  |             |-- 1---* application_status_history
  |             |-- 1---* interviews --1---* interview_questions *---1 topics
  |             |              `--1---1 interview_feedback
  |             |-- *---1 resumes
  |             `-- *---* tags (application_tags)
  |-- 1---* resumes --1---* resume_sections
  |-- 1---* questions (own bank) *---1 topics
  |-- 1---* practice_attempts *---1 questions
  |-- 1---* preparation_tasks *---1 topics
  |-- 1---* recommendations --1---* recommendation_evidence
  |-- 1---* contacts, offers, goals, stories, notifications, report_exports
  `-- 1---* fl.privacy_ledger_entries *---1 fl.fl_rounds

topics *---* topics (topic_prerequisites, kept acyclic)
```

- `application_status_history` is append-only. It is the source for the funnel, because the current status alone cannot tell whether an application ever reached an interview.
- `interviews` and `interview_questions` carry `user_id` directly, so every isolation check is a single-table filter.
- `interview_questions.linked_question_id` points at the question bank, which is how an interview question is saved into the user's own bank.
- `practice_attempts` is append-only, so repeated attempts give a confidence history instead of one overwritten value.
- `recommendation_evidence` stores the rows behind each recommendation, so "why am I seeing this?" is answered from the database.
- Account deletion cascades through every owned table; a test checks that no orphan rows remain.

### 5.3 Application status state machine

Statuses: Saved, Applied, OA, Interview, Final Round, Offer, Rejected, Withdrawn, Accepted, Declined.

```
 Saved --> Applied --> OA --> Interview --> Final Round --> Offer --+--> Accepted
             |          |         |              |            |     `--> Declined
             `----------+---------+--------------+------------+--> Rejected
             `----------+---------+--------------+------------+--> Withdrawn
```

- Forward skips are allowed (Applied to Interview), because employers skip stages.
- A backward move needs a correction note and is written to the history like any other change.
- Terminal statuses (Rejected, Withdrawn, Accepted, Declined) have no further moves. An illegal move returns 409 with the reason.

### 5.4 Migrations

Flyway migrations live in `api/src/main/resources/db/migration/`. Versioned scripts (`V1` onward) create and change tables; repeatable scripts (`R__question_bank.sql`, `R__topic_descriptions.sql`) hold the question bank and topic descriptions and are re-applied when their content changes. A versioned script is never edited after it is applied, because Flyway checks its checksum. Every user-owned table has a hand-written rollback script in `db/rollback/`, since Flyway Community has no undo. The development seed data is loaded only under the `dev` profile.

## 6. Data isolation

Three independent layers keep one user's data away from another:

1. Schema: `user_id NOT NULL` (or `created_by_user_id` for companies) on every owned table, with a foreign key and cascade.
2. Repository: `UserScopedRepository<T>` makes the owner id a required argument on every finder. Controllers never reach repositories directly.
3. Tests: every user-owned endpoint has a cross-user test, and a suite built from the OpenAPI document checks every endpoint for 401 without a token and 404 on another user's resource.

A request for another user's resource returns 404, never 403, so the API does not confirm that the resource exists. The user id always comes from the token; a `user_id` in a request body is ignored.

## 7. API

- Base path `/api/v1`, JSON in snake_case, bearer token on every authenticated endpoint.
- Errors use one envelope: `{"error": {"code": "...", "message": "...", "field_errors": {...}}}`.
- List endpoints take `page`, `size` and `sort`. Filtering happens on the server.
- Status codes: 400 or 422 for validation, 401 without a valid token, 404 for missing or foreign resources, 409 for conflicts and illegal transitions, 429 for rate limits, 503 when the AI provider is unavailable.

| Group | Base paths |
|---|---|
| Auth and account | `/auth`, `/auth/oauth`, `/users` |
| Applications | `/applications`, `/applications/import`, `/applications/{id}/readiness`, `/applications/{id}/email-drafts`, `/validate` |
| Companies and contacts | `/companies`, `/contacts`, `/directory/companies`, `/directory/contributions` |
| Resumes | `/resumes`, `/resumes/{id}/sections` |
| Interviews | `/interviews`, `/interview-questions`, `/interviews/{id}/prep-pack`, `/calendar` |
| Preparation | `/questions`, `/topics`, `/preparation`, `/prep` (review queue, mock interview), `/stories` |
| Insights | `/analytics`, `/recommendations`, `/reports`, `/goals`, `/offers` |
| Other | `/notifications`, `/search`, `/sample-data`, `/ai/status` |
| Federated learning | `/fl/consent`, `/fl/opt-in`, `/fl/opt-out`, `/fl/schema`, `/fl/privacy-ledger` |
| Development only | `/dev/fl/round`, present only under the `dev` profile |

There is no endpoint for submitting a model update. Updates are produced inside the FL service and never travel through the application API.

## 8. Frontend

```
frontend/src/
  App.tsx          routes (public pages, then protected pages inside the app shell)
  pages/           one folder per area: applications, interviews, prep, analytics, privacy, ...
  components/
    ui/            shadcn/ui primitives
    layout/        app shell, sidebar, mobile navigation, auth frame, theme toggle
    shared/        page header, section card, data table, timeline, status badge, empty and error states
    board/         Ascent staircase and animated figures
    <area>/        components owned by one area (applications, prep, dashboard, ...)
  lib/             one typed API client per resource (*-api.ts), formatting, validation, preferences
  hooks/           shared hooks
```

- Server data goes through TanStack Query. A single-flight refresh in the API client means many 401s at once trigger only one token refresh.
- Tokens are kept in local storage. `ProtectedRoute` sends signed-out users to the login page and back to where they were going afterwards.
- Forms use local state with validation rules that mirror the server's, plus unsaved-change warnings and saved drafts for long forms.
- Routes and heavy charts are loaded lazily. Data pages show skeletons in the shape of the final layout.
- Light and dark themes come from design tokens; a contrast test checks every token pair. The visual system is described in [`design.md`](design.md).

## 9. AI assistance

- `AiProvider` is a Strategy with two implementations: `OllamaProvider` (local, the default) and `GeminiProvider`. Feature code only talks to `AiService`, so switching provider touches no feature.
- Gemini is used only when `AI_PROVIDER=gemini` and `GEMINI_API_KEY` are both set. Without the key the app stays on the local model. `GET /api/v1/ai/status` reports the provider and whether it is hosted, and every AI screen shows a notice when it is.
- AI features: the interview prep pack, the resume and job description match with a cover letter draft, and follow-up and negotiation email drafts. Drafts are never sent automatically.
- Grounding: prompts are built on the server from the requesting user's own rows only. The client sends nothing but its own text.
- Failure is a normal state. Connect and read timeouts are set; any error becomes 503 `AI_UNAVAILABLE`, and pages show the non-AI content (for example the prep pack falls back to a template summary).
- Each user has a sliding-window rate limit (30 calls per hour by default), returned as 429 `AI_RATE_LIMITED`.
- `ai_usage_log` records the user, feature, provider, success, sizes and duration. Prompt and reply text are never stored or logged.
- Model output that should be JSON is validated before use and rejected when malformed.

## 10. Notifications and report export

Notifications. Detectors run on a schedule and publish domain events for a stalled application, an interview in the next 24 hours, a past interview without a debrief, and a contact not reached for 30 days. `NotificationListener` turns each event into an in-app notification for the affected user. The bell shows unread items first, and items can be marked read. A new channel is a new `NotificationChannel` implementation; the detectors do not change.

Reports. `ReportBuilder` assembles an applications report (PDF or CSV), an analytics summary (PDF) or a progress report (PDF), reusing the same formulas and sample-size gating as the Analytics page. Files are stored outside any served directory, expire after 7 days, are capped per user, and are downloaded only by their owner through an authenticated endpoint, the same rule as resume files.

Calendar. Each interview can be downloaded as an `.ics` event, and `/calendar/upcoming.ics` exports all upcoming interviews and application deadlines.

## 11. Security and privacy controls

| Area | Controls |
|---|---|
| Passwords | Argon2id (64 MiB memory, 3 iterations), rehashed on login if the parameters change; length policy with a blocklist |
| Sessions | Access tokens last 15 minutes. Refresh tokens last 7 days, are stored as hashes, rotate on use and are revoked on logout; a password change revokes other sessions |
| Login | Rate limited per email; register, login and password reset give the same answer and similar timing whether or not the email exists |
| Account recovery | Password reset and email verification tokens are single-use and expire |
| Social sign-in | Google, GitHub and LinkedIn issue an identity only; the app then issues its own tokens. An existing account is linked only when the provider marks the email verified |
| Authorisation | Repository-level user scoping; 404 on foreign resources |
| Input | Bean Validation with unknown fields rejected; length caps; text-quality checks on free text; server rules are the authority |
| Files | 5 MB limit; extension, MIME type and magic-byte checks; generated file names; stored outside served paths; owner-only expiring downloads |
| Database | Parameterised queries only; CI rejects SQL built from runtime values |
| Headers | `Content-Security-Policy`, `Strict-Transport-Security`, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` on every response |
| Secrets | Read from the environment only; `.env` is git-ignored; CI scans every commit for secrets |
| Account data | Full JSON export of everything a user owns; account deletion removes every owned row and the user's exported FL features |
| Federated learning | Opt-in and off by default; clipping and noise; minimum cohort; poisoning filter; the FL service has no database credentials; every transmitted vector is visible in the user's Privacy Ledger |

## 12. Deployment and running

`docker-compose.yml` starts three containers:

| Container | What it runs | Port |
|---|---|---|
| `cohort-db` | PostgreSQL 16 with a named volume and a health check | 5433 on the host (`POSTGRES_HOST_PORT`) |
| `cohort-api` | Spring Boot API; Flyway migrates on start | 8080 (`API_PORT`) |
| `cohort-web` | Vite dev server for the frontend | 5173 (`FRONTEND_PORT`) |

`docker-compose.fl.yml` adds `cohort-fl-service` on port 8000 and points the API at it. The FL service gets no main-database credentials, and a CI job checks this on a running stack.

Configuration comes from the environment (`.env`, based on `.env.example`):

| Variables | Purpose |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `FL_POSTGRES_PASSWORD` | Database and the restricted FL role |
| `JWT_SECRET`, `JWT_ACCESS_TOKEN_TTL_MINUTES`, `JWT_REFRESH_TOKEN_TTL_DAYS` | Tokens |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `LINKEDIN_CLIENT_ID`, `LINKEDIN_CLIENT_SECRET` | Social sign-in. A provider without both values is not registered and its button is hidden |
| `APP_FRONTEND_BASE_URL`, `APP_CORS_ALLOWED_ORIGINS` | Frontend origin |
| `AI_PROVIDER`, `OLLAMA_BASE_URL`, `OLLAMA_MODEL`, `GEMINI_API_KEY` | AI provider |
| `APP_FL_SERVICE_BASE_URL`, `APP_FL_EXPORT_CRON`, `APP_FL_ROUND_CRON` | FL service address and schedule (export every 6 hours, round 30 minutes later) |
| `APP_MAIL_SENDER`, `RESUME_STORAGE_DIR`, `REPORT_STORAGE_DIR` | Mail and file storage |

The OAuth redirect address for each provider is `http://localhost:8080/login/oauth2/code/{google|github|linkedin}` in local development.

The default `dev` profile loads a seed dataset marked as sample data (sign in as `seed.demo.user@example.com` / `SeedDemo123!`). New users can also load removable sample data from the onboarding page.

## 13. Key design choices

Federated learning sits behind a process and language boundary. The Spring Boot API is the only component with credentials for user content. The Python fl-service is small (Flower, scikit-learn, clipping and noise) and talks to the API through one narrow contract: allow-listed numeric features in, round results and model parameters out. `FlowerServiceAdapter` is the only Java class that knows this contract. Keeping Python to this one service gives the rest of the application one language and one toolchain, while the FL code uses the libraries that only exist in Python.

There is no endpoint for model updates. If clipped and noised updates were posted to the application API, the one process that holds every user's data would also see every update linked to a signed-in session. Updates are produced and aggregated inside the FL service, so the claim that the FL side never sees user data holds for the whole path.

The FL service may be down. It is a network call to another process, so it is wrapped in a circuit breaker. When it fails, recommendations are ranked from local data only and the page still returns 200.

FedAvg uses uniform weighting. Standard FedAvg weights each client by its number of training examples. Here that number is how many practice attempts and interviews a user logged, which reveals how intense their job search is. Every client counts equally and no sample count is ever sent. This costs a little statistical efficiency and removes a side channel.

Companies are scoped per user. A shared company table would reveal who is applying where through row creation and would need merge logic for names like "Google" and "Google LLC". Each user has their own company rows, uniqueness is per user, and the company repository uses the same user-scoped base as everything else. The cost is duplicate rows across users, and no cross-user company statistics can come from this table.

Access tokens are short JWTs, refresh tokens live in the database. A signed 15-minute access token keeps the API stateless, so more than one instance can run. Refresh tokens are opaque, hashed and revocable, so logout takes effect at once instead of when the access token expires. A stolen access token stays valid for at most 15 minutes.

Passwords use Argon2id. It is memory-hard, which makes GPU cracking expensive, and it is the current OWASP recommendation. Login also runs a dummy hash for unknown emails so response timing does not reveal whether an account exists.

Social sign-in issues the app's own tokens. The provider flow runs in its own filter chain on `/oauth2/**` and `/login/oauth2/**`; the API chain stays stateless. After a provider sign-in, the app issues the same tokens as a password login and discards the provider's tokens. A sign-in resolves in this order: a known provider identity signs in; otherwise an account with the same email is linked only if the provider says the email is verified; otherwise a new account is created. GitHub's verified address is read from its email endpoint. LinkedIn is configured by hand as an OpenID Connect provider with the `openid profile email` scopes, and its `email_verified` claim decides linking. Identities are stored by the stable `(provider, subject)` pair, not by email, because providers let users change their email. Refusing unverified emails stops someone who registers a victim's address at a provider from taking over the account. LinkedIn gives a verified identity only; it does not let apps read a user's job applications, so applications from LinkedIn come in through its CSV export or the save bookmarklet.

AI is local first. AI features handle personal text (notes, salary hopes, interview details), so the default provider is a local Ollama model and nothing leaves the server. A hosted provider is an operator choice and is always disclosed. AI is never required: every page works when the model is down.

Recommendations come from rules, with a bounded learned adjustment. Rules over the user's own data produce something useful on the first day and can point at the exact evidence behind each suggestion. The federated model only re-ranks within a bounded range, so it can reorder suggestions but never create or hide one, and its effect can be switched on and off on the Recommendations page.

Numbers show their sample size. Every rate carries its sample size and a `sufficient_data` flag, and the `AnalyticsMetric` base class makes this impossible to skip. A topic with fewer than two observations is shown as "insufficient data", not as a score.

Recommendations never claim a cause. Wording depends on the evidence tier, and a test fails the build if any template contains causal phrases such as "guarantees" or "because of this you were rejected".

The interface avoids a few common dashboard habits: the board is a second view next to a filterable table, there is no row of generic stat cards and no pie chart of statuses, and the federated part runs as real processes rather than appearing only in a diagram.
