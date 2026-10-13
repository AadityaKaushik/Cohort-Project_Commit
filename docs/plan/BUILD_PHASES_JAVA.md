# Cohort — Build Phases (Java Spring Boot Edition)

**Purpose:** Use this file together with `PROJECT_PLAN.md` and `ARCHITECTURE_REVISION_JAVA.md`. It replaces the earlier Python-only build-phases file — same idea, same one-commit-per-phase discipline, rebuilt for the Java/Python split: **Spring Boot owns the whole application; Python is scoped down to just the Flower client/server.** 69 phases across 5 sprints, including the two additions (notifications, report export) and the design-pattern work from the architecture revision. Extension Sprints 6–9, added after Sprint 4, bring the total to 105 phases after a scope review that cut low-priority phases (see "Extension sprints").

## How to use this plan

1. Keep `PROJECT_OVERVIEW.md` (the plain-language map of the whole project), `PROJECT_PLAN.md` (requirements), `ARCHITECTURE_REVISION_JAVA.md` (design-pattern rationale), and this file together in the repo, e.g. `docs/plan/`.
2. Work order: read `PROJECT_OVERVIEW.md` first for the big picture, then `PROJECT_PLAN.md` for requirements and `ARCHITECTURE_REVISION_JAVA.md` for the design-pattern rationale. Execute `BUILD_PHASES_JAVA.md` one phase at a time. After finishing a phase, run the verification checks listed, then commit with the exact message given, then stop and wait for review before starting the next phase.
3. Review every commit's diff before continuing — this is also how you actually learn the Spring Boot codebase well enough to defend it in a viva.
4. Never merge two phases into one commit, even if a phase looks small.

## Global rules

- Conventional commit format: `type(scope): description (US-XX)` — types: `feat`, `fix`, `test`, `docs`, `chore`, `refactor`.
- **Every commit that adds or changes a pattern-bearing class (see the "pattern" column below) must say which pattern it implements/extends, in one sentence, in the PR description** — this is a Definition-of-Done requirement from `ARCHITECTURE_REVISION_JAVA.md` §10, not optional polish.
- Every phase adding backend logic ships with tests in the **same commit** — JUnit 5 for units/slices, MockMvc for controller tests, Testcontainers for anything touching real Postgres.
- Every phase touching user-owned data includes the cross-user isolation test for any new endpoint (404 on foreign access).
- Run `mvn test` (Java side) or `pytest` (fl-service side) before every commit — never commit a red suite.
- No phase jumps ahead of its listed dependency.

## Mapping to your 10-person team

Same ownership split as before — P1–P10 map to the same people/epics from the original planning conversation. Ideally different phases are run by different teammates from their own machines/GitHub accounts, on branches named `feature/US-XX-description`, reviewed by the person listed under **Reviewer**. If P8/P9 (the FL owners) are less comfortable in Java, that's fine — their phases in this plan are almost entirely inside `fl-service/`, which stays Python.

---

## SPRINT 1 — Foundation (Java)

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

## SPRINT 2 — Resumes & Interview Capture

| # | Owner | Reviewer | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|
| P16 | P2 | P3 | `feat(applications): Specification-based search/filter (US-16,17)` | `ApplicationSpecifications` (status/priority/tag/date, combinable via `.and()`) | **Specification** | MockMvc tests for combined filters |
| P17 | P3 | P4 | `feat(db): resumes + resume_sections migration (US-20)` | Flyway scripts | — | Migrates cleanly |
| P18 | P3 | P4 | `feat(resumes): FileStorageStrategy + LocalFileStorage (US-21)` | `FileStorageStrategy` interface, `LocalFileStorageImpl` | **Strategy** | Unit test round-trips a file |
| P19 | P3 | P5 | `feat(resumes): ValidatingFileStorage decorator + validation chain (US-21)` | `ValidatingFileStorage` wraps base storage; extension → MIME → magic-byte checks as ordered chain | **Decorator + Chain of Responsibility** | **Test: renamed executable rejected** |
| P20 | P3 | P5 | `feat(resumes): CRUD + upload/download endpoints (US-20..22)` | `ResumeController/Service`, expiring authenticated download | — | Cross-user file access returns 404 |
| P21 | P3 | P6 | `feat(resumes): application association + clone (US-24,25)` | Association endpoint, clone with lineage | — | API tests |
| P22 | P3 | P7 | `feat(frontend): resumes list, detail, upload, clone (US-20..25)` | Resume pages, `UploadDropzone` | — | RTL + manual upload test |
| P23 | P4 | P8 | `feat(db): interviews, interview_questions, interview_feedback migration (US-27)` | Flyway scripts | — | Migrates cleanly |
| P24 | P4 | P8 | `feat(interviews): CRUD + questions + feedback endpoints (US-27..31)` | `InterviewController/Service`, round-number uniqueness | — | Isolation test |
| P25 | P4 | P9 | `feat(frontend): interview list, detail, question capture, debrief flow (US-27..30)` | Interview pages, guided debrief UI | — | **Debrief timed under 3 min**; RTL tests |
| P26 | P10 | P1 | `chore: deterministic seed data (US-72)` | Flyway `R__seed_dev_data.sql` or a `CommandLineRunner` seeder | — | Seed loads identically twice; marked synthetic |

**Sprint 2 DoD:** upload security verified; debrief timed; seed data working.

---

## SPRINT 3 — Preparation & Analytics

| # | Owner | Reviewer | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|
| P27 | P5 | P6 | `feat(db): topics, prerequisites, questions, practice_attempts migration + seed (US-38,35)` | Flyway + seed (≥150 questions, ~40-60 topics) | — | **Cycle-detection test on prerequisite DAG** |
| P28 | P5 | P6 | `feat(prep): question bank + practice attempt endpoints (US-35..37)` | `/questions/*`, `/questions/{id}/attempts` | — | API tests |
| P29 | P6 | P7 | `feat(analytics): AnalyticsMetric Template Method base class (US-58)` | Abstract base defining compute→sample-size-gate→format skeleton; concrete subclasses implement only the formula step | **Template Method** | Unit test: a new metric subclass cannot bypass the sample-size gate |
| P30 | P5 | P7 | `feat(prep): mastery scoring engine with time decay (US-39)` | `MasteryEngine` per §21.4 formula | — | **Golden-file tests against fixed fixtures** |
| P31 | P5 | P8 | `feat(prep): preparation tasks + progress endpoints/frontend (US-40,41)` | Tasks CRUD, `TopicGrid`, progress bars | — | RTL + API tests |
| P32 | P9 | — | `spike: 1-day Flower two-client FedAvg spike (risk reduction)` | Throwaway code in `fl-service/spike/`, not on the production path | — | Findings written to `docs/adr/ADR-011-fl-spike-findings.md` |
| P33 | P6 | P9 | `feat(analytics): funnel, conversions, timeline via Template Method (US-43..45)` | Concrete `AnalyticsMetric` subclasses; formulas exactly per §20.7 | uses **Template Method** | Golden-file tests |
| P34 | P6 | P9 | `feat(analytics): rejection distribution, dwell times, sample-size gating (US-46,48,50)` | Remaining metric subclasses | uses **Template Method** | Golden-file tests |
| P35 | P6 | P10 | `feat(frontend): analytics page with Recharts (US-43..50)` | `FunnelChart`, `ConversionCards`, `SampleSizeBadge` | — | RTL tests; chart empty/loading states |
| P36 | P2 | P10 | `feat(applications): stall detection (US-18)` | Days-in-stage + personal-median flag, reuses `AnalyticsMetric` base for the median calc | uses **Template Method** | Unit test on threshold logic |
| P37 | P2/P6 | P10 | `feat(frontend): dashboard pipeline strip + action queue (US-49)` | Replaces Sprint-1 basic dashboard | — | RTL tests; capped at 7 items |

**Sprint 3 DoD:** every analytics metric golden-file tested; topic graph acyclic; Flower spike written up.

---

## SPRINT 4 — Recommendations & Federated Learning (highest-risk sprint — smallest commits here)

| # | Owner | Reviewer | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|
| P38 | P7 | P5 | `feat(db): recommendations, recommendation_evidence migration (US-51)` | Flyway scripts | — | Migrates cleanly |
| P39 | P7 | P5 | `feat(recommendations): candidate generator chain (US-51,56)` | `RecommendationGenerator` interface + ordered chain (weak-topic → prerequisite-gap → decay → coverage → stalled-application → …), one commit per generator is fine as sub-commits | **Chain of Responsibility** | Unit test per generator |
| P40 | P7 | P6 | `feat(recommendations): prerequisite root-cause routing (US-52)` | `routeToRootCause` per §21.5 | — | Unit test incl. depth cap |
| P41 | P7 | P6 | `feat(recommendations): evidence tiers, scoring, template registry (US-53,54)` | Tiering (**Strategy** — one scoring strategy per tier A/B/C/D), template registry | **Strategy** | **`test_no_causal_language` linter — must fail build on banned phrase** |
| P42 | P7 | P8 | `feat(recommendations): RecommendationFacade + accept/snooze/dismiss + frontend (US-55,77)` | Facade hides the multi-step pipeline from the controller | **Facade + Builder** (recommendation card construction) | RTL + API tests |
| P43 | P8 | P9 | `feat(fl): feature schema v1 — single source of truth (US-58)` | `fl-service/app/common/schema.py` — the 24-feature list, §22.2 | — | Schema test: exactly 24 named floats |
| P44 | P8 | P9 | `feat(fl): local feature extractor, point-in-time correct (US-58)` | `fl-service/app/client/feature_extractor.py` | — | **Test: no post-`as_of` record leaks into the vector** |
| P45 | P8 | P9 | `feat(fl): local sklearn trainer (US-59)` | `fl-service/app/client/local_trainer.py` | — | Deterministic under fixed seed |
| P46 | P8 | P9 | `feat(fl): clipping and Gaussian noise (US-60)` | `fl-service/app/client/privacy.py` | — | Test: transmitted vector ≠ raw update; norm ≤ S |
| P47 | P9 | P8 | `feat(fl): Flower server, FedAvg strategy, min-cohort refusal (US-61)` | `fl-service/app/server/` | — | **Test: 2-client round refused and logged** |
| P48 | P9 | P8 | `feat(fl): norm-based poisoning filter (US-84)` | Filter in Flower strategy | — | Test with a crafted over-norm update |
| P49 | P9 | P8 | `feat(fl): model versioning + distribution endpoint (US-62)` | `/fl/model` in the thin FastAPI control wrapper | — | Failed round leaves previous model servable |
| P50 | P10 | P9 | `chore: docker-compose fl profile with isolated client schemas/roles (US-58..64)` | Separate compose file; per-client Postgres role, `USAGE` on one schema only | — | **CI job asserts fl-service container has zero DB credentials for user-content schemas** |
| P51 | P8 | P1 | `feat(api): sanitised feature export job, Java → fl-service (US-58)` | Scheduled/triggered job exporting only the allow-listed 24-feature inputs per opted-in user into the FL service's own store | — | Test: export payload matches allow-list exactly, no free text |
| P52 | P9 | P10 | `feat(api): FederatedLearningGateway + FlowerServiceAdapter (US-61,62)` | `FederatedLearningGateway` interface, `FlowerServiceAdapter` implementing it against the fl-service REST contract | **Adapter** | Contract test against a stubbed fl-service |
| P53 | P9 | P10 | `feat(api): Resilience4j circuit breaker + local-only fallback (new — architecture revision)` | Wraps `FlowerServiceAdapter`; on failure, recommendations silently degrade to local-only ranking | **Circuit Breaker** | **Fault-injection test: stop fl-service mid-request, assert 200 with local-only results, not 500** |
| P54 | P1 | P9 | `feat(fl): opt-in/opt-out endpoints + consent screen (US-57,64)` | `/fl/opt-in`, `/fl/opt-out`, `/fl/schema` | — | Test: opted-out client produces no update; default off |
| P55 | P9 | P7 | `feat(fl): Privacy Ledger backend — round metadata + per-user entries (US-63)` | `privacy_ledger_entries` migration + write path | — | Real transmitted vector recorded per client per round |
| P56 | P9 | P7 | `feat(frontend): Privacy Ledger UI with vector inspector (US-63)` | `PrivacyLedgerTable`, `VectorInspector` | — | RTL tests |
| P57 | P7/P9 | — | `feat(recommendations): Federated Lift viewer (US-65,77)` | Toggle UI showing rank delta; reuses the same local-only path the circuit breaker falls back to | — | Test: adjustment bounded to [0.75,1.25] |

**Sprint 4 DoD:** payload schema test passes; 2-client refusal test passes; fl-service credential-absence verified in CI; phrasing linter passes; circuit-breaker fault-injection test passes; a Privacy Ledger entry shows a real vector end to end.

---

## SPRINT 5 — Notifications, Reports, Hardening & Delivery

| # | Owner | Reviewer | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|
| P58 | P1 | P2 | `feat(db): notifications migration (FR-99..101)` | Flyway script per Architecture Revision §5 | — | Migrates cleanly |
| P59 | P2 | P1 | `feat(events): domain events for stall/interview/debrief detection (FR-99)` | `StalledApplicationEvent`, `UpcomingInterviewEvent`, `MissingDebriefEvent` published via Spring's `ApplicationEventPublisher` | **Observer** | Unit test: event fires on the right condition, detector doesn't know who listens |
| P60 | P1 | P2 | `feat(notifications): NotificationChannel strategy + in-app channel (FR-100, NFR-53)` | `NotificationChannel` interface, `InAppNotificationChannel` impl, `NotificationListener` subscribing to the events from P59 | **Strategy** | Unit test: swapping channel doesn't touch the event detector |
| P61 | P1 | P3 | `feat(notifications): endpoints + bell UI, mark-as-read (US-76..79)` | `/notifications/*`, notification bell component | — | RTL + API tests |
| P62 | P6 | P7 | `feat(db): report_exports migration (FR-102..104)` | Flyway script | — | Migrates cleanly |
| P63 | P6 | P7 | `feat(reports): ReportBuilder for PDF/CSV assembly (US-80)` | `ReportBuilder` — fluent `.withDateRange().withApplications().withAnalytics().build()`, reusing existing analytics formulas | **Builder** | Unit test: builder output matches a fixture |
| P64 | P6 | P8 | `feat(reports): export endpoints + frontend, secured like resume downloads (FR-104)` | Authenticated, expiring, owner-only download — mirrors FR-32 exactly | — | Cross-user download returns 404 |
| P65 | P1 | P10 | `feat(auth): change password, data export, account deletion (US-06,07,08)` | Endpoints + frontend | — | Deletion orphan-check test across every table |
| P66–P67 | | | *Moved to Sprint 9, see note below (P68–P69 cut)* | | | |

**Sprint 5 / project DoD:** all Must stories done; notifications and reports working end to end; coverage gates met; zero critical a11y violations; deletion leaves no orphans; fault-injection test still green; demo rehearsed twice.

> **Rescheduled (added after Sprint 4):** P66–P69 (full security suite, accessibility pass) keep their IDs and commit messages but now run in **Sprint 9**, after the extension sprints below, so they cover every feature instead of only the original scope. P68 and P69 were cut in the scope review.

---

## Extension sprints (added after Sprint 4)

A UI audit and a product review after Sprint 4 found gaps a real job seeker would hit: no way to log practice from the UI, no logout, no board view, no networking/referral tracking, no job-specific readiness, no AI assistance. Sprints 6–8 close those gaps; Sprint 9 is the original hardening work, moved to the end. Same rules as above: one commit per phase, tests in the same commit, cross-user isolation (404) on every new user-owned endpoint.

### Extension user stories

| ID | Story |
|---|---|
| US-85 | As a user, I get a consistent, professional interface in light or dark mode on any screen size |
| US-86 | As a user, I can see who I'm signed in as, log out, and jump anywhere with a command palette |
| US-87 | As a user, every action gives clear feedback, and forms tell me what's wrong before I submit |
| US-88 | As a user, tables, charts and the privacy views are readable on my phone |
| US-89 | As a user, I can manage applications on a Kanban board; illegal stage moves are refused with a reason |
| US-90 | As a user, I can browse the question bank and log practice attempts, so my mastery actually changes |
| US-91 | As a new user, onboarding asks my target role and date and gives me a starter plan |
| US-92 | As a user, I can reset a forgotten password and verify my email |
| US-93 | As a user, I can sign in with Google or GitHub |
| US-95 | As a user, I can track contacts and referrals per company and get nudged when a contact goes cold |
| US-96 | As a user, each company has a page with all my applications, interviews, questions, contacts and notes |
| US-100 | As a user, I can compare offers side by side with deadline countdowns |
| US-102 | As an operator, AI runs on a local model by default, with an optional hosted provider behind one interface |
| US-103 | As a user, I can chat with an AI coach that answers from my own data |
| US-104 | As a user, pasting a job posting creates a filled-in application |
| US-105 | As a user, AI turns my rough debrief notes into tagged interview questions that feed my prep |
| US-106 | As a user, I get "solve next", daily and interview-driven question recommendations, each with a reason |
| US-107 | As a user, each application shows how ready I am for that specific job |
| US-108 | As a user, I can check my resume against a job description and get tailoring suggestions |
| US-109 | As a user, I can take an AI mock interview that grades my answers and records them as practice |
| US-110 | As a user, I keep a STAR story bank that AI helps me structure |
| US-111 | As a user, I get a one-page prep pack before each interview |
| US-112 | As a user, AI drafts follow-up and negotiation emails for me |
| US-113 | As a user, I get an AI-written weekly review of my search |
| US-114 | As a user, I can see which resume version performs best and simulate how many applications I need |
| US-115 | As a user, I see benchmarks learned from the whole cohort without anyone's data being shared |

## SPRINT 6 — Experience & Core Gaps

| # | Owner | Reviewer | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|
| P70 | — | — | `feat(frontend): design system — tokens, typography, light/dark themes (US-85)` | Colour/type/spacing tokens, "calm & precise" visual language (neutral greys, one indigo accent, Geist), system-default theme + toggle | — | Both themes pass contrast checks; RTL theme-toggle test |
| P71 | — | — | `feat(frontend): app shell — user menu, logout, breadcrumbs, command palette (US-86)` | Sidebar user menu with logout, page titles + detail breadcrumbs, ⌘K palette | — | RTL: logout clears session; palette navigates |
| P72 | — | — | `feat(frontend): feedback layer — toasts, label formatting, form validation (US-87)` | Shared toast, one formatter for every enum value, client-side validation with friendly messages, consistent empty/loading/error states | — | RTL: no raw enum strings rendered; invalid form blocked before submit |
| P73 | — | — | `feat(frontend): responsive tables, charts and privacy views (US-88)` | Card rows on mobile, responsive Recharts with legends, conversion-card fixes, mobile vector inspector, plain-language privacy page | — | Playwright screenshots at 390px and 1440px; conversion numbers match the funnel |
| P74 | — | — | `feat(applications): Kanban board with State-guarded moves (US-89)` | Drag between stages; server's State pattern decides, illegal moves snap back with the reason; note prompt for backward moves | uses **State** | RTL + API: illegal drag refused, legal drag persisted with history |
| P75 | — | — | `feat(applications): filters, search, full form fields, status UX fixes (US-16,17)` | Filter bar over the P16 Specification API, all application fields in the form, only legal next statuses offered | — | RTL + MockMvc |
| P76 | — | — | `feat(prep): question bank browser + practice logging (US-90)` | Browse/filter questions, log attempts, topic drill-down; mastery updates visibly | — | RTL + API: logging an attempt changes mastery |
| P77 | — | — | `feat(users): profile page + onboarding starter plan (US-91)` | Profile editing, first-run onboarding (target role/date) seeding tasks and goals | — | RTL; new user lands on onboarding, not an empty dashboard |
| P78 | — | — | `fix(recommendations): accept creates a task, clear dismiss scope, snoozed view (US-55)` | Visible accepted state, error handling, dismiss warns about type-wide hiding, manage snoozed/dismissed | — | RTL + API |
| P79 | — | — | `feat(auth): password reset + email verification (US-92)` | Expiring single-use tokens; mail sender behind an interface (log/Mailpit in dev) | **Strategy** (mail sender) | Token reuse and expiry rejected; no email enumeration |
| P80 | — | — | `feat(auth): Google and GitHub sign-in (US-93)` | Spring Security OAuth2 client issuing the app's own JWTs; link only on provider-verified email; ADR-012 records the scope change | — | Unverified-email linking refused; existing auth tests unchanged |
| P120 | — | — | `feat(frontend): visual polish — landing and auth pages, dashboard, navigation (US-85)` | Bolder visual identity after review: a public landing page, split-layout sign-in and register with product preview, richer dashboard (progress rings, streak, next actions), refined sidebar and cards, subtle motion that respects reduced-motion, both themes | — | RTL: landing and auth render in both themes; no layout break at 390px and 1440px; axe has no critical violations on landing and auth |
| P121 | — | — | `feat(frontend): consistent professional polish across all application pages (US-85)` | Review and refine every inner page to one visual standard: applications, resumes, interviews, prep, analytics, recommendations, reports, notifications, account; shared page header, table, form, empty-state and chart patterns; no one-off styling | — | RTL for each page in both themes; screenshots at 390px and 1440px reviewed; axe has no critical violations |
| P105 | — | — | `fix(auth): Argon2id parameters and refresh-token lifetime per NFR-01/02` | Argon2id ≥64 MiB / ≥3 iterations with transparent rehash on next login; refresh tokens ≤7 days | — | Test asserts hash parameters and TTLs; existing users still log in |

**Sprint 6 DoD:** every audit "Broken" item fixed; both themes and both breakpoints reviewed with screenshots; practice logging moves mastery end to end.

## SPRINT 7 — Job Search Workflow

| # | Owner | Reviewer | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|
| P82 | — | — | `feat(db): contacts, company notes, offers, goals migration (US-95,96,100,101)` | Flyway scripts + rollbacks | — | Migrates cleanly |
| P83 | — | — | `feat(contacts): contacts and referrals + cold-contact nudges (US-95)` | CRUD, referral status, nudge via the P59 Observer events | uses **Observer** | Isolation test (404) |
| P84 | — | — | `feat(companies): company pages (US-96)` | Aggregated view per company | — | API + RTL |
| P88 | — | — | `feat(offers): offer comparison + deadline countdown (US-100)` | Side-by-side compare | — | API + RTL |
| P106 | — | — | `feat(resumes): structured resume section editor (US-23)` | Edit sections (experience, projects, skills); clones carry sections; export a clean PDF resume from the sections | — | Isolation test; clone copies edited sections |

**Sprint 7 DoD:** every new endpoint isolation-tested.

## SPRINT 8 — AI & Intelligence

AI runs on a local model (Ollama) by default, so user data never leaves the server and AI can be on for everyone; Google Gemini is an optional provider, and the UI discloses when it's active.

| # | Owner | Reviewer | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|
| P90 | — | — | `feat(ai): AiProvider strategy — local Ollama default, optional Gemini (US-102)` | Provider interface, Ollama + Gemini implementations, streaming, timeouts, graceful "AI unavailable", per-user rate limits, AI usage log (no prompt content stored); ADR-013 records the LLM scope change and the local-first choice | **Strategy** | Contract tests against stubbed providers; swapping provider touches no feature code |
| P91 | — | — | `feat(ai): Cohort Coach chat grounded in the user's data (US-103)` | Streaming chat; the app injects the user's own data into context; causal-language linter applied to replies | — | Grounding test: coach never sees another user's data |
| P92 | — | — | `feat(ai): job posting to application parser (US-104)` | Structured extraction with validation; user confirms before save | — | Malformed model output rejected, never saved |
| P93 | — | — | `feat(ai): debrief notes to tagged questions (US-33,34,105)` | Extract questions, map to topics, promote to the question bank | — | Fixture-based extraction tests |
| P94 | — | — | `feat(prep): question recommendations with embeddings (US-106)` | Local embeddings; solve-next, daily picks, interview-driven; each pick states its reason | — | Deterministic ranking tests on fixtures |
| P95 | — | — | `feat(prep): job readiness score per application (US-107)` | Job skills mapped to topics, weighted by mastery; gated by sample size | uses **Template Method** | Golden-file tests |
| P96 | — | — | `feat(ai): resume and job description match (US-108)` | Gaps, tailoring suggestions, and a tailored cover letter draft | — | Fixture tests |
| P97 | — | — | `feat(ai): mock interview with graded answers (US-109)` | Questions from weak topics; grades recorded as practice attempts | — | Attempts recorded with the right topic |
| P98 | — | — | `feat(ai): STAR story bank (US-110)` | CRUD + AI structuring + competency tags | — | Isolation test |
| P99 | — | — | `feat(ai): interview prep pack (US-111)` | One-page brief per upcoming interview | — | Content assembled only from the user's data |
| P100 | — | — | `feat(ai): follow-up and negotiation email drafts (US-112)` | Drafts only; never sent automatically | — | API + RTL |
| P101 | — | — | `feat(ai): weekly review (US-113)` | Generated from analytics; delivered through the notification channels | — | Fixture tests |
| P102 | — | — | `feat(analytics): resume performance and funnel simulator (US-26,114)` | Response rate per resume version; applications-needed simulator | uses **Template Method** | Golden-file tests |
| P103 | — | — | `feat(fl): cohort benchmarks from the federated model (US-115)` | Benchmarks from the global model only; min-cohort rule applies | — | Benchmarks hidden below the min-cohort size |
| P110 | — | — | `feat(prep): spaced review queue (US-42)` | Daily review queue scheduled from mastery decay | — | Deterministic scheduling tests on fixtures |
| P111 | — | — | `feat(fl): privacy budget meter (US-66)` | Cumulative ε per user from the ledger, shown against the configured budget | — | Meter total equals the sum of the user's ledger entries |

**Sprint 8 DoD:** every AI feature works with the local model and degrades gracefully when it's down; no AI prompt contains another user's data; Gemini disclosure shown whenever it's active.

## SPRINT 9 — Hardening & Delivery

| # | Owner | Reviewer | Commit message | Scope | Pattern | Verify |
|---|---|---|---|---|---|---|
| P104 | — | — | `chore: seed data for the extension features (US-72)` | Demo data for contacts, offers, goals, stories, AI history | — | Seed loads identically twice |
| P114 | — | — | `chore(security): security headers, secret scan, SQL-interpolation gate (NFR-03,08,09)` | CSP/nosniff/Referrer-Policy/HSTS, CI secret scanning, CI check for string-built SQL | — | Header test; CI gates fail on a planted violation |
| P119 | — | — | `feat(frontend): privacy policy, AI disclosure, help pages (NFR-16)` | Plain-language data, FL and AI disclosure; keyboard shortcut help | — | Linked from sign-up and settings |
| P66 | P10 | all | `test: full security suite (US-73)` | Enumerate every endpoint from the OpenAPI schema (springdoc), including all Sprint 6–8 endpoints; assert 401/404 systematically | — | Full suite green |
| P67 | P10 | P4 | `chore(a11y): accessibility remediation pass (US-74)` | Contrast, focus traps, aria-labels across the redesigned UI in both themes | — | axe-core: zero critical violations |

**Scope freeze.** After a scope review the feature set is final: tracking (list, board, companies, contacts, offers), resumes (files, structured editor, PDF export, versions, performance), interviews (debriefs, prep packs, mock interviews, story bank), preparation (question bank, practice, mastery, spaced review, recommendations, readiness), analytics, in-app notifications, reports, AI assistance, federated learning with a Privacy Ledger and budget meter, and sign-in with email or Google/GitHub. Deliberately excluded: calendar view and .ics export, CSV import, global search, weekly goals and heatmap, tags and bulk update, recruiter feedback, email notification channel, admin console, structured ops logging, CI quality gates, load and cross-browser tests, production deploy profile, a browser extension, job-board scraping or auto-apply, two-way calendar/email sync, MFA, sharing between users, and mobile apps (the web app is fully responsive instead).

**Project DoD:** everything in the Sprint 5 DoD, plus the Sprint 6–8 DoDs, plus every NFR in `PROJECT_PLAN.md` §8 that is in scope either verified by an automated check or recorded with a manual result (table below). NFRs whose verification phases were cut are listed as not measured.

### Requirement traceability

Every user story in `PROJECT_PLAN.md` maps to at least one phase. US-69 (schema and migrations) is delivered by every `feat(db)` phase rather than one commit.

| NFRs | Delivered or verified by |
|---|---|
| NFR-01, 02 | P05, corrected in P105 |
| NFR-03, 08, 09 | P114 |
| NFR-04, 05 | P66 |
| NFR-06 | P19, P20 |
| NFR-07 | P05 |
| NFR-10–13 | P43–P51, P54 |
| NFR-14 | P65 |
| NFR-15, 16 | P41, P91, P119 |
| NFR-17–22 | Not load-tested (P117 cut from scope) |
| NFR-23, 24 | Architecture (stateless API; config-driven FL cohort) — documented in `docs/architecture.md` |
| NFR-25, 29 | Not in scope (P69 cut) |
| NFR-26, 27 | Migration rollback scripts per `feat(db)` phase; transactional tests in P66 |
| NFR-28 | P49 |
| NFR-30, 31 | P25, P77 |
| NFR-32–35 | P72, P73 |
| NFR-36–39 | Not enforced in CI (P115 cut from scope) |
| NFR-40 | ADRs throughout; ADR-012 in P80 |
| NFR-41, 42 | P02 |
| NFR-43 | Not in scope (P118 cut) |
| NFR-44–48 | P70, P73, P67 |
| NFR-49–51 | Throughout (injected clock/storage/FL gateway; seeded randomness; golden files) |
| NFR-52 | Not in scope (P116 cut) |

---

## Quick reference: the commits worth walking an evaluator through personally

- **P10** — the State pattern replacing if/else transition logic
- **P19** — Decorator + Chain of Responsibility on file upload, and the malicious-upload test
- **P29** — Template Method structurally enforcing the sample-size rule
- **P30** — mastery decay golden-file tests
- **P39/P41** — the recommendation Chain of Responsibility, and the causal-language linter that fails the build
- **P43–P53** — the entire FL chain, especially **P47** (min-cohort refusal), **P50** (CI proves fl-service has no DB credentials), and **P53** (the circuit breaker fault-injection test — this is the single best "we thought about failure modes" commit in the whole project)
- **P59/P60** — Observer + Strategy cooperating cleanly on notifications
- **P74** — the Kanban board, where the P10 State pattern visibly refuses illegal moves
- **P90/P91** — the AI provider Strategy and a coach grounded strictly in the user's own data, on a local model so privacy holds
- **P103** — cohort benchmarks: the federated model turned into something users can see

Everything else is solid, necessary engineering — these are the commits that show design judgment, not just working code.
