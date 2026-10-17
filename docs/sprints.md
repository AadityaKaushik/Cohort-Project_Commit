# Cohort: Sprints and Phases

Cohort is built in nine two-week Scrum sprints, split into 104 phases. Each phase is one commit with its own tests, reviewed by someone other than its author. Requirements and user stories are in [`requirements.md`](requirements.md); the architecture and design patterns are in [`architecture.md`](architecture.md); the tests behind each check are listed in [`testing.md`](testing.md).

| Sprint | Goal | Phases |
|---|---|---|
| 1 | Foundation | P01-P15 |
| 2 | Resumes and interview capture | P16-P26 |
| 3 | Preparation and analytics | P27-P38 |
| 4 | Recommendations and federated learning | P39-P59 |
| 5 | Notifications, reports and account | P60-P69 |
| 6 | Experience and core gaps | P70-P82 |
| 7 | Job-search workflow | P83-P89 |
| 8 | AI and interview preparation | P90-P98 |
| 9 | Hardening and delivery | P99-P104 |

## Team

The team of ten works in three departments. Each department builds and tests its own phases, and every phase is reviewed by someone outside it.

| Department | Members | Typical phases |
|---|---|---|
| Backend | Anuj Madhani, Harshit Goyal, Achintya Chandwani, Dev Bhavsar | Migrations, Spring Boot services and endpoints, security, analytics, recommendations, notifications, reports |
| Frontend | Anshul Rai, Chinmay Dak | React pages and components, design system, accessibility |
| AI/ML | Aaditya Kaushik, Nirmal Nakum, Ved Dhanani, Abhishek Singh | fl-service (Flower, scikit-learn), AI provider and AI features, CI and environment set-up |

Backend writes the JUnit, MockMvc and Testcontainers tests for its phases, Frontend writes the React Testing Library tests and accessibility checks, and AI/ML writes the pytest suites and AI fixture tests.

Sprint 1 phases each have a named owner:

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

From Sprint 2 on, each phase names its department, and the department assigns it to one member at sprint planning.

## How a sprint runs

| Ceremony | When | Length | Output |
|---|---|---|---|
| Sprint planning | Start of sprint | 90 min | Sprint backlog, owners, sprint goal |
| Daily standup | Every day, often async in the team chat | 10 min | Blockers raised within a day |
| Backlog refinement | Mid-sprint | 60 min | Next sprint's stories estimated, acceptance criteria filled in |
| Sprint review | End of sprint | 60 min | Working demo from seed data, updated backlog |
| Retrospective | End of sprint | 45 min | Three things to keep, three to change, one action with an owner |

The board runs Backlog, Sprint Backlog, In Progress, In Review, Testing, Done, with at most two cards per person in progress.

## Working rules

- One phase per commit, with a conventional commit message `type(scope): description (US-XX)`. Two phases are never merged into one commit.
- A phase starts only after the phases in its Depends on column are committed.
- A change to a class that carries a design pattern (Pattern column) names that pattern in one sentence in its pull request.
- Every phase that adds backend logic ships its tests in the same commit: JUnit 5 for units, MockMvc for controllers, Testcontainers for anything that touches Postgres.
- Every endpoint on user-owned data has a cross-user isolation test: another user's id returns 404, never 403.
- The suites (`mvn test`, `npm test`, `pytest`) pass before every commit.
- LinkedIn and Naukri have no public API for a user's applications. Their data reaches Cohort only through the CSV import and the save bookmarklet, both started by the user. There is no scraping and no account connection.

## Definition of Done

A story is done when:

- its acceptance criteria are met and shown in a demo;
- lint, type checks and all tests pass locally and in CI, and coverage has not dropped;
- new logic has unit tests, new endpoints have API tests and new UI has component tests;
- every endpoint on user-owned data has an isolation test;
- input is validated on both client and server;
- the UI has empty, loading and error states and works from the keyboard;
- someone other than the author has reviewed and approved it;
- documentation is updated wherever behaviour or an API changed.

A sprint is done when every committed story meets the story DoD, CI is green, a fresh clone runs with `docker compose up`, the review demo runs from seed data without a code change, the retrospective is written up, and every member has authored and merged at least one reviewed pull request. Each sprint below adds its own checks.

---

## Sprint 1: Foundation

Goal: a running skeleton in which a user can register, log in and manage applications end to end, plus the concept poster. The sprint is a vertical slice through every layer, from React to Postgres to CI.

Stories: US-01 to US-05, US-09 to US-15, US-67 to US-71.

| Phase | Department | Depends on | Delivers | Pattern | Verified by |
|---|---|---|---|---|---|
| P01 | AI/ML | - | Monorepo layout: `.gitignore`, README, LICENSE, CONTRIBUTING, `.env.example`, empty `api/`, `fl-service/`, `frontend/`, `docs/` | - | Repo clones clean with no secrets |
| P02 | AI/ML | P01 | Docker Compose dev environment (db, api, web), `api/Dockerfile`, `frontend/Dockerfile` | - | `docker compose up` starts all three |
| P03 | Backend | P01 | Spring Boot skeleton, `config/` package, health endpoint | - | Health returns 200 |
| P04 | Backend | P03 | `users` and `refresh_tokens` Flyway migrations | - | Migrates on an empty database and rolls back |
| P05 | Backend | P04 | Spring Security with JWT and Argon2id, `/auth/*` endpoints, auth rate limiting | - | Happy path, bad password, 401s, no email enumeration |
| P06 | Backend | P05 | `GET` and `PATCH /users/me` | - | MockMvc test including 401 |
| P07 | Frontend | P01 | Vite, React, TypeScript, Tailwind and shadcn/ui scaffold, app shell, routing, 404 page | - | Shell renders |
| P08 | Frontend | P05, P07 | Login, register, protected routes, token refresh | - | RTL tests and a manual login |
| P09 | Backend | P04 | Companies, applications, status history and tags migrations | - | Migrates cleanly |
| P10 | Backend | P09 | One state class per application status, each owning its legal transitions; `UserScopedRepository` base that needs the owner id on every finder | State, Repository | Every transition pair, valid and invalid |
| P11 | Backend | P10 | Application CRUD and status endpoints with DTOs and mappers; input rules on names, URLs, dates, salary ranges and free text; `POST /api/v1/validate/text` dry run | DTO and Mapper | Isolation test (404); junk text and impossible dates return field errors |
| P12 | Frontend | P08, P11 | Applications list, detail, form and timeline | - | Empty, loading and error states |
| P13 | AI/ML | P03 | CI: Maven build, JUnit, JaCoCo coverage gate | - | CI green on a trivial pull request |
| P14 | Backend | P11 | Register, log in, create, change status, read history as one Testcontainers journey | - | Backend coverage at least 70% |
| P15 | AI/ML | P14 | Architecture documentation and concept poster | - | Poster reviewed by the team |

Sprint 1 DoD: `docker compose up` works on every member's machine; migrations apply and roll back cleanly; the isolation suite passes for every endpoint; backend coverage is at least 70%; the state classes match the status transition table exactly; the poster is reviewed by everyone.

---

## Sprint 2: Resumes and interview capture

Goal: users manage resume versions, attach them to applications, and record each interview round with the questions asked and a quick debrief.

Stories: US-16, US-17, US-20 to US-22, US-24, US-25, US-27 to US-31, US-72.

| Phase | Department | Depends on | Delivers | Pattern | Verified by |
|---|---|---|---|---|---|
| P16 | Backend | P11 | Application search and filters (status, priority, tag, date) that combine | Specification | MockMvc tests on combined filters |
| P17 | Backend | P09 | Resumes and resume sections migrations | - | Migrates cleanly |
| P18 | Backend | P17 | `FileStorageStrategy` with local file storage | Strategy | A file round-trips |
| P19 | Backend | P18 | `ValidatingFileStorage` wrapping the storage, with extension, MIME type and magic-byte checks run in order | Decorator, Chain of Responsibility | A renamed executable is rejected |
| P20 | Backend | P17, P19 | Resume CRUD, upload and expiring authenticated download | - | Another user's file returns 404 |
| P21 | Backend | P20 | Attach a resume to an application; clone a resume with its lineage | - | API tests |
| P22 | Frontend | P20, P21 | Resume list, detail, upload and clone pages | - | RTL tests and a manual upload |
| P23 | Backend | P09 | Interviews, interview questions and interview feedback migrations | - | Migrates cleanly |
| P24 | Backend | P23 | Interview CRUD, questions and feedback; unique round numbers; text and date rules on notes; `GET /interviews/{id}/calendar.ics` | - | Isolation test; another user's .ics returns 404 |
| P25 | Frontend | P24 | Interview pages and the one-tap debrief: outcome and topic chips preselected from the round type, opened from a "How did it go?" prompt on the dashboard and in notifications | - | RTL tests for the chips and the one-tap save |
| P26 | Backend | P11, P20, P24 | Seed data for development, marked as synthetic | - | Seed loads the same way twice |

Sprint 2 DoD: upload rejects a renamed executable; another user's file returns 404; a debrief takes under three minutes; seed data loads the same way twice.

---

## Sprint 3: Preparation and analytics

Goal: recorded history turns into measured preparation progress and a clear picture of the search, with a dashboard that points to the next action.

Stories: US-18, US-34, US-35 to US-41, US-43 to US-50, US-90.

| Phase | Department | Depends on | Delivers | Pattern | Verified by |
|---|---|---|---|---|---|
| P27 | Backend | P23 | Topics, prerequisites, questions and practice attempts migrations with seed data | - | Prerequisite graph has no cycles |
| P28 | Backend | P27 | Question bank and practice attempt endpoints | - | API tests |
| P29 | Backend | P27, P28 | Original interview questions for all 48 topics (DSA, system design, OS and networking, databases, OOP, behavioural) and a one-line description for each topic, loaded by a repeatable migration that keeps practice history | - | Every topic has real questions; old attempts still resolve |
| P30 | Backend | P11 | `AnalyticsMetric` base class: compute, sample-size gate, format | Template Method | A new metric cannot skip the sample-size gate |
| P31 | Backend | P28 | Mastery engine with time decay | - | Golden-file tests |
| P32 | Backend | P31 | Preparation tasks, progress endpoints, topic grid and progress bars | - | RTL and API tests |
| P33 | AI/ML | P02 | Two-client Flower prototype to check the FL approach, kept out of the production path | - | One FedAvg round between two clients |
| P34 | Backend | P30 | Funnel, conversion and timeline metrics | uses Template Method | Golden-file tests |
| P35 | Backend | P34 | Rejection distribution, dwell times and sample-size gating | uses Template Method | Golden-file tests |
| P36 | Frontend | P34, P35 | Analytics page with Recharts: funnel, conversion cards, sample-size badges | - | RTL tests with empty and loading states |
| P37 | Backend | P11, P30 | Stall detection: days in stage against the user's own median | uses Template Method | Unit tests on the threshold |
| P38 | Frontend | P36, P37 | Dashboard pipeline and action queue | - | RTL tests; queue capped at seven items |

Sprint 3 DoD: every analytics metric has a golden-file test; the topic graph is acyclic; every topic has real questions; the Flower prototype runs a round.

---

## Sprint 4: Recommendations and federated learning

Goal: explainable recommendations with evidence tiers, and a federated learning loop with a Privacy Ledger the user can inspect. This is the riskiest sprint, so its commits are the smallest.

Stories: US-51 to US-65, US-84.

| Phase | Department | Depends on | Delivers | Pattern | Verified by |
|---|---|---|---|---|---|
| P39 | Backend | P27, P32 | Recommendations and recommendation evidence migrations | - | Migrates cleanly |
| P40 | Backend | P31, P37, P39 | Recommendation generators in an ordered chain: weak topic, prerequisite gap, decay, coverage, stalled application | Chain of Responsibility | A unit test per generator |
| P41 | Backend | P40 | Routing a weak topic to its root-cause prerequisite | - | Unit tests including the depth cap |
| P42 | Backend | P41 | Evidence tiers A to D with one scoring strategy per tier; template registry for recommendation text | Strategy | Causal-language linter fails the build on a banned phrase |
| P43 | Backend | P32, P42 | `RecommendationFacade` with accept, snooze and dismiss, and the Recommendations page | Facade, Builder | RTL and API tests |
| P44 | AI/ML | P33 | FL feature schema: the 24 named features in one place | - | Exactly 24 named floats |
| P45 | AI/ML | P44 | Local feature extractor, correct at a point in time | - | No record after the cutoff leaks in |
| P46 | AI/ML | P45 | Local scikit-learn trainer | - | Deterministic under a fixed seed |
| P47 | AI/ML | P46 | Update clipping and Gaussian noise | - | Sent vector differs from the raw update; norm within the bound |
| P48 | AI/ML | P47 | Flower server with FedAvg and a minimum cohort size | - | A two-client round is refused and logged |
| P49 | AI/ML | P48 | Norm-based poisoning filter | - | A crafted over-norm update is dropped |
| P50 | AI/ML | P48 | Model versions and the `/fl/model` endpoint | - | A failed round leaves the previous model servable |
| P51 | AI/ML | P02, P50 | Compose profile for the FL service with its own schema and role | - | CI shows fl-service has no credentials for user tables |
| P52 | Backend | P31, P44, P51 | Export of the 24 allow-listed features for opted-in users | - | Payload matches the allow-list, no free text |
| P53 | Backend | P50, P52 | `FederatedLearningGateway` and `FlowerServiceAdapter` | Adapter | Contract test against a stubbed fl-service |
| P54 | Backend | P53 | Resilience4j circuit breaker with a local-only fallback | Circuit Breaker | fl-service stopped mid-request: 200 with local results |
| P55 | Backend | P06, P44 | FL opt-in and opt-out endpoints and the consent screen | - | Default off; an opted-out user sends nothing |
| P56 | Backend | P51, P53 | Privacy Ledger: round metadata and the vector each user sent | - | The real sent vector is stored per user per round |
| P57 | Frontend | P56 | Privacy Ledger page with a vector inspector | - | RTL tests |
| P58 | Frontend | P43, P54 | Federated Lift viewer showing how the shared model moves the ranking | - | Adjustment stays within 0.75 to 1.25 |
| P59 | AI/ML | P48, P56 | Demo command that runs a federated round with synthetic clients, so the Privacy Ledger can be shown with few real users | - | Synthetic clients are deterministic; the round writes ledger entries |

Sprint 4 DoD: the payload schema test passes; a two-client round is refused; CI shows fl-service has no credentials for user tables; the phrasing linter passes; the circuit-breaker fault-injection test passes; a Privacy Ledger entry shows a real sent vector, and the demo round fills the ledger on a fresh database.

---

## Sprint 5: Notifications, reports and account

Goal: in-app notifications driven by domain events, PDF and CSV report export, and full account self-service.

Stories: US-06, US-07, US-08, US-76 to US-80, US-92.

| Phase | Department | Depends on | Delivers | Pattern | Verified by |
|---|---|---|---|---|---|
| P60 | Backend | P09 | Notifications migration | - | Migrates cleanly |
| P61 | Backend | P24, P37 | Domain events for stalled applications, upcoming interviews and missing debriefs; `GET /calendar/upcoming.ics` | Observer | Event fires on the right condition; .ics lines, escaping, UTC times and UIDs |
| P62 | Backend | P60, P61 | `NotificationChannel` with an in-app channel and a listener on the events | Strategy | Swapping the channel does not touch the detector |
| P63 | Backend | P62 | Notification endpoints, bell and mark as read | - | RTL and API tests |
| P64 | Backend | P09 | Report exports migration | - | Migrates cleanly |
| P65 | Backend | P34, P35, P64 | `ReportBuilder` for PDF and CSV, reusing the analytics formulas | Builder | Output matches a fixture |
| P66 | Backend | P20, P65 | Report export endpoints and page, with owner-only expiring downloads | - | Another user's download returns 404 |
| P67 | Backend | P05, P06 | Change password, data export, account deletion | - | No orphan rows after deletion |
| P68 | Backend | P05 | Argon2id at 64 MiB and 3 iterations with rehash on login; refresh tokens of at most 7 days | - | Hash parameters and token lifetimes asserted |
| P69 | Backend | P05, P68 | Password reset and email verification with single-use expiring tokens | Strategy (mail sender) | Reused and expired tokens rejected; no email enumeration |

Sprint 5 DoD: notifications and reports work end to end; account deletion leaves no orphan rows; Argon2id parameters and token lifetimes are asserted; reset and verification tokens are single-use and expiring; no email enumeration.

---

## Sprint 6: Experience and core gaps

Goal: one consistent, responsive interface in light and dark themes, with the everyday interactions a job seeker expects: a board, filters, practice logging, onboarding, social sign-in, and security headers on every response.

Stories: US-16, US-17, US-55, US-85 to US-91, US-93.

| Phase | Department | Depends on | Delivers | Pattern | Verified by |
|---|---|---|---|---|---|
| P70 | Frontend | P07 | Design system: pine and ivory with one apricot accent, condensed display type, stage colours that darken as an application moves on, light and dark themes with a system default | - | Token contrast test (4.5:1 text, 3:1 UI) in both themes; theme toggle test |
| P71 | Frontend | P08, P70 | App shell: grouped sidebar, user menu with logout, breadcrumbs, command palette, keyboard shortcuts ignored while typing | - | Logout clears the session; shortcuts ignored in inputs |
| P72 | Frontend | P70 | Feedback layer: toasts, one label formatter for every enum, client validation, soft warnings with Save anyway, undo for deletes, unsaved-changes warning, drafts kept in localStorage | - | No raw enum strings; undo cancels the delete; drafts restored |
| P73 | Frontend | P36, P57, P70 | Responsive tables, charts and privacy views; card rows on phones | - | Layouts at 390 px and 1440 px |
| P74 | Frontend | P10, P12, P72 | Kanban board where the server's state classes decide each move and illegal moves snap back with the reason | uses State | Illegal drag refused; legal drag saved with history |
| P75 | Frontend | P16, P72 | Filter bar, all application fields, quick status menu, form defaults, company autocomplete, Duplicate, paste-a-posting parser, "Save to Cohort" bookmarklet | - | Parser and bookmarklet unit tests; prefill and filter RTL tests |
| P76 | Frontend | P28, P31, P72 | Question bank browser and practice logging, with mastery updating on the page | - | Logging an attempt changes mastery |
| P77 | Frontend | P06, P32, P72 | Profile page, onboarding with a starter plan, CSV import that recognises LinkedIn's export and common tracker sheets | - | New users land on onboarding; import parser, preview and isolation tests |
| P78 | Backend | P32, P43 | Accepting a recommendation creates a task; clear dismiss scope; snoozed and dismissed views | - | RTL and API tests |
| P79 | Backend | P05, P69 | Google, GitHub and LinkedIn sign-in issuing the app's own tokens; an account is linked only on a provider-verified email | - | Unverified email is not linked; provider registration tests |
| P80 | Frontend | P38, P70, P71, P79 | Landing page, split sign-in and register pages, dashboard with the pipeline drawn as a staircase | - | Both themes render; no layout break at 390 px and 1440 px |
| P81 | Frontend | P80 | One visual standard across every inner page: page header, tables, forms, empty states, charts, readiness steps on the prep page | - | RTL per page in both themes; contrast test |
| P82 | Backend | P05, P13, P79 | Security headers (CSP, nosniff, Referrer-Policy, HSTS), secret scan and a CI check against SQL built from strings | - | Header test; CI gates fail on a planted violation |

Sprint 6 DoD: both themes and both breakpoints are reviewed with screenshots; tokens pass the contrast test in both themes; logging a practice attempt moves mastery end to end; illegal board moves are refused with a reason; the header test passes and the CI gates fail on a planted violation.

---

## Sprint 7: Job-search workflow

Goal: the networking and decision side of a job search: contacts and referrals, a page per company, offer comparison, weekly goals, one search box across everything, and a structured resume editor.

Stories: US-23, US-95, US-96, US-99, US-100, US-101.

| Phase | Department | Depends on | Delivers | Pattern | Verified by |
|---|---|---|---|---|---|
| P83 | Backend | P09, P11 | Contacts, company notes, offers and goals migrations with rollbacks | - | Migrates cleanly |
| P84 | Backend | P61, P83 | Contacts and referrals, nudges for contacts gone cold, and referral and reconnect message drafts | uses Observer | Isolation test; drafts hold only the user's own data |
| P85 | Backend | P24, P83, P84 | Company pages, a read-only company directory of sourced interview patterns, and anonymous opt-in experience contributions shown only with five or more contributors | - | Unknown slug is 404; figures hidden below five contributors |
| P86 | Backend | P83 | Offer comparison with deadline countdowns | - | API and RTL tests |
| P87 | Backend | P38, P77, P83 | Weekly goals, activity heatmap and streak | - | Isolation test |
| P88 | Backend | P28, P71, P84, P85 | Global search across applications, companies, questions and notes, in the command palette | - | Results never include another user's data |
| P89 | Backend | P17, P21 | Structured resume section editor with PDF export; clones carry sections | - | Isolation test; clone copies edited sections |

Sprint 7 DoD: every new endpoint has an isolation test; search never returns another user's data; a cloned resume carries its edited sections.

---

## Sprint 8: AI and interview preparation

Goal: AI help that works only from the user's own data on a local model, plus the interview preparation tools: question saving, readiness, mock interviews, stories, prep packs and a review queue. AI runs on Ollama by default, so user data stays on the server; Google Gemini is optional and the UI says when it is active.

Stories: US-42, US-102, US-107 to US-112, US-118.

| Phase | Department | Depends on | Delivers | Pattern | Verified by |
|---|---|---|---|---|---|
| P90 | AI/ML | P03 | `AiProvider` with Ollama as default and Gemini as an option: streaming, timeouts, an "AI unavailable" state, per-user rate limits, a usage log that stores no prompt text | Strategy | Contract tests on stubbed providers; swapping provider changes no feature code |
| P91 | Backend | P24, P28 | Save a question asked in an interview into the question bank, linked to its topic | - | Saved question is linked; another user's question returns 404 |
| P92 | Backend | P11, P30, P31 | Readiness score per application: posting skills mapped to topics and weighted by mastery, gated by sample size | uses Template Method | Golden-file tests |
| P93 | AI/ML | P20, P89, P90 | Resume and job description match: gaps, tailoring suggestions, cover letter draft | - | Fixture tests |
| P94 | Backend | P28, P31, P76 | Self-graded mock interview from the weakest topics; each answer is saved as a practice attempt | - | Attempts recorded under the right topic |
| P95 | Backend | P24 | STAR story bank with competency tags, linkable to an interview | - | Isolation test |
| P96 | AI/ML | P24, P90 | One-page prep pack for each upcoming interview | - | Built only from the user's own data |
| P97 | AI/ML | P86, P90 | Follow-up and negotiation email drafts, never sent automatically | - | API and RTL tests |
| P98 | Backend | P31, P76 | Daily review queue scheduled from mastery decay | - | Deterministic scheduling tests |

Sprint 8 DoD: the AI features work on the local model and show a clear state when it is down; no AI prompt contains another user's data; the Gemini notice shows whenever that provider is active; mock interview answers land as practice attempts on the right topic; stories and saved questions pass their isolation tests.

---

## Sprint 9: Hardening and delivery

Goal: a demo-ready product: realistic data, a first-run experience, designed error states, privacy and AI disclosure pages, an accessibility pass, and a security check across every endpoint.

Stories: US-72, US-73, US-74, US-116, US-117.

| Phase | Department | Depends on | Delivers | Pattern | Verified by |
|---|---|---|---|---|---|
| P99 | Backend | P26, P84, P86, P87, P95 | Seed data for contacts, offers, goals, stories and AI history | - | Seed loads the same way twice |
| P100 | Frontend | P77, P99 | One-click sample data that is marked and removable; a guided product tour across every area with Skip, Back and Next; per-page guides from a "How this page works" button | - | Sample data touches only the current user; removal leaves no orphans; tour works at 375 px |
| P101 | Frontend | P71, P72 | Designed error, 404, offline and session-expired screens | - | RTL for each state in both themes |
| P102 | Frontend | P55, P90 | Privacy policy, AI disclosure and help pages | - | Linked from sign-up and settings |
| P103 | Backend | P14, P102 | Security suite that reads every endpoint from the OpenAPI schema and checks 401 without a token and 404 on another user's id | - | Suite green |
| P104 | Frontend | P81, P102 | Accessibility and polish pass: skeleton loaders, lazy charts and routes, phone layouts with 40 px touch targets, one h1 per page, skip link, labelled controls, dialog focus, aria-live, reduced motion | - | axe-core: no critical violations |

Sprint 9 DoD: the security suite is green for every endpoint; axe-core reports no critical violations in both themes; sample data and the tour work for a new user; the app works at 375 px without horizontal scroll; the demo runs end to end twice from a fresh clone.

---

## Project Definition of Done

Every user story in `requirements.md` is delivered by the phases above; every NFR is covered by the phase or document listed below; the documentation and poster are complete; the demo runs from a fresh clone; and every member can explain the architecture, the analytics formulas and the federated learning privacy design.

## NFR traceability

US-69 (schema and migrations) is delivered by every migration phase rather than one commit.

| NFRs | Delivered or verified by |
|---|---|
| NFR-01, 02 | P05, P68 |
| NFR-03, 08, 09 | P82 |
| NFR-04, 05 | P103 |
| NFR-06 | P19, P20 |
| NFR-07 | P05 |
| NFR-10 to 13 | P44 to P52, P55 |
| NFR-14 | P67 |
| NFR-15, 16 | P42, P102 |
| NFR-17 to 22 | Documented in `architecture.md` |
| NFR-23, 24 | Documented in `architecture.md` (P15) |
| NFR-25, 29 | Documented in `architecture.md` |
| NFR-26, 27 | Rollback script in every migration phase; transactional tests in P103 |
| NFR-28 | P50 |
| NFR-30, 31 | P25, P77 |
| NFR-32 to 35 | P72, P73, P101 |
| NFR-36, 38, 39 | Documented in `architecture.md` |
| NFR-37 | P13 |
| NFR-40 | `architecture.md`, "Key design choices" |
| NFR-41, 42 | P02 |
| NFR-43 | Documented in `architecture.md` |
| NFR-44 to 48 | P70, P73, P104 |
| NFR-49 to 51 | Throughout: injected clock, storage and FL gateway; seeded randomness; golden files |
| NFR-52 | Documented in `architecture.md` |
| NFR-53 | P62 |

## Key phases

- P10: the State pattern for application status changes.
- P19: Decorator and Chain of Responsibility on file upload, with the malicious-upload test.
- P30: Template Method enforcing the sample-size rule on every metric.
- P31: mastery decay golden-file tests.
- P40 and P42: the recommendation chain and the causal-language linter that fails the build.
- P44 to P54: the federated learning chain, especially P48 (minimum cohort), P51 (fl-service has no credentials for user tables) and P54 (circuit breaker fault injection).
- P59: a federated round with synthetic clients for the demo.
- P61 and P62: Observer and Strategy working together on notifications.
- P74: the Kanban board, where the P10 state classes refuse illegal moves.
- P90: the AI provider Strategy on a local model.
