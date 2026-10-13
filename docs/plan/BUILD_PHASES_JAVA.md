# Cohort — Build Phases

This file is the build sequence for Cohort: 110 phases across nine sprints, one commit per phase. Spring Boot owns the whole application; Python is limited to the Flower federated-learning service. Read it together with `PROJECT_OVERVIEW.md` (the plain-language map), `PROJECT_PLAN.md` (requirements, user stories, sprint roadmap) and `ARCHITECTURE_REVISION_JAVA.md` (architecture and design patterns).

| Sprint | Goal | Phases |
|---|---|---|
| 1 | Foundation | P01–P15 |
| 2 | Resumes and interview capture | P16–P26 |
| 3 | Preparation and analytics | P27–P38 |
| 4 | Recommendations and federated learning | P39–P58 |
| 5 | Notifications, reports and account | P59–P68 |
| 6 | Experience and core gaps | P69–P81 |
| 7 | Job-search workflow | P82–P88 |
| 8 | AI and intelligence | P89–P104 |
| 9 | Hardening and delivery | P105–P110 |

## How to use this plan

1. Keep `PROJECT_OVERVIEW.md`, `PROJECT_PLAN.md`, `ARCHITECTURE_REVISION_JAVA.md` and this file together in `docs/plan/`.
2. Read `PROJECT_OVERVIEW.md` first for the big picture, then `PROJECT_PLAN.md` for requirements and `ARCHITECTURE_REVISION_JAVA.md` for the design-pattern rationale. Execute this file one phase at a time: finish the phase, run its verification checks, commit with the exact message given, then stop for review before starting the next phase.
3. Review every commit's diff before continuing; this is also how each member learns the codebase well enough to defend it in a viva.
4. Never merge two phases into one commit, even if a phase looks small.

## Global rules

- Conventional commit format: `type(scope): description (US-XX)`; types: `feat`, `fix`, `test`, `docs`, `chore`, `refactor`, `spike`.
- **Every commit that adds or changes a pattern-bearing class (see the Pattern column) must state which pattern it implements or extends, in one sentence, in the PR description.** This is a Definition-of-Done requirement from `ARCHITECTURE_REVISION_JAVA.md` §10.
- Every phase adding backend logic ships with tests in the **same commit**: JUnit 5 for units and slices, MockMvc for controllers, Testcontainers for anything touching real Postgres.
- Every phase touching user-owned data includes the cross-user isolation test for each new endpoint (404 on foreign access).
- Run `mvn test` (Java) or `pytest` (fl-service) before every commit; never commit a red suite.
- No phase starts before the phases in its **Depends on** column are committed.

## Team and ownership

The team works in three departments. Each department builds its own phases and tests its own work; every phase is reviewed by someone other than its author.

| Department | Members | Typical phases |
|---|---|---|
| Backend | Anuj Madhani, Harshit Goyal, Achintya Chandwani, Dev Bhavsar | Migrations, Spring Boot services and endpoints, security, analytics, recommendations, notifications, reports |
| Frontend | Anshul Rai, Chinmay Dak | React pages and components, design system, accessibility |
| AI/ML | Aaditya Kaushik, Nirmal Nakum, Ved Dhanani, Abhishek Singh | fl-service (Flower, scikit-learn), AI provider and AI features, CI and environment set-up |

**Testing.** Each person tests their own department's work: Backend writes the JUnit, MockMvc and Testcontainers tests for its phases, Frontend writes the React Testing Library tests and accessibility checks, and AI/ML writes the pytest suites and AI fixture tests.

**Sprint 1 owners.** The Sprint 1 table keeps its member codes (P1–P10) in the Owner and Reviewer columns. The named owner of each Sprint 1 phase is:

| Phase | Department | Owner |
|---|---|---|
| P01 | AI/ML | Nirmal Nakum |
| P02 | AI/ML | Abhishek Singh |
| P03 | Backend | Anuj Madhani |
| P04 | Backend | Achintya Chandwani |
| P05 | Backend | Harshit Goyal |
| P06 | Backend | Dev Bhavsar |
| P07 | Frontend | Anshul Rai |
| P08 | Frontend | Chinmay Dak |
| P09 | Backend | Achintya Chandwani |
| P10 | Backend | Anuj Madhani |
| P11 | Backend | Dev Bhavsar |
| P12 | Frontend | Anshul Rai |
| P13 | AI/ML | Ved Dhanani |
| P14 | Backend | Harshit Goyal |
| P15 | AI/ML | Aaditya Kaushik |

From Sprint 2 onward the Owner column names the owning department; the department assigns each phase to one of its members at sprint planning. Work happens on branches named `feature/US-XX-description`.

---

## SPRINT 1 — Foundation

| # | Owner | Reviewer | Commit message | Scope | Pattern | Verify before commit |
|---|---|---|---|---|---|---|
| P01 | P10 | P1 | `chore: bootstrap monorepo structure (US-67)` | `.gitignore`, `README.md`, `LICENSE`, `CONTRIBUTING.md`, `.env.example`, empty `api/` `fl-service/` `frontend/` `docs/` dirs | — | Repo clones clean, no secrets present |
| P02 | P10 | P1 | `chore: docker-compose dev environment (US-68)` | `docker-compose.yml` (db + api + web; fl-service stubbed until Sprint 4), `api/Dockerfile`, `frontend/Dockerfile` | — | `docker compose up` starts db/api/web |
| P03 | P10 | P2 | `feat(api): Spring Boot skeleton + health endpoint (US-67)` | `ApiApplication.java`, `config/` package, `GET /health` | — | `curl localhost:8080/health` returns 200 |
| P04 | P1 | P10 | `feat(db): users + refresh_tokens Flyway migration (US-01)` | `V1__users.sql`, `V2__refresh_tokens.sql` per PROJECT_PLAN §17.2 | — | Flyway migrates cleanly on empty db; `flyway:undo` (or a rollback script) verified |
| P05 | P1 | P2 | `feat(auth): Spring Security + JWT + Argon2id (US-01,02,03,04)` | `SecurityConfig`, `AuthService`, `Argon2PasswordEncoder`, `/auth/*` endpoints, rate limiting | — | JUnit + MockMvc: happy path, bad password, 401s, no email enumeration |
| P06 | P1 | P3 | `feat(auth): profile endpoints (US-05)` | `GET/PATCH /users/me` | — | MockMvc test incl. 401 unauthenticated |
| P07 | P10 | P4 | `chore(frontend): Vite+React+TS+Tailwind+shadcn scaffold (US-71)` | Frontend scaffold, `AppShell`, `Sidebar`, routing skeleton, 404 page | — | `npm run dev` renders shell |
| P08 | P1 | P5 | `feat(frontend): login, register, protected routes, token refresh (US-01..04)` | Login/Register pages, `ProtectedRoute`, TanStack Query with refresh interceptor | — | RTL tests; manual login works end to end |
| P09 | P2 | P6 | `feat(db): companies, applications, status_history, tags migration (US-09)` | Flyway scripts per §17.2/§17.5 | — | Migrates cleanly |
| P10 | P2 | P7 | `feat(applications): State pattern for status transitions + Repository base (US-14)` | `ApplicationStatus` state classes (one per status, each owning its legal transitions), `UserScopedRepository<T>` base requiring `userId` on every finder | **State + Repository** | Unit tests: every valid/invalid transition pair parametrised |
| P11 | P2 | P8 | `feat(applications): CRUD + status endpoints (US-09..15)` | `ApplicationController` → `ApplicationService` → repository, DTOs + MapStruct mapper | **DTO + Mapper** (cross-cutting — applied in every CRUD phase from here on, not called out again per-phase) | Cross-user isolation test (404) passing |
| P12 | P2 | P9 | `feat(frontend): applications list, detail, form, timeline (US-09..15)` | `DataTable`, `StatusBadge`, `Timeline`, application pages | — | Empty/loading/error states present |
| P13 | P10 | P1 | `chore(ci): Maven build, JUnit, JaCoCo coverage gate (US-70)` | `.github/workflows/ci.yml` | — | CI green on a trivial PR |
| P14 | all | P10 | `test: sprint 1 isolation + integration suite (US-73 partial)` | Register→login→create→status→history Testcontainers integration test | — | Coverage ≥70% (JaCoCo) |
| P15 | P10 | — | `docs: sprint 1 docs, ADRs, concept poster (US-67)` | `docs/architecture.md`, ADR-001, ADR-004, ADR-007, ADR-009 (carried over from `PROJECT_PLAN.md`) plus **ADR-010** ("Java/Spring Boot for the main API, Python retained only for Flower") — the next unused number, since ADR-001–009 are already assigned to specific decisions in `PROJECT_PLAN.md` and must not be reused; poster | — | Poster reviewed by team |

**Sprint 1 DoD:** `docker compose up` works on every machine; Flyway migrations reversible; isolation suite passing; coverage ≥70%; State pattern transition table matches §17.5 exactly.

---

## SPRINT 2 — Resumes and interview capture

**Goal:** Users manage resume versions, attach them to applications, and capture structured interview experience, including the questions asked and a fast debrief.

| # | Department | Owner | Depends on | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|---|
| P16 | Backend | Backend team | P11 | `feat(applications): Specification-based search/filter (US-16,17)` | `ApplicationSpecifications` (status/priority/tag/date, combinable via `.and()`) | **Specification** | MockMvc tests for combined filters |
| P17 | Backend | Backend team | P09 | `feat(db): resumes + resume_sections migration (US-20)` | Flyway scripts | — | Migrates cleanly |
| P18 | Backend | Backend team | P17 | `feat(resumes): FileStorageStrategy + LocalFileStorage (US-21)` | `FileStorageStrategy` interface, `LocalFileStorageImpl` | **Strategy** | Unit test round-trips a file |
| P19 | Backend | Backend team | P18 | `feat(resumes): ValidatingFileStorage decorator + validation chain (US-21)` | `ValidatingFileStorage` wraps base storage; extension → MIME → magic-byte checks as ordered chain | **Decorator + Chain of Responsibility** | **Test: renamed executable rejected** |
| P20 | Backend | Backend team | P17, P19 | `feat(resumes): CRUD + upload/download endpoints (US-20..22)` | `ResumeController/Service`, expiring authenticated download | — | Cross-user file access returns 404 |
| P21 | Backend | Backend team | P20 | `feat(resumes): application association + clone (US-24,25)` | Association endpoint, clone with lineage | — | API tests |
| P22 | Frontend | Frontend team | P20, P21 | `feat(frontend): resumes list, detail, upload, clone (US-20..25)` | Resume pages, `UploadDropzone` | — | RTL + manual upload test |
| P23 | Backend | Backend team | P09 | `feat(db): interviews, interview_questions, interview_feedback migration (US-27)` | Flyway scripts | — | Migrates cleanly |
| P24 | Backend | Backend team | P23 | `feat(interviews): CRUD + questions + feedback endpoints (US-27..31)` | `InterviewController/Service`, round-number uniqueness | — | Isolation test |
| P25 | Frontend | Frontend team | P24 | `feat(frontend): interview list, detail, question capture, debrief flow (US-27..30)` | Interview pages, guided debrief UI | — | **Debrief timed under 3 min**; RTL tests |
| P26 | Backend | Backend team | P11, P20, P24 | `chore: deterministic seed data (US-72)` | Flyway `R__seed_dev_data.sql` or a `CommandLineRunner` seeder | — | Seed loads identically twice; marked synthetic |

**Sprint 2 DoD:** Upload rejects a renamed executable; cross-user file access returns 404; the debrief is timed under 3 minutes; seed data loads identically twice.

---

## SPRINT 3 — Preparation and analytics

**Goal:** Recorded history becomes measured preparation progress and a legible campaign picture, with a dashboard that recommends action instead of only displaying numbers.

| # | Department | Owner | Depends on | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|---|
| P27 | Backend | Backend team | P23 | `feat(db): topics, prerequisites, questions, practice_attempts migration + seed (US-38,35)` | Flyway + seed (≥150 questions, ~40-60 topics) | — | **Cycle-detection test on prerequisite DAG** |
| P28 | Backend | Backend team | P27 | `feat(prep): question bank + practice attempt endpoints (US-35..37)` | `/questions/*`, `/questions/{id}/attempts` | — | API tests |
| P29 | Backend | Backend team | P27, P28 | `feat(prep): real question bank content for all topics (US-90)` | Replaces the templated placeholder prompts with real, original interview problems for all 48 topics (DSA, system design, OS and networking, databases, OOP, behavioural), present from first run through a repeatable migration; existing practice history is preserved | — | Every topic has real questions; no templated prompt text remains; practice attempts referencing old rows still resolve |
| P30 | Backend | Backend team | P11 | `feat(analytics): AnalyticsMetric Template Method base class (US-58)` | Abstract base defining compute→sample-size-gate→format skeleton; concrete subclasses implement only the formula step | **Template Method** | Unit test: a new metric subclass cannot bypass the sample-size gate |
| P31 | Backend | Backend team | P28 | `feat(prep): mastery scoring engine with time decay (US-39)` | `MasteryEngine` per §21.4 formula | — | **Golden-file tests against fixed fixtures** |
| P32 | Backend | Backend team | P31 | `feat(prep): preparation tasks + progress endpoints/frontend (US-40,41)` | Tasks CRUD, `TopicGrid`, progress bars | — | RTL + API tests |
| P33 | AI/ML | AI/ML team | P02 | `spike: 1-day Flower two-client FedAvg spike (risk reduction)` | Throwaway code in `fl-service/spike/`, not on the production path | — | Findings written to `docs/adr/ADR-011-fl-spike-findings.md` |
| P34 | Backend | Backend team | P30 | `feat(analytics): funnel, conversions, timeline via Template Method (US-43..45)` | Concrete `AnalyticsMetric` subclasses; formulas exactly per §20.7 | uses **Template Method** | Golden-file tests |
| P35 | Backend | Backend team | P34 | `feat(analytics): rejection distribution, dwell times, sample-size gating (US-46,48,50)` | Remaining metric subclasses | uses **Template Method** | Golden-file tests |
| P36 | Frontend | Frontend team | P34, P35 | `feat(frontend): analytics page with Recharts (US-43..50)` | `FunnelChart`, `ConversionCards`, `SampleSizeBadge` | — | RTL tests; chart empty/loading states |
| P37 | Backend | Backend team | P11, P30 | `feat(applications): stall detection (US-18)` | Days-in-stage + personal-median flag, reuses `AnalyticsMetric` base for the median calc | uses **Template Method** | Unit test on threshold logic |
| P38 | Frontend | Frontend team | P36, P37 | `feat(frontend): dashboard pipeline strip + action queue (US-49)` | Replaces Sprint-1 basic dashboard | — | RTL tests; capped at 7 items |

**Sprint 3 DoD:** Every analytics metric is golden-file tested; the topic graph is acyclic; every topic has real questions; the Flower spike is written up.

---

## SPRINT 4 — Recommendations and federated learning

**Goal:** Explainable, evidence-tiered recommendations, and a federated learning loop with a user-visible Privacy Ledger. This is the highest-risk sprint, so its commits are the smallest.

| # | Department | Owner | Depends on | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|---|
| P39 | Backend | Backend team | P27, P32 | `feat(db): recommendations, recommendation_evidence migration (US-51)` | Flyway scripts | — | Migrates cleanly |
| P40 | Backend | Backend team | P31, P37, P39 | `feat(recommendations): candidate generator chain (US-51,56)` | `RecommendationGenerator` interface + ordered chain (weak-topic → prerequisite-gap → decay → coverage → stalled-application → …), one commit per generator is fine as sub-commits | **Chain of Responsibility** | Unit test per generator |
| P41 | Backend | Backend team | P40 | `feat(recommendations): prerequisite root-cause routing (US-52)` | `routeToRootCause` per §21.5 | — | Unit test incl. depth cap |
| P42 | Backend | Backend team | P41 | `feat(recommendations): evidence tiers, scoring, template registry (US-53,54)` | Tiering (**Strategy** — one scoring strategy per tier A/B/C/D), template registry | **Strategy** | **`test_no_causal_language` linter — must fail build on banned phrase** |
| P43 | Backend | Backend team | P32, P42 | `feat(recommendations): RecommendationFacade + accept/snooze/dismiss + frontend (US-55,77)` | Facade hides the multi-step pipeline from the controller | **Facade + Builder** (recommendation card construction) | RTL + API tests |
| P44 | AI/ML | AI/ML team | P33 | `feat(fl): feature schema v1 — single source of truth (US-58)` | `fl-service/app/common/schema.py` — the 24-feature list, §22.2 | — | Schema test: exactly 24 named floats |
| P45 | AI/ML | AI/ML team | P44 | `feat(fl): local feature extractor, point-in-time correct (US-58)` | `fl-service/app/client/feature_extractor.py` | — | **Test: no post-`as_of` record leaks into the vector** |
| P46 | AI/ML | AI/ML team | P45 | `feat(fl): local sklearn trainer (US-59)` | `fl-service/app/client/local_trainer.py` | — | Deterministic under fixed seed |
| P47 | AI/ML | AI/ML team | P46 | `feat(fl): clipping and Gaussian noise (US-60)` | `fl-service/app/client/privacy.py` | — | Test: transmitted vector ≠ raw update; norm ≤ S |
| P48 | AI/ML | AI/ML team | P47 | `feat(fl): Flower server, FedAvg strategy, min-cohort refusal (US-61)` | `fl-service/app/server/` | — | **Test: 2-client round refused and logged** |
| P49 | AI/ML | AI/ML team | P48 | `feat(fl): norm-based poisoning filter (US-84)` | Filter in Flower strategy | — | Test with a crafted over-norm update |
| P50 | AI/ML | AI/ML team | P48 | `feat(fl): model versioning + distribution endpoint (US-62)` | `/fl/model` in the thin FastAPI control wrapper | — | Failed round leaves previous model servable |
| P51 | AI/ML | AI/ML team | P02, P50 | `chore: docker-compose fl profile with isolated client schemas/roles (US-58..64)` | Separate compose file; per-client Postgres role, `USAGE` on one schema only | — | **CI job asserts fl-service container has zero DB credentials for user-content schemas** |
| P52 | Backend | Backend team | P31, P44, P51 | `feat(api): sanitised feature export job, Java -> fl-service (US-58)` | Scheduled/triggered job exporting only the allow-listed 24-feature inputs per opted-in user into the FL service's own store | — | Test: export payload matches allow-list exactly, no free text |
| P53 | Backend | Backend team | P50, P52 | `feat(api): FederatedLearningGateway + FlowerServiceAdapter (US-61,62)` | `FederatedLearningGateway` interface, `FlowerServiceAdapter` implementing it against the fl-service REST contract | **Adapter** | Contract test against a stubbed fl-service |
| P54 | Backend | Backend team | P53 | `feat(api): Resilience4j circuit breaker + local-only fallback (new — architecture revision)` | Wraps `FlowerServiceAdapter`; on failure, recommendations silently degrade to local-only ranking | **Circuit Breaker** | **Fault-injection test: stop fl-service mid-request, assert 200 with local-only results, not 500** |
| P55 | Backend | Backend team | P06, P44 | `feat(fl): opt-in/opt-out endpoints + consent screen (US-57,64)` | `/fl/opt-in`, `/fl/opt-out`, `/fl/schema` | — | Test: opted-out client produces no update; default off |
| P56 | Backend | Backend team | P51, P53 | `feat(fl): Privacy Ledger backend — round metadata + per-user entries (US-63)` | `privacy_ledger_entries` migration + write path | — | Real transmitted vector recorded per client per round |
| P57 | Frontend | Frontend team | P56 | `feat(frontend): Privacy Ledger UI with vector inspector (US-63)` | `PrivacyLedgerTable`, `VectorInspector` | — | RTL tests |
| P58 | Frontend | Frontend team | P43, P54 | `feat(recommendations): Federated Lift viewer (US-65,77)` | Toggle UI showing rank delta; reuses the same local-only path the circuit breaker falls back to | — | Test: adjustment bounded to [0.75,1.25] |

**Sprint 4 DoD:** The payload schema test passes; the 2-client refusal test passes; fl-service credential absence is verified in CI; the phrasing linter passes; the circuit-breaker fault-injection test passes; a Privacy Ledger entry shows a real vector end to end.

---

## SPRINT 5 — Notifications, reports and account

**Goal:** In-app notifications driven by domain events, PDF and CSV report export, and complete account self-service: password change, password reset, email verification, data export and account deletion.

| # | Department | Owner | Depends on | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|---|
| P59 | Backend | Backend team | P09 | `feat(db): notifications migration (FR-99..101)` | Flyway script per `ARCHITECTURE_REVISION_JAVA.md` §5 | — | Migrates cleanly |
| P60 | Backend | Backend team | P24, P37 | `feat(events): domain events for stall/interview/debrief detection (FR-99)` | `StalledApplicationEvent`, `UpcomingInterviewEvent`, `MissingDebriefEvent` published via Spring's `ApplicationEventPublisher` | **Observer** | Unit test: event fires on the right condition, detector doesn't know who listens |
| P61 | Backend | Backend team | P59, P60 | `feat(notifications): NotificationChannel strategy + in-app channel (FR-100, NFR-53)` | `NotificationChannel` interface, `InAppNotificationChannel` impl, `NotificationListener` subscribing to the events from P60 | **Strategy** | Unit test: swapping channel doesn't touch the event detector |
| P62 | Backend | Backend team | P61 | `feat(notifications): endpoints + bell UI, mark-as-read (US-76..79)` | `/notifications/*`, notification bell component | — | RTL + API tests |
| P63 | Backend | Backend team | P09 | `feat(db): report_exports migration (FR-102..104)` | Flyway script | — | Migrates cleanly |
| P64 | Backend | Backend team | P34, P35, P63 | `feat(reports): ReportBuilder for PDF/CSV assembly (US-80)` | `ReportBuilder` — fluent `.withDateRange().withApplications().withAnalytics().build()`, reusing existing analytics formulas | **Builder** | Unit test: builder output matches a fixture |
| P65 | Backend | Backend team | P20, P64 | `feat(reports): export endpoints + frontend, secured like resume downloads (FR-104)` | Authenticated, expiring, owner-only download — mirrors FR-32 exactly | — | Cross-user download returns 404 |
| P66 | Backend | Backend team | P05, P06 | `feat(auth): change password, data export, account deletion (US-06,07,08)` | Endpoints + frontend | — | Deletion orphan-check test across every table |
| P67 | Backend | Backend team | P05 | `fix(auth): Argon2id parameters and refresh-token lifetime per NFR-01/02` | Argon2id ≥64 MiB / ≥3 iterations with transparent rehash on next login; refresh tokens ≤7 days | — | Test asserts hash parameters and TTLs; existing users still log in |
| P68 | Backend | Backend team | P05, P67 | `feat(auth): password reset + email verification (US-92)` | Expiring single-use tokens; mail sender behind an interface (log/Mailpit in dev) | **Strategy** (mail sender) | Token reuse and expiry rejected; no email enumeration |

**Sprint 5 DoD:** Notifications and reports work end to end; account deletion leaves no orphan rows; Argon2id parameters and token lifetimes are asserted by tests; reset and verification tokens are single-use and expiring; no email enumeration.

---

## SPRINT 6 — Experience and core gaps

**Goal:** One consistent, responsive interface in light and dark themes, with the everyday interactions a job seeker expects: a board view, filters, practice logging from the UI, onboarding, social sign-in, and security headers on every response.

| # | Department | Owner | Depends on | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|---|
| P69 | Frontend | Frontend team | P07 | `feat(frontend): design system — tokens, typography, light/dark themes (US-85)` | Colour/type/spacing tokens, "calm & precise" visual language (neutral greys, one indigo accent, Geist), system-default theme + toggle | — | Both themes pass contrast checks; RTL theme-toggle test |
| P70 | Frontend | Frontend team | P08, P69 | `feat(frontend): app shell — user menu, logout, breadcrumbs, command palette (US-86)` | Sidebar user menu with logout, page titles + detail breadcrumbs, ⌘K palette | — | RTL: logout clears session; palette navigates |
| P71 | Frontend | Frontend team | P69 | `feat(frontend): feedback layer — toasts, label formatting, form validation (US-87)` | Shared toast, one formatter for every enum value, client-side validation with friendly messages, consistent empty/loading/error states | — | RTL: no raw enum strings rendered; invalid form blocked before submit |
| P72 | Frontend | Frontend team | P36, P57, P69 | `feat(frontend): responsive tables, charts and privacy views (US-88)` | Card rows on mobile, responsive Recharts with legends, conversion-card fixes, mobile vector inspector, plain-language privacy page | — | Playwright screenshots at 390px and 1440px; conversion numbers match the funnel |
| P73 | Frontend | Frontend team | P10, P12, P71 | `feat(applications): Kanban board with State-guarded moves (US-89)` | Drag between stages; server's State pattern decides, illegal moves snap back with the reason; note prompt for backward moves | uses **State** | RTL + API: illegal drag refused, legal drag persisted with history |
| P74 | Frontend | Frontend team | P16, P71 | `feat(applications): filters, search, full form fields, status UX fixes (US-16,17)` | Filter bar over the P16 Specification API, all application fields in the form, only legal next statuses offered | — | RTL + MockMvc |
| P75 | Frontend | Frontend team | P28, P31, P71 | `feat(prep): question bank browser + practice logging (US-90)` | Browse/filter questions, log attempts, topic drill-down; mastery updates visibly | — | RTL + API: logging an attempt changes mastery |
| P76 | Frontend | Frontend team | P06, P32, P71 | `feat(users): profile page + onboarding starter plan (US-91)` | Profile editing, first-run onboarding (target role/date) seeding tasks and goals | — | RTL; new user lands on onboarding, not an empty dashboard |
| P77 | Backend | Backend team | P32, P43 | `fix(recommendations): accept creates a task, clear dismiss scope, snoozed view (US-55)` | Visible accepted state, error handling, dismiss warns about type-wide hiding, manage snoozed/dismissed | — | RTL + API |
| P78 | Backend | Backend team | P05, P68 | `feat(auth): Google and GitHub sign-in (US-93)` | Spring Security OAuth2 client issuing the app's own JWTs; link only on provider-verified email; ADR-012 records the decision | — | Unverified-email linking refused; existing auth tests unchanged |
| P79 | Frontend | Frontend team | P38, P69, P70, P78 | `feat(frontend): visual polish — landing and auth pages, dashboard, navigation (US-85)` | A bolder visual identity: a public landing page, split-layout sign-in and register with product preview, richer dashboard (progress rings, streak, next actions), refined sidebar and cards, subtle motion that respects reduced-motion, both themes | — | RTL: landing and auth render in both themes; no layout break at 390px and 1440px; axe has no critical violations on landing and auth |
| P80 | Frontend | Frontend team | P79 | `feat(frontend): consistent professional polish across all application pages (US-85)` | Review and refine every inner page to one visual standard: applications, resumes, interviews, prep, analytics, recommendations, reports, notifications, account; shared page header, table, form, empty-state and chart patterns; no one-off styling | — | RTL for each page in both themes; screenshots at 390px and 1440px reviewed; axe has no critical violations |
| P81 | Backend | Backend team | P05, P13, P78 | `chore(security): security headers, secret scan, SQL-interpolation gate (NFR-03,08,09)` | CSP/nosniff/Referrer-Policy/HSTS, CI secret scanning, CI check for string-built SQL | — | Header test; CI gates fail on a planted violation |

**Sprint 6 DoD:** Both themes and both breakpoints are reviewed with screenshots; logging a practice attempt moves mastery end to end; illegal board moves are refused with a reason; the security header test passes and the CI gates fail on a planted violation.

---

## SPRINT 7 — Job-search workflow

**Goal:** The networking and decision side of a job search: contacts and referrals, a page per company, offer comparison, weekly goals with an activity heatmap, one search box across everything, and a structured resume editor.

| # | Department | Owner | Depends on | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|---|
| P82 | Backend | Backend team | P09, P11 | `feat(db): contacts, company notes, offers, goals migration (US-95,96,100,101)` | Flyway scripts + rollbacks | — | Migrates cleanly |
| P83 | Backend | Backend team | P60, P82 | `feat(contacts): contacts and referrals + cold-contact nudges (US-95)` | CRUD, referral status, nudge via the P60 Observer events | uses **Observer** | Isolation test (404) |
| P84 | Backend | Backend team | P24, P82, P83 | `feat(companies): company pages (US-96)` | Aggregated view per company | — | API + RTL |
| P85 | Backend | Backend team | P82 | `feat(offers): offer comparison + deadline countdown (US-100)` | Side-by-side compare | — | API + RTL |
| P86 | Backend | Backend team | P38, P76, P82 | `feat(goals): weekly goals + activity heatmap (US-101)` | Targets, progress, GitHub-style heatmap, streak on the dashboard | — | API + RTL; isolation test |
| P87 | Backend | Backend team | P28, P70, P83, P84 | `feat(search): global search behind the command palette (US-99)` | One endpoint across applications, companies, questions, notes; results appear in the ⌘K palette | — | Results never include another user's data |
| P88 | Backend | Backend team | P17, P21 | `feat(resumes): structured resume section editor (US-23)` | Edit sections (experience, projects, skills); clones carry sections; export a clean PDF resume from the sections | — | Isolation test; clone copies edited sections |

**Sprint 7 DoD:** Every new endpoint has a cross-user isolation test (404); search never returns another user's data; a cloned resume carries its edited sections.

---

## SPRINT 8 — AI and intelligence

**Goal:** AI assistance grounded in the user's own data, prep intelligence (question recommendations, readiness, spaced review), and cohort benchmarks and a privacy budget meter built on the federated model. AI runs on a local model (Ollama) by default, so user data never leaves the server; Google Gemini is an optional provider, and the UI discloses when it is active.

| # | Department | Owner | Depends on | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|---|
| P89 | AI/ML | AI/ML team | P03 | `feat(ai): AiProvider strategy — local Ollama default, optional Gemini (US-102)` | Provider interface, Ollama + Gemini implementations, streaming, timeouts, graceful "AI unavailable", per-user rate limits, AI usage log (no prompt content stored); ADR-013 records the local-first AI decision | **Strategy** | Contract tests against stubbed providers; swapping provider touches no feature code |
| P90 | AI/ML | AI/ML team | P42, P89 | `feat(ai): Cohort Coach chat grounded in the user's data (US-103)` | Streaming chat; the app injects the user's own data into context; causal-language linter applied to replies | — | Grounding test: coach never sees another user's data |
| P91 | AI/ML | AI/ML team | P11, P89 | `feat(ai): job posting to application parser (US-104)` | Structured extraction with validation; user confirms before save | — | Malformed model output rejected, never saved |
| P92 | AI/ML | AI/ML team | P24, P28, P89 | `feat(ai): debrief notes to tagged questions (US-33,34,105)` | Extract questions, map to topics, promote to the question bank | — | Fixture-based extraction tests |
| P93 | AI/ML | AI/ML team | P31, P89 | `feat(prep): question recommendations with embeddings (US-106)` | Local embeddings; solve-next, daily picks, interview-driven; each pick states its reason | — | Deterministic ranking tests on fixtures |
| P94 | Backend | Backend team | P11, P30, P31 | `feat(prep): job readiness score per application (US-107)` | Job skills mapped to topics, weighted by mastery; gated by sample size | uses **Template Method** | Golden-file tests |
| P95 | AI/ML | AI/ML team | P20, P88, P89 | `feat(ai): resume and job description match (US-108)` | Gaps, tailoring suggestions, and a tailored cover letter draft | — | Fixture tests |
| P96 | AI/ML | AI/ML team | P28, P31, P89 | `feat(ai): mock interview with graded answers (US-109)` | Questions from weak topics; grades recorded as practice attempts | — | Attempts recorded with the right topic |
| P97 | AI/ML | AI/ML team | P89 | `feat(ai): STAR story bank (US-110)` | CRUD + AI structuring + competency tags | — | Isolation test |
| P98 | AI/ML | AI/ML team | P24, P89, P93 | `feat(ai): interview prep pack (US-111)` | One-page brief per upcoming interview | — | Content assembled only from the user's data |
| P99 | AI/ML | AI/ML team | P85, P89 | `feat(ai): follow-up and negotiation email drafts (US-112)` | Drafts only; never sent automatically | — | API + RTL |
| P100 | AI/ML | AI/ML team | P36, P61, P89 | `feat(ai): weekly review (US-113)` | Generated from analytics; delivered through the notification channels | — | Fixture tests |
| P101 | Backend | Backend team | P21, P30, P36 | `feat(analytics): resume performance and funnel simulator (US-26,114)` | Response rate per resume version; applications-needed simulator | uses **Template Method** | Golden-file tests |
| P102 | AI/ML | AI/ML team | P48, P50 | `feat(fl): cohort benchmarks from the federated model (US-115)` | Benchmarks from the global model only; min-cohort rule applies | — | Benchmarks hidden below the min-cohort size |
| P103 | Backend | Backend team | P31, P75 | `feat(prep): spaced review queue (US-42)` | Daily review queue scheduled from mastery decay | — | Deterministic scheduling tests on fixtures |
| P104 | AI/ML | AI/ML team | P56, P57 | `feat(fl): privacy budget meter (US-66)` | Cumulative ε per user from the ledger, shown against the configured budget | — | Meter total equals the sum of the user's ledger entries |

**Sprint 8 DoD:** Every AI feature works with the local model and degrades gracefully when it is down; no AI prompt contains another user's data; the Gemini disclosure is shown whenever that provider is active.

---

## SPRINT 9 — Hardening and delivery

**Goal:** Demo-ready data, a first-run experience, designed error states, privacy and AI disclosure pages, and the final security and accessibility verification across every feature.

| # | Department | Owner | Depends on | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|---|
| P105 | Backend | Backend team | P26, P83, P85, P86, P97 | `chore: seed data for contacts, offers, goals, stories and AI history (US-72)` | Demo data for contacts, offers, goals, stories, AI history | — | Seed loads identically twice |
| P106 | Frontend | Frontend team | P76, P105 | `feat(onboarding): sample data loader + guided product tour (US-116)` | One-click realistic sample data that is clearly marked and removable; first-run guided tour of the main areas, skippable and replayable from help | — | Sample data only touches the current user's records; removal leaves no orphans; tour skippable |
| P107 | Frontend | Frontend team | P70, P71 | `feat(frontend): designed error, 404, offline and session-expired screens (US-117)` | Consistent full-page and inline states with clear next actions and retry; offline banner; graceful token-expiry flow | — | RTL for each state in both themes |
| P108 | Frontend | Frontend team | P55, P89 | `feat(frontend): privacy policy, AI disclosure, help pages (NFR-16)` | Plain-language data, FL and AI disclosure; keyboard shortcut help | — | Linked from sign-up and settings |
| P109 | Backend | Backend team | P14, P108 | `test: full security suite (US-73)` | Enumerate every endpoint from the OpenAPI schema (springdoc), including all Sprint 6–8 endpoints; assert 401/404 systematically | — | Full suite green |
| P110 | Frontend | Frontend team | P80, P108 | `chore(a11y): accessibility remediation pass (US-74)` | Contrast, focus traps, aria-labels across the whole UI in both themes | — | axe-core: zero critical violations |

**Sprint 9 DoD:** The full security suite is green across every endpoint; axe-core reports zero critical violations in both themes; sample data and the tour work for a new user; the demo has been rehearsed twice end to end.

---

## Scope

The feature set is: tracking (list, board, companies, contacts, offers, global search, weekly goals and heatmap), resumes (files, structured editor, PDF export, versions, performance), interviews (debriefs, prep packs, mock interviews, story bank), preparation (question bank, practice, mastery, spaced review, recommendations, readiness), analytics, in-app notifications, reports, AI assistance, federated learning with a Privacy Ledger and budget meter, and sign-in with email or Google/GitHub.

**Out of scope.** The product does not include: a calendar view or .ics export, CSV import, tag management and bulk status update, recruiter feedback capture, an email notification channel, an admin console, structured operations logging, CI quality gates beyond the coverage gate (lint, type and import-boundary gates), load and cross-browser testing, a production deployment profile with backups, a browser extension, job-board scraping or auto-apply, two-way calendar or email sync, MFA, sharing between users, and native mobile apps (the web app is fully responsive instead). `PROJECT_PLAN.md` §31.2 holds the full list.

**Project DoD:** every sprint DoD above is met, and every NFR in `PROJECT_PLAN.md` §7 is either verified by the phase listed below or marked "documented, not measured".

## Requirement traceability

Every user story in `PROJECT_PLAN.md` §10.1 lists the phases that deliver it. US-69 (schema and migrations) is delivered by every `feat(db)` phase rather than one commit.

| NFRs | Delivered or verified by |
|---|---|
| NFR-01, 02 | P05, P67 |
| NFR-03, 08, 09 | P81 |
| NFR-04, 05 | P109 |
| NFR-06 | P19, P20 |
| NFR-07 | P05 |
| NFR-10–13 | P44–P52, P55 |
| NFR-14 | P66 |
| NFR-15, 16 | P42, P90, P108 |
| NFR-17–22 | Documented, not measured |
| NFR-23, 24 | Documented in `docs/architecture.md` (P15), not measured |
| NFR-25, 29 | Documented, not measured |
| NFR-26, 27 | Rollback script in every `feat(db)` phase; transactional tests in P109 |
| NFR-28 | P50 |
| NFR-30, 31 | P25, P76 |
| NFR-32–35 | P71, P72, P107 |
| NFR-36, 38, 39 | Documented, not measured |
| NFR-37 | P13 |
| NFR-40 | ADRs in P15, P33, P78, P89 |
| NFR-41, 42 | P02 |
| NFR-43 | Documented, not measured |
| NFR-44–48 | P69, P72, P110 |
| NFR-49–51 | Throughout (injected clock, storage and FL gateway; seeded randomness; golden files) |
| NFR-52 | Documented, not measured |
| NFR-53 | P61 |

---

## Quick reference: the commits worth walking an evaluator through personally

- **P10** — the State pattern replacing if/else transition logic
- **P19** — Decorator + Chain of Responsibility on file upload, and the malicious-upload test
- **P30** — Template Method structurally enforcing the sample-size rule
- **P31** — mastery decay golden-file tests
- **P40/P42** — the recommendation Chain of Responsibility, and the causal-language linter that fails the build
- **P44–P54** — the entire FL chain, especially **P48** (min-cohort refusal), **P51** (CI proves fl-service has no DB credentials), and **P54** (the circuit breaker fault-injection test, the clearest evidence of designing for failure)
- **P60/P61** — Observer + Strategy cooperating cleanly on notifications
- **P73** — the Kanban board, where the P10 State pattern visibly refuses illegal moves
- **P89/P90** — the AI provider Strategy and a coach grounded strictly in the user's own data, on a local model so privacy holds
- **P102** — cohort benchmarks: the federated model turned into something users can see

Everything else is solid, necessary engineering; these are the commits that show design judgment, not just working code.
