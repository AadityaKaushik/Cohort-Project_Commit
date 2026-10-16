# Cohort: Project Plan

*Cohort* is a smart job application tracker with federated interview preparation. This document holds the requirements (functional, non-functional and domain), the user stories grouped by epic, the sprint roadmap, and the product, data, ML and federated-learning design.

---

## 1. About this plan

### 1.1 Document set

| Document | Contents |
|---|---|
| `PROJECT_OVERVIEW.md` | Plain-language map of the whole project; read first |
| `PROJECT_PLAN.md` (this file) | Requirements, user stories, backlog, sprint roadmap, product and system design |
| `ARCHITECTURE_REVISION_JAVA.md` | Technology stack, Java/Python architecture, design patterns, notifications and report export |
| `BUILD_PHASES_JAVA.md` | The 110 build phases, one commit each, with owners, dependencies and verification; team and ownership; requirement traceability |

### 1.2 Delivery shape

Nine sprints. Sprints 1-4 deliver the core loop (tracking, interviews, preparation, analytics, recommendations, federated learning); Sprint 5 adds notifications, reports and account self-service; Sprint 6 brings the interface to one consistent standard; Sprint 7 adds the job-search workflow; Sprint 8 adds AI assistance and prep intelligence; Sprint 9 hardens and verifies everything for delivery. §12 describes each sprint.

### 1.3 Conventions

- Requirement IDs (FR, NFR, DR), user-story IDs (US) and phase IDs (P) are stable and cited in commit messages.
- Every user story maps to at least one build phase (§10.1); every phase cites at least one story or requirement.
- The conventions in §25 apply from the first commit.

### 1.4 Planning assumptions

| # | Assumption | Reason |
|---|---|---|
| A1 | The team has 10 members in three departments (Backend, Frontend, AI/ML) | Drives ownership and parallel work (`BUILD_PHASES_JAVA.md`, Team and ownership) |
| A2 | Sprints are 2 weeks; nine sprints in total | Roadmap shape |
| A3 | Deployment target is local Docker Compose | Avoids over-engineering; the FL isolation demo runs locally |
| A4 | Process artifacts (backlog, sprints, conflicts, poster) are part of the deliverable alongside the running product | Documentation effort |
| A5 | Real multi-user data will not exist during the project | The FL demo uses synthetic cohorts |
| A6 | Java 21, Python 3.11+ and Node 20+ are available on team machines | Toolchain |

---

## 2. Project Overview

### 2.1 The problem

A student applying for internships or graduate roles typically runs 40-150 applications across a season. The state of that campaign lives in a spreadsheet, a notes app, three email threads, and memory. Three specific failures follow:

1. Pipeline amnesia. Applications stall silently. Nobody follows up because nobody notices.
2. Post-interview evaporation. The memory of what was actually asked decays within 48 hours. It is rarely captured in a structured way, so it cannot inform the next round of preparation.
3. Blind preparation. Students prepare by grinding a generic list, not by targeting the topics that actually cost them interviews.

Existing tools solve (1) as a Kanban board and ignore (2) and (3) entirely.

### 2.2 The solution

A web application that closes the loop: track → interview → capture what happened → convert that into a targeted preparation plan → measure whether it worked. We then use Federated Learning to learn *which preparation patterns are associated with better outcomes across many users*, without any user's raw data leaving their own data partition.

### 2.3 Design decisions

Seven design decisions shape the project. Each is small enough for a student team to build.

D1. Federated Learning runs on separated data.

Each FL client is a separate OS process with its own data store (its own Postgres schema, with credentials scoped to that schema only). The Flower server has no database credentials, so it cannot read user data. Only a fixed-length float vector of clipped, noised model parameters crosses the boundary. Stopping the server container and inspecting its filesystem shows that it holds no user data.

D2. Privacy Ledger.

Each user has a page that logs every federated round the user's client took part in. For each round it shows the round ID, timestamp, feature schema used, L2 norm of the update before and after clipping, noise multiplier, number of peer clients in the cohort, and a "what was NOT sent" list. The user can expand a round to see the 24-float vector that was sent, revoke participation per round, and export the whole ledger as JSON.

D3. Evidence-tiered recommendations.

Every recommendation carries an evidence badge, and the wording depends on the tier:

| Tier | Source | Example phrasing |
|---|---|---|
| Tier A, Your data | ≥5 of the user's own observations | "In 6 of your last 8 technical rounds, graph questions were marked low-confidence." |
| Tier B, Your data, thin | 2-4 observations | "Early signal only (2 rounds): dynamic programming may be a gap." |
| Tier C, Federated | Global model, cohort ≥ k | "Across the federated cohort, users who logged ≥10 practice reps on a weak topic before a repeat round reported higher confidence. This is an association, not a guarantee." |
| Tier D, Heuristic | Rules, no data yet | "You have no interviews logged. Start with a broad baseline." |

Causal phrasing is not allowed. A test in the suite (`test_no_causal_language`) fails the build if a recommendation template contains banned strings such as "will get you", "guarantees" or "because of this you were rejected".

D4. Topic mastery graph with time decay.

Prep topics are nodes in a directed prerequisite graph (Arrays → Two Pointers → Sliding Window; Recursion → Trees → Graphs → Shortest Path). Each user has a mastery score per topic in [0,1] that:
- rises with successful practice reps and good interview outcomes on that topic,
- decays exponentially with time since the last touch (a forgetting curve, half-life configurable per difficulty),
- propagates weakness backwards: after a failure on Shortest Path, the engine checks whether Graphs and BFS/DFS mastery is the root cause and recommends the prerequisite.

D5. Pipeline health with stall detection.

Besides the funnel, the app computes the time-to-transition distribution for each stage from the user's own history. It flags applications that have exceeded the median dwell time for their stage. The dashboard shows entries such as "3 applications are stalled past your usual OA turnaround" with a suggested follow-up, and adds a sample-size caveat when history is thin.

D6. Federated Lift viewer.

The Recommendations page has a "Local only" / "Local + Federated" toggle. Switching it re-ranks the recommendations and shows which items moved and by how much.

D7. Privacy budget in the UI.

The user sees a budget meter with total ε allocated, ε spent per round, rounds remaining before the budget is exhausted, and what happens at that point (the client stops contributing and says so). The user also picks a privacy posture (Strict / Balanced / Contributor), which sets the noise multiplier and clipping norm.

Design guardrails. The project avoids these patterns:

- A board as the only view: the primary UI is a dense, sortable, filterable data table with a detail view, and the Kanban board is a second view whose moves are checked by the server's state machine.
- Four generic stat cards across the top of a dashboard (one pipeline-health strip and one action queue are used instead).
- A pie chart of application statuses.
- AI as a single prompt to a hosted model: AI features run on a local model by default, answer only from the user's own data, and are disclosed when a hosted provider is active.
- A default shadcn theme with a purple gradient hero and a single font.
- Recommendations phrased as predictions.
- FL shown only as a diagram, with no running processes behind it.

### 2.4 Naming

`Cohort` works on three levels: the peer cohort of job seekers, the FL client cohort, and the minimum-cohort-size privacy guarantee.

---

## 3. Goals & Objectives

### 3.1 Product goals

| ID | Goal | Success measure |
|---|---|---|
| G1 | Single source of truth for a job-search campaign | A user can answer "where does every application stand?" in one screen |
| G2 | Capture interview experience while it is still fresh | Post-interview debrief completable in under 3 minutes |
| G3 | Convert experience into targeted preparation | Every logged interview produces at least one concrete, evidence-backed prep item |
| G4 | Make campaign performance legible | Funnel, conversion rates, and stall detection with defined formulas |
| G5 | Learn across users without collecting their data | Zero raw records at the FL server; provable by inspection |
| G6 | Make privacy inspectable, not just promised | Every byte that leaves a client is user-visible in the ledger |

### 3.2 Course goals

The project is a course deliverable. It follows Agile/Scrum with real sprint artifacts, has a full requirements set (FR/NFR/DR, elicitation, conflict analysis), shows each member's contribution through the GitHub history, and ends as a working, deployable, tested system that uses federated learning.

### 3.3 Explicit non-goals

- Not a job board or scraper. We do not ingest listings from third-party sites (ToS and scope risk).
- Not an LLM wrapper. AI assists the user's own workflow, answers only from the user's own data, and never acts on their behalf (drafts are never sent automatically).
- Not a social network. No user-to-user visibility of any kind, ever.
- Not a production multi-tenant SaaS. Single-region, single-deployment, Compose-based.

---

## 4. Stakeholders

| ID | Stakeholder | Role | Interest | Requirements they bring | Influence | Elicitation technique |
|---|---|---|---|---|---|---|
| S1 | Students / job seekers (primary end user) | Daily user; owns all data | Manage campaign, prepare better, get offers | Fast data entry, mobile-legible, analytics that state their limits, confidence that private notes stay private | High, they define the product; low authority over scope | Semi-structured interviews (6-8), survey (30+), observation of current spreadsheet workflow |
| S2 | Project team / developers | Build, test, document, present | Deliverable scope is achievable; clean architecture; individual contribution is visible | Modular boundaries, CI, seed data, clear DoD | High | Sprint planning, retrospectives, brainstorming |
| S3 | Course staff | Set the project scope and review it | Sound SE process, complete documentation, individual contribution | SCRUM artifacts, requirement traceability, poster, per-member commit history | Very high, they accept the deliverable | Document analysis of the course brief; clarification meetings |
| S4 | System administrator (operational role, played by team) | Deploy, monitor, back up, manage user accounts | Reliable, observable, recoverable system | Health endpoint, structured logs, migrations, backup/restore path, admin ability to disable an account | Medium | Brainstorming; analysis of ops runbooks |
| S5 | ML / FL administrator (operational role, played by team) | Configure and run federated rounds | Rounds converge; privacy parameters are correct; poisoning is contained | Round configuration, min-cohort enforcement, clipping/noise settings, round audit log, model rollback | Medium-high (owns the innovation) | Document analysis of FL literature; prototype experimentation |
| S6 | Database / system maintainer (operational role, played by team) | Schema, migrations, integrity, performance | Normalized schema, safe migrations, indexed queries | Flyway migrations, FK constraints, per-user isolation enforced in the data layer | Medium | Design walkthroughs |
| S7 | University / institution (indirect) | Provides infrastructure, governs conduct | Academic integrity; no misuse of student data; licensing | Data handling within institutional norms; open-source licence compliance | Low-medium, but a hard constraint | Document analysis of institutional policy |

Excluded stakeholders: *Recruiters and employers* never touch the system and have no interface to it. *Career services staff* would be a possible future stakeholder for a cohort-analytics dashboard, but that feature is outside the product scope, so including them would add requirements the project does not build. Both are recorded in Future Scope (§32).

---

## 5. Requirement Elicitation

### 5.1 Strategy overview

Requirements come from three sources: the course brief (fixed), end users (who must be sampled), and the FL/ML literature (technical constraints we do not choose).

### 5.2 Techniques

| Technique | Used with | Gathers | Why suitable | Expected output |
|---|---|---|---|---|
| Document analysis | S3, S7, FL/DP literature, existing job-tracking tools | Mandatory deliverables; established FL algorithms (FedAvg), DP mechanisms, secure-aggregation constraints; feature baselines and their gaps | The brief is fixed and must be traced; FL correctness cannot be derived from intuition | Requirement traceability matrix; FL parameter defaults; feature gap list feeding §2.3 |
| Semi-structured interviews (6-8 × 30 min) | S1 students who are actively applying | The real workflow, the actual pain, what they currently record after interviews, what they refuse to type into a computer, privacy attitudes | Depth on motivations and privacy sensitivity, which surveys cannot reach | Interview transcripts; pain-point inventory; a "sensitive data" list that constrains the FL feature schema |
| Survey (target 30-50 responses) | S1 broader student population | Frequency distributions: applications per season, tools used, drop-off stages, willingness to share anonymised model updates, feature demand ranking | Cheap validation of whether interview findings generalise; gives quantitative backing for prioritisation | Prioritised feature demand table; the willingness-to-federate percentage, which directly justifies opt-in defaults |
| Observation / artefact study | 3-4 S1 volunteers | How their existing spreadsheet or Notion board is actually structured; which columns they created themselves | Users under-report their own workarounds; the columns they invented *are* requirements | Field-derived list of application attributes → validates the §21 data model |
| Brainstorming workshops | S2 team, with S5 hat on | Architecture options, FL task selection, conflict identification, risk register | Design decisions need divergent generation then convergent selection | Decision log (ADRs); requirement conflict list (§14); risk register (§28) |
| Prototype feedback / walkthrough | S1 (5 users), S3 | Reaction to clickable wireframes and to the Privacy Ledger concept specifically | Privacy UX is impossible to specify correctly without showing people something | Sprint 2+ backlog refinements; ledger content validation |

### 5.3 Known requirements vs assumptions needing validation

Everything below is tracked in a live document.

Known, from the course brief:
- The stack (React/TS/Tailwind/shadcn/Recharts; Java/Spring Boot; PostgreSQL with JPA and Flyway; JWT + Argon2id; scikit-learn; Flower; JUnit/pytest/RTL; Docker/Compose/Git/GitHub)
- FL is mandatory and raw data must not reach the central server
- Required documentation set and Agile/SCRUM process
- GitHub development with per-member contribution
- Concept poster in Sprint 1

Known, from domain logic (low risk):
- Application lifecycle stages and the fact that they are non-linear (rejection can occur at any stage)
- Users need multiple resume versions
- Interviews belong to applications and have rounds
- Users must never see each other's data

Assumptions, to be validated before the dependent sprint:

| ID | Assumption | Depends on | Validate by | Deadline |
|---|---|---|---|---|
| V1 | Users will complete a structured post-interview debrief if it takes <3 min | Sprint 2 (EPIC 4) | Interviews + prototype walkthrough | End Sprint 1 |
| V2 | Users will opt in to federated contribution when shown the ledger | Sprint 4 (EPIC 8) | Survey question + prototype reaction | End Sprint 2 |
| V3 | Self-rated confidence per topic is a usable label for the ML task | Sprint 4 (EPIC 7) | Interviews; check whether users can rate consistently | End Sprint 2 |
| V4 | Users track enough applications (≥20) for personal analytics to be meaningful | Sprint 3 (EPIC 6) | Survey | End Sprint 1 |
| V5 | Users want resume-version performance comparison despite weak statistical power | Sprint 3 (EPIC 3/6) | Interviews | End Sprint 2 |
| V6 | Tagging free-text notes is acceptable; users won't demand full-text search | Sprint 2 | Survey | End Sprint 1 |

Any assumption that fails validation triggers a backlog refinement, recorded in the sprint review notes.

---

## 6. Functional Requirements

Priority: M = Must, S = Should, C = Could, Out = outside the product scope (§31.2), kept so the requirement IDs stay stable.

### 6.1 Authentication & User Management

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | The system shall allow a visitor to register with email, password, and display name. | M |
| FR-02 | The system shall reject registration for an email that already exists, without revealing account existence in a timing-observable way. | M |
| FR-03 | The system shall enforce a password policy (≥10 characters, checked against a common-password blocklist). | M |
| FR-04 | The system shall hash passwords using Argon2id with per-user salt; plaintext passwords shall never be stored or logged. | M |
| FR-05 | The system shall authenticate users and issue a short-lived access token plus a longer-lived refresh token. | M |
| FR-06 | The system shall allow a user to log out, invalidating the refresh token server-side. | M |
| FR-07 | The system shall allow a user to view and update their profile (display name, target role, graduation year, timezone). | M |
| FR-08 | The system shall allow a user to change their password after re-authenticating. | S |
| FR-09 | The system shall allow a user to permanently delete their account and all associated data, including any stored FL client state. | S |
| FR-10 | The system shall allow a user to export all of their own data as JSON. | S |
| FR-11 | The system shall support an administrator role able to disable accounts and view aggregate system health, but never able to read another user's application, resume, or interview content. | Out |

### 6.2 Job Application Tracking

| ID | Requirement | Priority |
|---|---|---|
| FR-12 | The system shall allow a user to create an application with company, job title, status, and date applied as required fields. | M |
| FR-13 | The system shall support optional application fields: job description, job URL, location, work mode, job type, salary range and currency, priority, source, referral flag, deadline, recruiter contact, notes, tags. | M |
| FR-14 | The system shall allow a user to view a paginated, sortable list of their own applications. | M |
| FR-15 | The system shall allow a user to full-text search their applications by company and title. | M |
| FR-16 | The system shall allow a user to filter applications by status, stage, priority, tag, date range, and work mode, with filters combinable. | M |
| FR-17 | The system shall allow a user to view a single application's full detail, including related interviews, resume, and status history. | M |
| FR-18 | The system shall allow a user to edit any field of their own application. | M |
| FR-19 | The system shall allow a user to delete an application, with a confirmation step, cascading to its interviews and history. | M |
| FR-20 | The system shall allow a user to change an application's status, and shall record every change as an immutable history entry with timestamp, previous status, new status, and optional note. | M |
| FR-21 | The system shall validate status transitions against a defined state machine and reject invalid transitions (e.g. `Rejected` → `Applied`). | S |
| FR-22 | The system shall render an application's status history as a chronological timeline. | M |
| FR-23 | The system shall maintain a normalised company record, reused across applications, created on demand. | M |
| FR-24 | The system shall allow bulk status update for multiple selected applications. | Out |
| FR-25 | The system shall compute, per application, the number of days spent in the current stage. | M |
| FR-26 | The system shall flag applications whose current-stage dwell time exceeds the user's own median dwell time for that stage (minimum 5 observations), or a system default when history is insufficient. | S |
| FR-27 | The system shall allow archiving an application without deleting it. | C |

### 6.3 Resume Management

| ID | Requirement | Priority |
|---|---|---|
| FR-28 | The system shall allow a user to create a resume record with name, version label, target role, and summary. | M |
| FR-29 | The system shall allow structured resume content: skills (tagged), experience entries, education entries, projects, achievements. | S |
| FR-30 | The system shall allow uploading a PDF or DOCX file (≤5 MB) attached to a resume record. | M |
| FR-31 | The system shall validate uploaded files by extension, declared MIME type, and magic-byte signature, and shall store them outside the web root under a generated name. | M |
| FR-32 | The system shall serve resume files only to their owner via an authenticated, expiring download endpoint. | M |
| FR-33 | The system shall allow a user to maintain multiple resume versions and mark one as default. | M |
| FR-34 | The system shall allow associating exactly one resume with an application, and changing that association. | M |
| FR-35 | The system shall allow a user to clone a resume to create a new version, preserving lineage (`parent_resume_id`). | S |
| FR-36 | The system shall display, per resume, the applications using it and the outcome distribution of those applications. | S |
| FR-37 | The system shall present resume outcome comparisons with sample size and an explicit statement that differences are associations, not proven causes, and shall suppress comparison entirely below 5 applications per version. | S |

### 6.4 Interview Tracking

| ID | Requirement | Priority |
|---|---|---|
| FR-38 | The system shall allow a user to create an interview linked to one of their applications. | M |
| FR-39 | The system shall record interview date/time, round number, round type (OA, Phone Screen, Technical, System Design, Behavioural, HR, Final), duration, mode, and interviewer name/role. | M |
| FR-40 | The system shall support multiple interview rounds per application, ordered by round number. | M |
| FR-41 | The system shall allow recording questions asked during an interview, each with text, category, topic, difficulty, and how well the user felt they answered. | M |
| FR-42 | The system shall allow linking an interview question to an existing question-bank entry, or promoting a recorded question into the user's question bank. | S |
| FR-43 | The system shall allow recording structured post-interview feedback: overall self-rating (1-5), what went well, what went poorly, improvement areas, perceived difficulty, and outcome. | M |
| FR-44 | The system shall allow recording externally provided recruiter feedback separately from self-evaluation. | Out |
| FR-45 | The system shall provide a guided debrief flow completable in under 3 minutes. | S |
| FR-46 | The system shall automatically suggest an application status change when an interview outcome is recorded, requiring user confirmation. | C |
| FR-47 | The system shall derive the set of topics tested in an interview from its recorded questions. | M |

### 6.5 Interview Preparation

| ID | Requirement | Priority |
|---|---|---|
| FR-48 | The system shall provide a question bank containing both seeded system questions and user-created questions. | M |
| FR-49 | The system shall categorise questions by category (DSA, Technical, Behavioural, HR, System Design, Company-specific), topic, and difficulty (Easy/Medium/Hard). | M |
| FR-50 | The system shall allow a user to create, edit, and delete their own questions; system-seeded questions shall be read-only but forkable. | M |
| FR-51 | The system shall allow a user to record a practice attempt on a question, capturing practice status, self-confidence (1-5), time taken, and notes. | M |
| FR-52 | The system shall maintain a topic taxonomy with prerequisite relationships between topics. | S |
| FR-53 | The system shall compute a per-topic mastery score in [0,1] from practice attempts and interview outcomes, with exponential time decay. | S |
| FR-54 | The system shall allow a user to create preparation tasks with title, linked topic, due date, estimated effort, and completion state. | M |
| FR-55 | The system shall generate a suggested preparation plan from weak topics, which the user may accept, edit, or dismiss. | S |
| FR-56 | The system shall display preparation progress per topic and per category. | M |
| FR-57 | The system shall support a spaced-review queue surfacing topics whose decayed mastery has fallen below a threshold. | C |

### 6.6 Analytics

| ID | Requirement | Priority |
|---|---|---|
| FR-58 | The system shall display an application funnel: Applied → OA → Interview → Final Round → Offer, with counts at each stage. | M |
| FR-59 | The system shall compute stage-to-stage conversion rates using the definitions in §20.7, based on *ever-reached* stage attainment rather than current status. | M |
| FR-60 | The system shall display applications created over time (weekly buckets). | M |
| FR-61 | The system shall display interviews attended over time. | S |
| FR-62 | The system shall display the distribution of rejection stages. | S |
| FR-63 | The system shall display per-resume outcome rates subject to the minimum-sample rule in FR-37. | S |
| FR-64 | The system shall display per-topic interview performance derived from question-level self-ratings. | S |
| FR-65 | The system shall display preparation progress trends over time. | C |
| FR-66 | The system shall display median dwell time per stage from the user's own history. | S |
| FR-67 | The system shall display a sample-size indicator alongside every computed rate and shall mark any rate computed from fewer than 5 observations as low confidence. | M |
| FR-68 | The system shall allow filtering all analytics by date range and by tag. | C |
| FR-69 | The system shall compute all analytics exclusively from the requesting user's own records. | M |

### 6.7 Recommendations

| ID | Requirement | Priority |
|---|---|---|
| FR-70 | The system shall generate ranked, personalised recommendations for the authenticated user. | M |
| FR-71 | The system shall identify weak topics from interview question ratings, practice confidence, and decayed mastery. | M |
| FR-72 | The system shall perform prerequisite root-cause routing: when a weak topic has weak prerequisites, the recommendation shall target the prerequisite. | S |
| FR-73 | The system shall attach to every recommendation an evidence tier (A/B/C/D), the supporting observation count, and a human-readable explanation of why it was generated. | M |
| FR-74 | The system shall express all recommendations in associative, non-causal language, and shall be verified against a prohibited-phrasing list in automated tests. | M |
| FR-75 | The system shall allow a user to accept a recommendation (converting it to a preparation task), snooze it, or dismiss it with a reason. | S |
| FR-76 | The system shall detect stalled applications and recommend a follow-up action. | S |
| FR-77 | The system shall allow the user to toggle between local-only and local+federated recommendation ranking, and shall display the rank delta between the two. | S |
| FR-78 | The system shall not generate recommendations that reference or imply data from any other identifiable user. | M |

### 6.8 Federated Learning & Privacy Controls

| ID | Requirement | Priority |
|---|---|---|
| FR-79 | The system shall extract, on the client side, a fixed-schema numeric feature vector from a user's local data, containing no free text, no identifiers, and no company names. | M |
| FR-80 | The system shall train a model locally on a client's own data without transmitting training examples. | M |
| FR-81 | The system shall clip each local model update to a configured L2 norm before transmission. | M |
| FR-82 | The system shall add calibrated Gaussian noise to each local update before transmission (local differential privacy). | M |
| FR-83 | The FL server shall aggregate client updates using FedAvg and shall refuse to aggregate any round with fewer than *k* participating clients (default k=3). | M |
| FR-84 | The FL server shall reject or down-weight updates whose norm exceeds the clipping bound (poisoning defence). | S |
| FR-85 | The system shall version global models and record per-round metadata: round number, participant count, aggregation timestamp, noise multiplier, clipping norm, and evaluation metric. | M |
| FR-86 | The system shall distribute the current global model to clients on request. | M |
| FR-87 | The system shall require explicit, informed opt-in before a user's client participates in any federated round, defaulting to off. | M |
| FR-88 | The system shall allow a user to withdraw from federated participation at any time, taking effect from the next round. | M |
| FR-89 | The system shall maintain a per-user Privacy Ledger recording, for each round the user participated in, the feature schema version, pre- and post-clipping norms, noise multiplier, cohort size, epsilon consumed, and the transmitted vector. | S |
| FR-90 | The system shall display a cumulative privacy budget (epsilon) per user and shall halt that user's participation when the budget is exhausted. | C |
| FR-91 | The system shall allow the user to select a privacy posture (Strict / Balanced / Contributor) that configures noise and clipping parameters. | C |
| FR-92 | The FL server shall have no credentials granting read access to any user's application, resume, interview, or note data. | M |
| FR-93 | The system shall allow the user to export their Privacy Ledger as JSON. | C |

### 6.9 Cross-cutting

| ID | Requirement | Priority |
|---|---|---|
| FR-94 | Every data-access operation shall be scoped to the authenticated user's ID at the query layer, not merely filtered in application code. | M |
| FR-95 | The system shall return HTTP 404 (not 403) when a user requests a resource belonging to another user, to avoid confirming existence. | M |
| FR-96 | The system shall validate all request payloads against typed schemas and reject unknown fields. | M |
| FR-97 | The system shall provide a health endpoint reporting API, database, and FL-server reachability. | S |
| FR-98 | The system shall provide seed data for demonstration, clearly marked as synthetic. | S |

---

## 7. Non-Functional Requirements

### 7.1 Security

| ID | Requirement | Measure |
|---|---|---|
| NFR-01 | Passwords stored using Argon2id, memory cost ≥64 MiB, time cost ≥3, unique salt | Verified by unit test asserting hash prefix and parameters |
| NFR-02 | Access tokens expire ≤15 minutes; refresh tokens ≤7 days and are revocable server-side | Token TTL assertions in tests |
| NFR-03 | All ORM access uses parameterised queries; zero string-interpolated SQL | Static check in CI (grep gate) + code review |
| NFR-04 | All endpoints except register/login/health require a valid bearer token | Automated test enumerating the OpenAPI schema and asserting 401 on unauthenticated call |
| NFR-05 | Cross-user access attempts return 404 and are logged | Dedicated security test suite covering every owned resource type |
| NFR-06 | Uploaded files validated by magic bytes; stored with generated names outside static-served paths | Upload test with a renamed executable must fail |
| NFR-07 | Rate limiting on authentication endpoints: ≤10 attempts per IP per 15 min | Integration test |
| NFR-08 | Security headers set (CSP, X-Content-Type-Options, Referrer-Policy, HSTS in production) | Response header test |
| NFR-09 | No secret is committed to the repository; all secrets come from environment | CI secret-scan step |

### 7.2 Privacy

| ID | Requirement | Measure |
|---|---|---|
| NFR-10 | No raw user record, free text, identifier, or company name is transmitted to the FL server | Test asserting the serialised client payload matches an allow-listed numeric schema exactly |
| NFR-11 | FL server container holds no database credentials for user schemas | Verified by container environment inspection in CI |
| NFR-12 | Federated participation is opt-in, default off, revocable | Test asserting a new user's `fl_opt_in` is false and no update is produced |
| NFR-13 | Aggregation requires ≥3 participating clients | Unit test: a 2-client round must be refused |
| NFR-14 | Account deletion removes all user rows and files within 60 seconds and leaves no orphan records | Deletion integration test with orphan check across all tables |
| NFR-15 | Analytics and recommendations never reference another identifiable user | Code review + test on recommendation output |
| NFR-16 | Only the minimum necessary fields are collected; no field exists without a requirement referencing it | Data-dictionary review at each sprint review |

### 7.3 Performance

| ID | Requirement | Measure |
|---|---|---|
| NFR-17 | 95th-percentile API response ≤300 ms for list and detail endpoints with 500 applications, excluding analytics | Load test with seeded data |
| NFR-18 | Analytics endpoints respond ≤800 ms p95 for 500 applications and 200 interviews | Load test |
| NFR-19 | Frontend first contentful paint ≤2 s on a mid-range laptop over local network | Lighthouse run |
| NFR-20 | Application list is paginated at 25 rows and never loads the full table | Code review + response size assertion |
| NFR-21 | A federated round of 5 clients completes in ≤60 s in the demo configuration | Timed FL test run |

### 7.4 Scalability

| ID | Requirement | Measure |
|---|---|---|
| NFR-22 | Schema and queries support 10,000 applications per user without full-table scans; all FK and filter columns indexed | `EXPLAIN` review of the five hottest queries |
| NFR-23 | Backend is stateless apart from the database, allowing horizontal replication | Architecture review |
| NFR-24 | FL design supports adding clients without server code changes | Config-driven client count |

### 7.5 Availability & Reliability

| ID | Requirement | Measure |
|---|---|---|
| NFR-25 | Target 99% availability during the demonstration period | Uptime log |
| NFR-26 | All write operations are transactional; a failed multi-step operation leaves no partial state | Integration test with induced failure |
| NFR-27 | Database migrations are versioned and reversible | Flyway migrate and rollback test in CI |
| NFR-28 | An FL round failure never corrupts the current global model; the previous version remains servable | Test: kill a client mid-round, assert model integrity |
| NFR-29 | Daily automated `pg_dump` in the deployed environment with a documented restore procedure | Restore rehearsed once before final demo |

### 7.6 Usability

| ID | Requirement | Measure |
|---|---|---|
| NFR-30 | A new user can create their first application within 60 seconds of first login | Usability test with 5 participants |
| NFR-31 | Post-interview debrief completable in ≤3 minutes | Timed usability test |
| NFR-32 | Every list view has a designed empty state with a primary action | UI review checklist |
| NFR-33 | Every asynchronous action shows loading, success, and error states | UI review checklist |
| NFR-34 | Destructive actions require explicit confirmation naming the object being deleted | UI review checklist |
| NFR-35 | Error messages state what happened and what to do next; no raw stack traces or status codes reach the user | UI review |

### 7.7 Maintainability

| ID | Requirement | Measure |
|---|---|---|
| NFR-36 | Backend layered as controllers → services → repositories → entities, with no layer skipping | Import-boundary lint rule |
| NFR-37 | Backend line coverage ≥70%; services and analytics/recommendation modules ≥85% | JaCoCo coverage gate in CI |
| NFR-38 | fl-service public functions typed; frontend `strict: true` with no `any` in committed code | CI gate |
| NFR-39 | Linting and formatting enforced on every PR (ESLint on the frontend) | CI gate |
| NFR-40 | Every non-obvious design decision recorded as a short ADR in `docs/adr/` | Review |

### 7.8 Portability

| ID | Requirement | Measure |
|---|---|---|
| NFR-41 | Full stack starts with a single `docker compose up` on Linux, macOS, and Windows/WSL2 | Verified by each team member on their own machine |
| NFR-42 | No hard-coded absolute paths, hostnames, or ports outside configuration | Code review |
| NFR-43 | Frontend targets the last two major versions of Chrome, Firefox, Edge, and Safari | Manual matrix check |

### 7.9 Accessibility

| ID | Requirement | Measure |
|---|---|---|
| NFR-44 | Colour contrast meets WCAG 2.1 AA (4.5:1 body text) | Automated contrast check |
| NFR-45 | All interactive elements keyboard reachable with a visible focus ring; modals trap and restore focus | Keyboard-only walkthrough |
| NFR-46 | All form inputs have associated labels; errors linked via `aria-describedby` | axe-core scan with zero critical violations |
| NFR-47 | Status is never conveyed by colour alone; badges carry text | UI review |
| NFR-48 | Layout usable from 360 px to 1920 px width | Responsive check at 360/768/1280/1920 |

### 7.10 Testability

| ID | Requirement | Measure |
|---|---|---|
| NFR-49 | All external dependencies (DB, filesystem, FL server, clock) injectable and mockable | Architecture review |
| NFR-50 | Test suite runs fully offline and deterministically; all randomness seeded | CI run with network disabled |
| NFR-51 | A seeded fixture dataset produces identical analytics output on every run | Golden-file test |
| NFR-52 | Full backend suite completes in ≤3 minutes | CI timing |

---

## 8. Domain Requirements

Domain requirements are constraints that come from the problem domain itself: recruitment, interviewing, and federated learning. They are not requested features or implementation choices. They are facts about the domain that the system has to respect. A functional requirement says what the system does; a domain requirement says what the world is like.

### 8.1 Job applications & recruitment

| ID | Domain requirement | Rationale / consequence |
|---|---|---|
| DR-01 | An application progresses through discrete stages, but the progression is not strictly linear: rejection or withdrawal may occur at any stage, and stages may be skipped entirely. | The status model must be a state machine permitting terminal transitions from every non-terminal state, not an ordered pipeline with fixed steps. |
| DR-02 | The absence of a response is the most common outcome, and it is not an event: no employer sends a "we are ignoring you" signal. | "Ghosted" must be inferable from elapsed time rather than recorded, and any funnel that treats missing outcomes as failures will misreport. |
| DR-03 | A single application belongs to exactly one company and one role, but a user may hold several concurrent applications with the same company. | Company must be a separate normalised entity; uniqueness must not be enforced on (user, company). |
| DR-04 | Recruitment stages are not standardised across employers; one company's "OA" is another's "screening test", and round counts vary from 1 to 7. | The canonical stage set must be small and abstract, with a free-text round label carried alongside it. |
| DR-05 | Timelines are long and irregular, weeks between stages is normal, and outcomes can arrive months later. | Time-to-transition must be modelled as a distribution, and analytics must handle right-censored (still-open) applications rather than counting them as failures. |
| DR-06 | Outcomes are influenced by many factors outside the system's observation: referrals, headcount freezes, timing, luck. | The system must never attribute an outcome to a single tracked variable. This constraint flows directly into FR-74. |

### 8.2 Interviews

| ID | Domain requirement | Rationale / consequence |
|---|---|---|
| DR-07 | Interview content is recalled from memory and decays rapidly; recall is partial and often wrong on detail. | Debrief capture must be fast, allow partial records, and never require fields the user cannot reliably produce. |
| DR-08 | Self-assessment of interview performance is systematically biased and only weakly correlated with the actual result. | Self-rating is a legitimate input signal but must not be treated as ground truth; models must be described as predicting *self-reported* confidence, not success. |
| DR-09 | A single interview round typically tests multiple topics simultaneously, so credit assignment between topics is ambiguous. | Topic attribution must be at question level, not round level; round-level attribution would produce false weak-topic signals. |
| DR-10 | Interview questions are frequently under NDA or otherwise confidentiality-constrained. | Question text is private by default, never shared to any global surface, and never included in any federated payload. |
| DR-11 | Interviewer identity is personal data about a third party who has not consented to being in the system. | Interviewer fields are optional, stored as free text with no separate entity, excluded from all analytics and all model features, and purged on account deletion. |

### 8.3 Resumes

| ID | Domain requirement | Rationale / consequence |
|---|---|---|
| DR-12 | A resume is a versioned document tailored per role; a job seeker legitimately maintains several simultaneously. | Versioning with lineage is a domain necessity, not a convenience feature. |
| DR-13 | Resume files contain directly identifying personal data: full name, phone, address, sometimes date of birth. | Resume files are the highest-sensitivity artefact in the system: never publicly served, never parsed into shared storage, never used to derive federated features. |
| DR-14 | The number of applications per resume version is small (typically 5-40), giving very low statistical power for outcome comparison. | Resume performance comparison must display sample size and be suppressed below threshold (FR-37). Reporting "Resume B is 12% better" from n=6 would be misleading. |

### 8.4 Interview preparation

| ID | Domain requirement | Rationale / consequence |
|---|---|---|
| DR-15 | Technical topics have prerequisite structure: shortest-path algorithms cannot be learned before graph traversal. | The topic taxonomy must be a DAG with prerequisite edges, and recommendations must respect it (FR-72). |
| DR-16 | Skill decays with disuse; a topic practised four months ago is not currently ready. | Mastery must carry a time-decay term (FR-53). A static "completed" checkbox misrepresents the domain. |
| DR-17 | Preparation effort competes with academic and personal commitments; capacity is scarce and lumpy. | Recommendations must be few and ranked, with realistic effort estimates. A 40-item plan will be ignored. |

### 8.5 Privacy & Federated Learning

| ID | Domain requirement | Rationale / consequence |
|---|---|---|
| DR-18 | Job-search data is career-sensitive: disclosure that a user is interviewing elsewhere can cause real-world harm. Users know this and behave accordingly. | Privacy is a functional prerequisite for the product: a user who does not trust the system will not enter accurate data, which breaks the analytics. |
| DR-19 | Federated learning requires the learning task to be learnable from a fixed-schema numeric vector; anything requiring raw text or cross-user joins is out of scope for FL. | Task selection is constrained before design begins. This rules out, for example, federated learning over question text. |
| DR-20 | Federated data is non-IID: each user's distribution reflects their own field, seniority, and geography. | FedAvg convergence will be uneven; evaluation must report per-client as well as global metrics, and the plan must not assume clean convergence curves. |
| DR-21 | Model updates can leak information about training data (gradient inversion, membership inference); transmitting raw updates is not automatically private. | Clipping plus calibrated noise is mandatory, not optional. "We use FL therefore it is private" is a false claim and must not appear in the report or poster. |
| DR-22 | A federated cohort of fewer than a few clients provides effectively no anonymity: averaging two updates barely hides either. | Minimum cohort size *k* must be enforced server-side (FR-83). |
| DR-23 | Any client in an open federated system may be malicious and submit crafted updates. | Norm-based filtering is required (FR-84); the threat model must be stated plainly, including what this design does *not* defend against. |

---

## 9. User Stories

Format: As a [role], I want [goal], so that [benefit].
Roles used: *job seeker* (primary), *administrator*, *FL administrator*, *developer*.
Points use a modified Fibonacci scale (1, 2, 3, 5, 8, 13) where 1 point ≈ 2-3 focused hours for a student developer.

Acceptance criteria are given in full for Sprint 1 and Sprint 2 stories, and condensed (the highest-value checks) for later sprints; they are expanded during backlog refinement in the sprint before the story is pulled. The sprint shown is the sprint whose phases deliver the story (§10.1 lists the phases).

### EPIC 1: Authentication & User Management

US-01, Register an account · Priority: Must · Points: 5 · Sprint 1 · Deps: US-67, US-68
> As a job seeker, I want to register with my email and a password, so that I have a private account for my job-search data.

Acceptance criteria:
- Given valid email, password (≥10 chars), and display name, the account is created and I am redirected to the dashboard.
- The password is stored as an Argon2id hash; the plaintext appears in no database column, no log line, and no API response.
- Registering with an already-used email returns a generic failure message that does not confirm the address is registered.
- A password shorter than 10 characters or present in the common-password blocklist is rejected with an inline field error.
- Email format is validated on both client and server; the server is authoritative.
- A newly created user has `fl_opt_in = false`.

US-02, Log in · Must · 3 · Sprint 1 · Deps: US-01
> As a job seeker, I want to log in, so that I can reach my own data.

- Correct credentials return an access token (≤15 min) and a refresh token; I land on the dashboard.
- Incorrect credentials return 401 with a message that does not reveal whether the email exists.
- After 10 failed attempts from one IP within 15 minutes, further attempts are rate-limited.
- A valid session survives a page refresh.

US-03, Log out · Must · 2 · Sprint 1 · Deps: US-02
> As a job seeker, I want to log out, so that my data is not reachable from a shared machine.

- Logging out clears client-side credentials and revokes the refresh token server-side.
- The revoked refresh token cannot mint a new access token.
- Protected routes redirect to login after logout.

US-04, Session persistence and expiry · Must · 3 · Sprint 1 · Deps: US-02
> As a job seeker, I want my session refreshed silently while I work and ended when it truly expires, so that I am neither logged out mid-task nor left logged in indefinitely.

- An expired access token is transparently refreshed once, and the original request is retried.
- If the refresh token is expired or revoked, I am redirected to login with an explanatory message.
- Concurrent requests during a refresh do not trigger multiple refresh calls.

US-05, View and edit my profile · Must · 3 · Sprint 1 · Deps: US-02
> As a job seeker, I want to maintain my profile, so that the system can tailor defaults to my situation.

- I can view and update display name, target role, graduation year, and timezone.
- Changes persist and are reflected immediately in the header.
- I cannot change my email to one already in use.
- Validation errors appear inline against the offending field.

US-06, Change my password · Should · 3 · Sprint 5 · Deps: US-05
> As a job seeker, I want to change my password, so that I can respond to a suspected compromise.
- Requires current password; all other refresh tokens are revoked on success; new password re-validated against the policy.

US-07, Export all my data · Should · 3 · Sprint 5 · Deps: EPIC 2-5
> As a job seeker, I want to export everything I have entered as JSON, so that I am not locked in.
- Export includes applications, resumes (metadata + files), interviews, questions, tasks, and the privacy ledger; it contains no other user's data; it completes in ≤10 s for 500 applications.

US-08, Delete my account · Should · 5 · Sprint 5 · Deps: US-07
> As a job seeker, I want to permanently delete my account, so that I control my own footprint.
- Requires typed confirmation; removes every row referencing my ID plus uploaded files and FL client state; a post-deletion orphan-check query returns zero rows.

US-91, Onboarding and starter plan · Should · 5 · Sprint 6 · Deps: US-05, US-40
> As a new user, I want onboarding to ask my target role and date and give me a starter plan, so that I do not land on an empty dashboard.
- First login routes to onboarding, not the dashboard; target role and date are saved to the profile; starter preparation tasks are created and can be edited or removed.

US-92, Password reset and email verification · Should · 5 · Sprint 5 · Deps: US-02
> As a job seeker, I want to reset a forgotten password and verify my email, so that I can recover my account and prove the address is mine.
- Reset and verification tokens are single-use and expire; a reused or expired token is rejected; the reset request gives the same response whether or not the email exists.

US-93, Sign in with Google or GitHub · Could · 5 · Sprint 6 · Deps: US-02
> As a job seeker, I want to sign in with Google or GitHub, so that I do not need another password.
- The provider sign-in issues the app's own tokens; an existing account is linked only when the provider has verified the email; email-and-password sign-in keeps working unchanged.

---

### EPIC 2: Job Application Tracking

US-09, Create an application · Must · 5 · Sprint 1 · Deps: US-02, US-68
> As a job seeker, I want to record a job application, so that I stop losing track of where I applied.

- The form requires company, job title, status, and date applied; all other fields are optional.
- Typing a company name offers existing companies and creates a new one if none matches.
- On save I return to the list with the new row visible and a success toast shown.
- Server-side validation rejects a future `date_applied` and an unknown status.
- The created record is owned by me; the owner ID is set server-side from the token and can never be supplied by the client.

US-10, List my applications · Must · 5 · Sprint 1 · Deps: US-09
> As a job seeker, I want a table of all my applications, so that I can see my whole campaign at once.

- Columns: company, role, status badge, stage, days in stage, date applied, priority.
- Paginated at 25 rows with page-size options; total count displayed.
- Sortable by date applied, company, status, and days in stage.
- The table shows only my applications; another user's records are never returned by the endpoint.
- An empty state with a "Create your first application" action appears when I have none.
- Loading and error states are distinct from the empty state.

US-11, View application detail · Must · 3 · Sprint 1 · Deps: US-09
> As a job seeker, I want a detail view for an application, so that I can see everything about it in one place.

- Shows all stored fields, the linked resume, the status timeline, and related interviews.
- Requesting an application ID belonging to another user returns 404.
- A non-existent ID returns 404 with a friendly not-found screen.

US-12, Edit an application · Must · 3 · Sprint 1 · Deps: US-11
> As a job seeker, I want to edit an application, so that I can correct and enrich it as I learn more.
- Every field is editable; `updated_at` changes; unchanged fields are untouched; concurrent edit does not silently discard data (last-write-wins is acceptable but must be documented).

US-13, Delete an application · Must · 3 · Sprint 1 · Deps: US-11
> As a job seeker, I want to delete an application, so that mistakes and duplicates do not pollute my analytics.
- A confirmation dialog names the company and role; deletion cascades to interviews, questions, and status history; the row disappears without a full page reload; deleting another user's record is impossible (404).

US-14, Change status and record history · Must · 5 · Sprint 1 · Deps: US-11
> As a job seeker, I want to update an application's status with each change recorded, so that I keep an accurate history rather than only a current state.

- Status can be changed from the list row and the detail view.
- Every change writes an immutable `application_status_history` row: previous status, new status, timestamp, optional note.
- History rows cannot be edited or deleted independently of the application.
- The state machine rejects invalid transitions with a clear message.
- Changing to a terminal status prompts for an optional reason.

US-15, See an application timeline · Must · 3 · Sprint 1 · Deps: US-14
> As a job seeker, I want a chronological timeline on the application detail page, so that I can see how it progressed.
- Newest-first vertical timeline combining status changes and interviews; each entry shows relative and absolute time; empty state when only the creation event exists.

US-16, Search applications · Must · 3 · Sprints 2, 6 · Deps: US-10
> As a job seeker, I want to search by company or role, so that I can find one application among hundreds.
- Case-insensitive substring match across company name and job title; debounced at 300 ms; result count shown; a no-results state distinct from the empty state.

US-17, Filter and sort applications · Must · 5 · Sprints 2, 6 · Deps: US-10
> As a job seeker, I want to combine filters, so that I can narrow to exactly the slice I care about.
- Filters on status, stage, priority, tag, work mode, and date range, combinable with AND semantics; active filters shown as removable chips; filter state encoded in the URL so a view can be bookmarked and shared with myself; "Clear all" resets.

US-18, Days-in-stage and stall flag · Should · 5 · Sprint 3 · Deps: US-14
> As a job seeker, I want stalled applications flagged, so that I follow up before an opportunity dies quietly.
- Days-in-stage computed from the latest history entry; an application is flagged when it exceeds my own median dwell time for that stage (min 5 observations) or a documented default; the flag explains its own threshold and sample size on hover.

US-89, Kanban board · Should · 5 · Sprint 6 · Deps: US-14
> As a job seeker, I want to manage applications on a board, so that I can move them through stages at a glance.
- Dragging between columns calls the status endpoint; an illegal move is refused by the state machine and snaps back with the reason; a backward move prompts for a correction note; every accepted move writes history.

US-95, Contacts and referrals · Should · 5 · Sprint 7 · Deps: US-09
> As a job seeker, I want to track contacts and referrals per company, so that networking is part of my campaign rather than a separate list.
- Contacts belong to me and link to companies and applications; referral status is recorded; a contact with no interaction for a configurable period raises a nudge; another user's contacts return 404.

US-96, Company pages · Should · 3 · Sprint 7 · Deps: US-95, US-27
> As a job seeker, I want a page per company, so that everything I know about an employer is in one place.
- Shows all my applications, interviews, recorded questions, contacts and notes for that company; private notes are editable; only my own records appear.

US-99, Global search · Should · 5 · Sprint 7 · Deps: US-95, US-96
> As a job seeker, I want one search box across everything, so that I can find any record quickly.
- One query searches applications, companies, contacts, questions and notes; results open from the command palette; results never include another user's data.

US-100, Offer comparison · Should · 3 · Sprint 7 · Deps: US-09
> As a job seeker, I want to compare offers side by side with deadline countdowns, so that I decide well and on time.
- Offers record compensation and terms; two or more offers render side by side; each shows days remaining to its deadline.

US-101, Weekly goals and activity heatmap · Should · 3 · Sprint 7 · Deps: US-09, US-37
> As a job seeker, I want weekly goals and an activity heatmap, so that I keep a steady cadence.
- Weekly targets for applications and practice with progress; a calendar heatmap of activity; the current streak appears on the dashboard.

---

### EPIC 3: Resume Management

US-20, Create a resume record · Must · 3 · Sprint 2 · Deps: US-02
> As a job seeker, I want to register a resume version, so that I can distinguish the variants I send out.
- Name, version label, target role, and summary captured; one resume can be marked default; versions belong only to me.

US-21, Upload a resume file · Must · 5 · Sprint 2 · Deps: US-20
> As a job seeker, I want to attach the actual PDF, so that I know exactly which document I sent.

- Accepts PDF and DOCX up to 5 MB.
- Validation checks extension, declared MIME type, and magic bytes; a `.exe` renamed to `.pdf` is rejected.
- Files are stored outside any statically served directory under a generated UUID filename; the original name is stored only as metadata.
- Upload shows progress, and failure shows a specific, actionable error.

US-22, Download my resume file · Must · 3 · Sprint 2 · Deps: US-21
> As a job seeker, I want to download a stored resume, so that I can reuse it.
- Download requires authentication; the URL is not guessable and expires; requesting another user's file returns 404; the correct `Content-Disposition` and content type are set.

US-23, Structured resume content · Should · 5 · Sprint 7 · Deps: US-20
> As a job seeker, I want to record skills, experience, education, and projects, so that the system has structure to reason about, not just a binary blob.
- Repeatable sections with add/remove/reorder; skills are tags; content saves independently of the file.

US-24, Attach a resume to an application · Must · 3 · Sprint 2 · Deps: US-20, US-09
> As a job seeker, I want to record which resume I used for an application, so that I can later compare outcomes.
- Selectable from application create and edit forms; exactly one resume per application; the association is visible from both sides; changing it is allowed and logged in `updated_at`.

US-25, Clone a resume into a new version · Should · 3 · Sprint 2 · Deps: US-23
> As a job seeker, I want to duplicate a resume as a new version, so that I can tailor it without starting over.
- Clone copies structured content, not the file; `parent_resume_id` records lineage; version label auto-increments.

US-26, Compare resume outcomes · Should · 5 · Sprint 8 · Deps: US-24, US-43
> As a job seeker, I want to see how applications using each resume turned out, so that I can form a hypothesis about which is working.
- Per-resume counts and rates with explicit sample size; comparison is suppressed below 5 applications per version with an explanatory message; the panel carries a standing note that observed differences are associations and may reflect role, timing, or company mix.

---

### EPIC 4: Interview Tracking

US-27, Schedule/record an interview · Must · 5 · Sprint 2 · Deps: US-09
> As a job seeker, I want to record an interview against an application, so that my rounds are tracked in context.
- Date/time, round number, round type, duration, mode, and interviewer are captured; the interview must belong to one of my own applications (server-enforced); round numbers within an application are unique.

US-28, List and view interviews · Must · 3 · Sprint 2 · Deps: US-27
> As a job seeker, I want to see all my interviews, upcoming and past, so that I can prepare and review.
- Split into upcoming and completed; each row links to its application; empty and loading states present; only my interviews are returned.

US-29, Record questions asked · Must · 5 · Sprint 2 · Deps: US-27
> As a job seeker, I want to log the questions I was asked, so that I capture the most valuable output of the interview before I forget it.
- Multiple questions per interview, each with text, category, topic, difficulty, and a self-rating of how well I answered; questions can be added inline without leaving the page; question text is stored privately and never appears in any shared or federated surface.

US-30, Post-interview debrief · Must · 5 · Sprint 2 · Deps: US-27
> As a job seeker, I want a guided debrief, so that I capture a structured reflection quickly rather than writing nothing.
- Captures overall self-rating (1-5), what went well, what went poorly, improvement areas, perceived difficulty, and outcome; partial completion is saved as a draft; completion in ≤3 minutes verified in usability testing; a prompt appears on the dashboard for interviews whose date has passed without a debrief.

US-31, Edit and delete an interview · Must · 3 · Sprint 2 · Deps: US-27
> As a job seeker, I want to correct interview records, so that my history is accurate.
- All fields editable; deletion confirmed and cascades to its questions and feedback; cross-user access returns 404.

US-33, Interview outcome suggests a status change · Could · 3 · Sprint 8 · Deps: US-30, US-14
> As a job seeker, I want the app to suggest updating the application status after I record an outcome, so that I do not have to remember to do it twice.
- Suggestion is a prompt, never automatic; accepting writes a normal history entry; dismissing is remembered for that interview.

US-34, Derive tested topics from an interview · Must · 3 · Sprint 8 · Deps: US-29
> As a job seeker, I want the topics I was tested on inferred from my recorded questions, so that preparation can target them without extra data entry.
- Topic set is the distinct topics of the interview's questions; displayed on the interview detail; feeds the mastery calculation.

US-110, STAR story bank · Should · 3 · Sprint 8 · Deps: US-102
> As a job seeker, I want a bank of STAR stories that AI helps me structure, so that behavioural answers are ready before the interview.
- Create, edit and delete stories with competency tags; AI suggests a Situation-Task-Action-Result structure that I accept or edit; stories are private to me.

---

### EPIC 5: Interview Preparation & Topic Mastery

US-35, Browse the question bank · Must · 5 · Sprint 3 · Deps: US-02
> As a job seeker, I want a searchable bank of practice questions, so that I have a starting point for preparation.
- Seeded with ≥150 questions across all categories; filter by category, topic, and difficulty; my own questions and seeded ones are visually distinguished.

US-36, Add my own questions · Must · 3 · Sprint 3 · Deps: US-35
> As a job seeker, I want to add questions I encountered, so that my bank reflects my real interviews.
- Create/edit/delete my own; seeded questions are read-only but can be forked into a personal copy; questions recorded in an interview can be promoted into the bank in one click.

US-37, Record a practice attempt · Must · 5 · Sprint 3 · Deps: US-35
> As a job seeker, I want to log practising a question, so that my progress is measured rather than remembered.
- Captures status (not started / in progress / confident / needs review), confidence 1-5, time taken, and notes; repeated attempts are appended, not overwritten, so a confidence trajectory exists.

US-38, Topic taxonomy with prerequisites · Should · 5 · Sprint 3 · Deps: none (data work)
> As a developer, I want a topic graph with prerequisite edges, so that recommendations can identify root causes rather than symptoms.
- ~40-60 topics across categories, seeded and versioned; edges form a DAG with a cycle-detection test; every question links to exactly one topic.

US-39, Topic mastery score with decay · Should · 8 · Sprint 3 · Deps: US-37, US-38, US-34
> As a job seeker, I want a mastery score per topic that fades if I stop practising, so that my readiness reflects reality rather than a checkbox I ticked in March.
- Score in [0,1] combining practice confidence and interview question ratings, with exponential time decay; the formula is documented, unit-tested against fixed fixtures, and explained in the UI on hover.

US-40, Create preparation tasks · Must · 3 · Sprint 3 · Deps: US-38
> As a job seeker, I want a task list for preparation, so that intentions become scheduled work.
- Title, linked topic, due date, estimated effort, completion state; overdue tasks are visually marked; completion updates progress.

US-41, Preparation progress view · Must · 5 · Sprint 3 · Deps: US-37, US-40
> As a job seeker, I want to see progress by topic and category, so that I know where I stand.
- Progress bars per category; a topic grid coloured by mastery with an accessible non-colour indicator; drill-down from category to topic to questions.

US-42, Spaced review queue · Could · 5 · Sprint 8 · Deps: US-39
> As a job seeker, I want topics resurfaced as my mastery decays, so that I retain what I learned.
- Queue ordered by decayed mastery and interview relevance; capped at 5 items to stay actionable; items can be deferred.

US-90, Question bank browser and practice logging · Must · 5 · Sprints 3, 6 · Deps: US-35, US-37
> As a job seeker, I want to browse the question bank and log practice from the UI, so that my mastery actually changes as I work.
- Every topic has real, original interview questions; I can filter and drill from topic to question; logging an attempt updates mastery visibly.

US-106, Question recommendations · Should · 5 · Sprint 8 · Deps: US-39, US-102
> As a job seeker, I want "solve next", daily and interview-driven question picks, each with a reason, so that practice targets what matters.
- Picks use local embeddings and my mastery; each pick states its reason; ranking is deterministic on fixed fixtures.

US-107, Job readiness score · Should · 5 · Sprint 8 · Deps: US-39
> As a job seeker, I want each application to show how ready I am for that job, so that I prepare for the role in front of me.
- The job's skills map to topics weighted by my mastery; the score shows its sample size and is suppressed when evidence is thin.

---

### EPIC 6: Analytics & Pipeline Health

US-43, Application funnel · Must · 5 · Sprint 3 · Deps: US-14
> As a job seeker, I want a funnel from applications through to offers, so that I can see where my campaign leaks.
- Stages Applied → OA → Interview → Final → Offer with counts based on *stages ever reached* (from history), not current status; open applications are shown as still in flight rather than counted as failures; each stage links to its filtered application list.

US-44, Conversion rates · Must · 5 · Sprint 3 · Deps: US-43
> As a job seeker, I want stage-to-stage conversion rates, so that I know which step to work on.
- All five rates computed per the definitions in §20.7; every rate displays its denominator; rates from fewer than 5 observations are marked low confidence and visually de-emphasised.

US-45, Activity over time · Must · 3 · Sprint 3 · Deps: US-09
> As a job seeker, I want applications and interviews plotted over time, so that I can see my own consistency.
- Weekly buckets, applications and interviews as separate series; date-range selector; empty state for users with no history.

US-46, Rejection stage distribution · Should · 3 · Sprint 3 · Deps: US-14
> As a job seeker, I want to see at which stage I am rejected, so that I can target the right skill.
- Distribution of the last stage reached before rejection; separates "rejected" from "no response" and never conflates them.

US-47, Topic performance · Should · 5 · Sprint 3 · Deps: US-29, US-38
> As a job seeker, I want per-topic interview performance, so that I can see which topics actually hurt me in real rounds.
- Mean self-rating per topic weighted by recency, with question counts; topics with fewer than 3 questions are grouped as "insufficient data" rather than shown as extremes.

US-48, Stage dwell times · Should · 3 · Sprint 3 · Deps: US-18
> As a job seeker, I want median time spent at each stage, so that I know what "normal" looks like for me.
- Median and interquartile range per stage from completed transitions only; right-censored applications excluded from the median and reported separately.

US-49, Dashboard pipeline strip and action queue · Must · 8 · Sprint 3 · Deps: US-43, US-18
> As a job seeker, I want the dashboard to tell me what to do today, so that the app is a tool rather than a report.
- Top: compact pipeline strip with live counts. Below: a ranked action queue mixing stalled applications, missing debriefs, upcoming interviews, and due prep tasks; every item is one click from its action; the queue is capped at 7 items.

US-50, Sample sizes shown across analytics · Must · 3 · Sprint 3 · Deps: US-44
> As a job seeker, I want to know how much data each number is based on, so that I do not over-read noise.
- Every rate, average, and median displays n; a shared low-confidence badge component is used consistently; a golden-file test pins outputs for a fixed fixture dataset.

US-80, Export my applications and analytics · Should · 5 · Sprint 5 · Deps: EPIC 2, EPIC 6
> As a job seeker, I want to export my application list and summary analytics as a PDF or CSV, so that I can share my progress outside the app or keep an offline record.
- Full definition and FR-102 to FR-104 in `ARCHITECTURE_REVISION_JAVA.md` §6.2; downloads are owner-only, authenticated and expiring.

US-114, Resume performance and funnel simulator · Could · 5 · Sprint 8 · Deps: US-26, US-44
> As a job seeker, I want to see which resume version performs best and simulate how many applications I need, so that I plan volume realistically.
- Response rate per resume version, sample-size gated; the simulator uses my own conversion rates and states its assumptions.

---

### EPIC 7: Explainable Recommendations

US-51, Weak-topic recommendations · Must · 8 · Sprint 4 · Deps: US-39, US-47
> As a job seeker, I want to be told which topics to work on, so that preparation is targeted instead of generic.
- Ranked list of at most 5; each cites the observations behind it; topics with no supporting evidence are never recommended.

US-52, Root-cause prerequisite routing · Should · 5 · Sprint 4 · Deps: US-51, US-38
> As a job seeker, I want to be pointed at the underlying gap rather than the symptom, so that I fix the cause.
- When a weak topic has a prerequisite with lower mastery, the recommendation targets the prerequisite and explains the chain; routing depth is capped at 2 to stay comprehensible.

US-53, Evidence tiers and explanations · Must · 5 · Sprint 4 · Deps: US-51
> As a job seeker, I want to know where each recommendation came from and how confident it is, so that I can judge it myself.
- Every recommendation carries tier A/B/C/D, an observation count, and a plain-language "why am I seeing this?" expansion; a unit test asserts no recommendation renders without an explanation.

US-54, Non-causal language enforcement · Must · 3 · Sprint 4 · Deps: US-53
> As a job seeker, I want the app to be clear that correlation is not causation, so that I am not misled about my own chances.
- All recommendation copy lives in a template registry; an automated test fails the build if any template matches the prohibited-phrasing list; associative phrasing is verified by review.

US-55, Act on a recommendation · Should · 3 · Sprints 4, 6 · Deps: US-51, US-40
> As a job seeker, I want to convert a recommendation into a task, snooze it, or dismiss it, so that the list stays relevant.
- Accept creates a linked preparation task; snooze hides it for 7 days; dismiss records a reason and suppresses that recommendation type for 30 days.

US-56, Stalled-application follow-up recommendations · Should · 3 · Sprint 4 · Deps: US-18
> As a job seeker, I want to be nudged to follow up on stalled applications, so that opportunities do not lapse silently.
- Generated from the stall flag; includes the specific application and the dwell time; capped at 3 concurrently.

---

### EPIC 8: Federated Learning & Privacy Ledger

US-57, Opt in to federated learning · Must · 5 · Sprint 4 · Deps: US-05
> As a job seeker, I want to explicitly choose whether to contribute to the shared model, so that participation is my decision.
- Default off for every new user; the opt-in screen states in plain language what is sent and what is not, showing the actual feature schema; a test asserts that an opted-out client produces no update.

US-58, Local feature extraction · Must · 8 · Sprint 4 · Deps: US-39, US-47
> As an FL administrator, I want a fixed numeric feature schema derived entirely on the client, so that no raw data is ever a candidate for transmission.
- Fixed-length, versioned, all-numeric vector; a serialisation test asserts the payload matches the allow-listed schema exactly and contains no strings; extraction runs against the client's own store only.

US-59, Local model training · Must · 8 · Sprint 4 · Deps: US-58
> As an FL administrator, I want each client to train on its own data, so that learning happens without data movement.
- scikit-learn model trained in the client process; training examples never leave; per-client metrics logged locally; deterministic under a fixed seed.

US-60, Clipping and noise before transmission · Must · 5 · Sprint 4 · Deps: US-59
> As a job seeker, I want my model update masked before it leaves my client, so that it cannot be reverse-engineered into my data.
- Update is L2-clipped to a configured norm, then Gaussian noise is added; pre- and post-values recorded in the ledger; a test asserts the transmitted vector differs from the raw update and respects the norm bound.

US-61, Federated aggregation with minimum cohort · Must · 8 · Sprint 4 · Deps: US-60
> As an FL administrator, I want FedAvg aggregation that refuses small cohorts, so that averaging provides real anonymity.
- Flower server aggregates ≥k clients (default 3) and refuses below k with a logged reason; round metadata is persisted; the server holds no user-schema database credentials, verified in CI.

US-62, Global model distribution · Must · 5 · Sprint 4 · Deps: US-61
> As a job seeker, I want my client to use the improved shared model, so that I benefit from collective experience.
- Clients fetch the current global model version; the version in use is displayed in the UI; a failed round leaves the previous model servable.

US-63, Privacy Ledger · Should · 8 · Sprint 4 · Deps: US-60
> As a job seeker, I want a log of everything my client has ever sent, so that I can verify the privacy claim instead of trusting it.
- One entry per round: round ID, timestamp, schema version, pre/post-clip norm, noise multiplier, cohort size, epsilon consumed, and the exact transmitted vector, expandable; a "what was not sent" list is shown; entries are immutable.

US-64, Withdraw from federation · Must · 3 · Sprint 4 · Deps: US-57
> As a job seeker, I want to stop contributing at any time, so that consent is reversible.
- Takes effect from the next round; historical contributions are explained as already aggregated and non-retractable; ledger remains visible after withdrawal.

US-65, Federated Lift viewer · Should · 5 · Sprint 4 · Deps: US-62, US-51
> As a job seeker, I want to compare my recommendations with and without the shared model, so that I can see what federation actually contributes.
- A toggle re-ranks recommendations live; changed items are highlighted with their rank delta; the comparison works offline against a cached global model.

US-66, Privacy budget meter · Could · 5 · Sprint 8 · Deps: US-63
> As a job seeker, I want to see my cumulative privacy budget, so that I understand the cost of participating.
- Displays total and spent epsilon and remaining rounds; participation halts at exhaustion with a clear explanation; posture selection (Strict/Balanced/Contributor) adjusts parameters and shows the trade-off.

US-84, Poisoning filter · Should · 3 · Sprint 4 · Deps: US-61
> As an FL administrator, I want over-norm updates rejected before aggregation, so that one malicious client cannot steer the shared model.
- Updates whose norm exceeds the configured bound are discarded and logged; a crafted over-norm update in a test is excluded from the average.

US-115, Cohort benchmarks · Should · 5 · Sprint 8 · Deps: US-62
> As a job seeker, I want benchmarks learned from the whole cohort, so that I can compare my preparation without anyone's data being shared.
- Benchmarks come from the global model only; they are hidden below the minimum cohort size; no individual's data is shown or inferable.

---

### EPIC 9: Platform, Quality & DevOps

Infrastructure, CI, seeding, and security hardening take real capacity. If they are not in the backlog, velocity is overstated and they get skipped under deadline pressure. As stories they also give every team member reviewable work in Sprint 1.

US-67, Repository and toolchain bootstrap · Must · 5 · Sprint 1
> As a developer, I want a configured repository, so that the team can work in parallel without conflicts.
- Monorepo structure, `.gitignore`, `.env.example`, licence, README skeleton, branch protection on `main`, PR template, commit convention documented; every member can clone and run.

US-68, Docker Compose development environment · Must · 5 · Sprint 1
> As a developer, I want one-command startup, so that "works on my machine" never happens.
- `docker compose up` starts Postgres, backend, and frontend with hot reload; verified independently by every team member on their own OS; documented in the README.

US-69, Database schema and migrations · Must · 8 · Sprint 1
> As a developer, I want versioned migrations, so that schema changes are reproducible and reversible.
- Alembic configured; Sprint 1 tables created by migration, not by `create_all`; up and down both tested in CI.

US-70, CI pipeline · Must · 5 · Sprint 1
> As a developer, I want automated checks on every PR, so that quality is enforced rather than hoped for.
- GitHub Actions runs lint, type-check, backend tests with coverage, and frontend tests; a failing check blocks merge; the run completes in under 5 minutes.

US-71, Application shell and navigation · Must · 5 · Sprint 1
> As a job seeker, I want consistent navigation and layout, so that the product feels coherent.
- Sidebar with the nine sections, responsive collapse below 768 px, active-route highlighting, protected route wrapper, 404 page, global error boundary, toast system.

US-72, Seed and demo data · Should · 3 · Sprints 2, 9
> As a developer, I want realistic seed data, so that features can be demonstrated and analytics tested without hand-entry.
- One command seeds a demo user with ~60 applications, 20 interviews, 3 resumes, and practice history; data is deterministic under a fixed seed and clearly marked synthetic.

US-73, Security test suite · Must · 5 · Sprints 1, 9 · Deps: EPIC 1-5
> As a developer, I want automated cross-user access tests, so that isolation is proven, not assumed.
- For every owned resource type, a test asserts that user B receives 404 for user A's object; unauthenticated access to every non-public endpoint returns 401; the suite is enumerated from the OpenAPI schema so new endpoints cannot be forgotten.

US-74, Accessibility and responsive pass · Should · 5 · Sprint 9
> As a job seeker with accessibility needs, I want the app to be keyboard and screen-reader usable, so that I can actually use it.
- axe-core reports zero critical violations on the main pages; full keyboard walkthrough passes; layouts verified at 360/768/1280/1920 px.

---

### EPIC 10: Notifications

US-76 to US-79 (stalled application, upcoming interview, missing debrief, notification bell) are defined with FR-99 to FR-101 and NFR-53 in `ARCHITECTURE_REVISION_JAVA.md` §6.1. All four are Should, Sprint 5.

---

### EPIC 11: Experience and First Run

US-85, Consistent professional interface · Should · 8 · Sprint 6 · Deps: US-71
> As a user, I want a consistent, professional interface in light or dark mode on any screen size, so that the product is pleasant and trustworthy to use daily.
- Design tokens for colour, type and spacing; system-default theme with a toggle; every page follows one visual standard; both themes pass contrast checks.

US-86, App shell: user menu, logout, command palette · Must · 3 · Sprint 6 · Deps: US-71
> As a user, I want to see who I am signed in as, log out, and jump anywhere with a command palette, so that navigation is fast.
- User menu with logout that clears the session; page titles and breadcrumbs; the command palette opens any page.

US-87, Feedback and validation · Must · 3 · Sprint 6 · Deps: US-71
> As a user, I want every action to give clear feedback and forms to tell me what is wrong before I submit, so that I never guess what happened.
- Shared toasts; one formatter for every enum value (no raw enum strings); client-side validation blocks invalid submits; consistent empty, loading and error states.

US-88, Responsive tables, charts and privacy views · Must · 5 · Sprint 6 · Deps: US-85
> As a user, I want tables, charts and the privacy views to be readable on my phone, so that I can use the app anywhere.
- Card rows on narrow screens; responsive charts with legends; the vector inspector works on mobile; verified at 390 px and 1440 px.

US-116, Sample data and guided tour · Could · 3 · Sprint 9 · Deps: US-72
> As a new user, I want to load realistic sample data and take a short guided tour, so that I understand the product before entering my own data.
- Sample data is clearly marked, touches only my records and is removable without orphans; the tour is skippable and replayable from help.

US-117, Designed error and edge states · Should · 3 · Sprint 9 · Deps: US-87
> As a user, I want errors, missing pages, offline state and expired sessions to show clear, helpful screens, so that I always know what to do next.
- Full-page and inline states with a next action and retry; an offline banner; a graceful session-expiry flow; both themes covered.

---

### EPIC 12: AI Assistance

AI runs on a local model by default, so user data never leaves the server; a hosted provider is optional and disclosed whenever active. Every prompt is assembled only from the requesting user's own data.

US-102, AI provider · Must · 5 · Sprint 8
> As an operator, I want AI to run on a local model by default, with an optional hosted provider behind one interface, so that privacy holds and providers are swappable.
- Provider interface with local and hosted implementations; timeouts and a graceful "AI unavailable" state; per-user rate limits; usage log without prompt content.

US-103, Cohort Coach · Should · 5 · Sprint 8 · Deps: US-102
> As a job seeker, I want to chat with an AI coach that answers from my own data, so that advice is specific to my campaign.
- Streaming replies grounded in my records only; the causal-language linter applies to replies; the coach never sees another user's data.

US-104, Job posting parser · Should · 3 · Sprint 8 · Deps: US-102, US-09
> As a job seeker, I want pasting a job posting to create a filled-in application, so that data entry takes seconds.
- Structured extraction with validation; I confirm before saving; malformed model output is rejected and never saved.

US-105, Debrief notes to questions · Should · 5 · Sprint 8 · Deps: US-102, US-29
> As a job seeker, I want AI to turn my rough debrief notes into tagged interview questions, so that they feed my preparation.
- Extracted questions map to topics and can be promoted to the bank; topics tested are derived for the interview (US-34); a status change is suggested, never applied automatically (US-33).

US-108, Resume and job description match · Should · 5 · Sprint 8 · Deps: US-102, US-23
> As a job seeker, I want to check my resume against a job description, so that I can tailor it.
- Gaps and tailoring suggestions per section; a tailored cover-letter draft; nothing changes until I accept it.

US-109, Mock interview · Should · 5 · Sprint 8 · Deps: US-102, US-39
> As a job seeker, I want an AI mock interview that grades my answers, so that practice feels like the real thing.
- Questions drawn from my weak topics; graded answers are recorded as practice attempts on the right topic.

US-111, Interview prep pack · Should · 3 · Sprint 8 · Deps: US-102, US-27
> As a job seeker, I want a one-page prep pack before each interview, so that I walk in prepared.
- One brief per upcoming interview, assembled only from my own data: company history, likely topics, recommended questions and stories.

US-112, Follow-up and negotiation drafts · Could · 3 · Sprint 8 · Deps: US-102
> As a job seeker, I want AI to draft follow-up and negotiation emails, so that I respond promptly and well.
- Drafts only, never sent automatically; drafts use the application and offer context.

US-113, Weekly review · Could · 3 · Sprint 8 · Deps: US-102, US-43
> As a job seeker, I want an AI-written weekly review of my search, so that I reflect on progress without effort.
- Generated from my analytics; delivered through the notification channel; numbers match the analytics exactly.

---

## 10. Product Backlog

### 10.1 Master backlog

Stories are grouped by epic. Sprint is the sprint whose phases deliver the story; Phases are the build phases in `BUILD_PHASES_JAVA.md`.

EPIC 1, Authentication & User Management

| ID | User story | Priority | Points | Sprint | Phases |
|---|---|---|---:|---|---|
| US-01 | Register an account | Must | 5 | 1 | P04, P05, P08 |
| US-02 | Log in | Must | 3 | 1 | P05, P08 |
| US-03 | Log out | Must | 2 | 1 | P05, P08 |
| US-04 | Session persistence and expiry | Must | 3 | 1 | P05, P08 |
| US-05 | View and edit my profile | Must | 3 | 1 | P06 |
| US-06 | Change my password | Should | 3 | 5 | P66 |
| US-07 | Export all my data | Should | 3 | 5 | P66 |
| US-08 | Delete my account | Should | 5 | 5 | P66 |
| US-91 | Onboarding and starter plan | Should | 5 | 6 | P76 |
| US-92 | Password reset and email verification | Should | 5 | 5 | P68 |
| US-93 | Sign in with Google or GitHub | Could | 5 | 6 | P78 |

EPIC 2, Job Application Tracking

| ID | User story | Priority | Points | Sprint | Phases |
|---|---|---|---:|---|---|
| US-09 | Create an application | Must | 5 | 1 | P09, P11, P12 |
| US-10 | List my applications | Must | 5 | 1 | P11, P12 |
| US-11 | View application detail | Must | 3 | 1 | P11, P12 |
| US-12 | Edit an application | Must | 3 | 1 | P11, P12 |
| US-13 | Delete an application | Must | 3 | 1 | P11, P12 |
| US-14 | Change status and record history | Must | 5 | 1 | P10, P11, P12 |
| US-15 | See an application timeline | Must | 3 | 1 | P11, P12 |
| US-16 | Search applications | Must | 3 | 2, 6 | P16, P74 |
| US-17 | Filter and sort applications | Must | 5 | 2, 6 | P16, P74 |
| US-18 | Days-in-stage and stall flag | Should | 5 | 3 | P37 |
| US-89 | Kanban board | Should | 5 | 6 | P73 |
| US-95 | Contacts and referrals | Should | 5 | 7 | P82, P83 |
| US-96 | Company pages | Should | 3 | 7 | P82, P84 |
| US-99 | Global search | Should | 5 | 7 | P87 |
| US-100 | Offer comparison | Should | 3 | 7 | P82, P85 |
| US-101 | Weekly goals and activity heatmap | Should | 3 | 7 | P82, P86 |

EPIC 3, Resume Management

| ID | User story | Priority | Points | Sprint | Phases |
|---|---|---|---:|---|---|
| US-20 | Create a resume record | Must | 3 | 2 | P17, P20, P22 |
| US-21 | Upload a resume file | Must | 5 | 2 | P18, P19, P20, P22 |
| US-22 | Download my resume file | Must | 3 | 2 | P20, P22 |
| US-23 | Structured resume content | Should | 5 | 7 | P88 |
| US-24 | Attach a resume to an application | Must | 3 | 2 | P21, P22 |
| US-25 | Clone a resume into a new version | Should | 3 | 2 | P21, P22 |
| US-26 | Compare resume outcomes | Should | 5 | 8 | P101 |

EPIC 4, Interview Tracking

| ID | User story | Priority | Points | Sprint | Phases |
|---|---|---|---:|---|---|
| US-27 | Schedule/record an interview | Must | 5 | 2 | P23, P24, P25 |
| US-28 | List and view interviews | Must | 3 | 2 | P24, P25 |
| US-29 | Record questions asked | Must | 5 | 2 | P24, P25 |
| US-30 | Post-interview debrief | Must | 5 | 2 | P24, P25 |
| US-31 | Edit and delete an interview | Must | 3 | 2 | P24 |
| US-33 | Interview outcome suggests a status change | Could | 3 | 8 | P92 |
| US-34 | Derive tested topics from an interview | Must | 3 | 8 | P92 |
| US-110 | STAR story bank | Should | 3 | 8 | P97 |

EPIC 5, Interview Preparation & Topic Mastery

| ID | User story | Priority | Points | Sprint | Phases |
|---|---|---|---:|---|---|
| US-35 | Browse the question bank | Must | 5 | 3 | P27, P28 |
| US-36 | Add my own questions | Must | 3 | 3 | P28 |
| US-37 | Record a practice attempt | Must | 5 | 3 | P28 |
| US-38 | Topic taxonomy with prerequisites | Should | 5 | 3 | P27 |
| US-39 | Topic mastery score with decay | Should | 8 | 3 | P31 |
| US-40 | Create preparation tasks | Must | 3 | 3 | P32 |
| US-41 | Preparation progress view | Must | 5 | 3 | P32 |
| US-42 | Spaced review queue | Could | 5 | 8 | P103 |
| US-90 | Question bank browser and practice logging | Must | 5 | 3, 6 | P29, P75 |
| US-106 | Question recommendations | Should | 5 | 8 | P93 |
| US-107 | Job readiness score | Should | 5 | 8 | P94 |

EPIC 6, Analytics & Pipeline Health

| ID | User story | Priority | Points | Sprint | Phases |
|---|---|---|---:|---|---|
| US-43 | Application funnel | Must | 5 | 3 | P34, P36 |
| US-44 | Conversion rates | Must | 5 | 3 | P34, P36 |
| US-45 | Activity over time | Must | 3 | 3 | P34, P36 |
| US-46 | Rejection stage distribution | Should | 3 | 3 | P35, P36 |
| US-47 | Topic performance | Should | 5 | 3 | P36 |
| US-48 | Stage dwell times | Should | 3 | 3 | P35, P36 |
| US-49 | Dashboard pipeline strip and action queue | Must | 8 | 3 | P38 |
| US-50 | Sample sizes shown across analytics | Must | 3 | 3 | P30, P35, P36 |
| US-80 | Export my applications and analytics | Should | 5 | 5 | P63, P64, P65 |
| US-114 | Resume performance and funnel simulator | Could | 5 | 8 | P101 |

EPIC 7, Explainable Recommendations

| ID | User story | Priority | Points | Sprint | Phases |
|---|---|---|---:|---|---|
| US-51 | Weak-topic recommendations | Must | 8 | 4 | P39, P40 |
| US-52 | Root-cause prerequisite routing | Should | 5 | 4 | P41 |
| US-53 | Evidence tiers and explanations | Must | 5 | 4 | P42 |
| US-54 | Non-causal language enforcement | Must | 3 | 4 | P42 |
| US-55 | Act on a recommendation | Should | 3 | 4, 6 | P43, P77 |
| US-56 | Stalled-application follow-up recommendations | Should | 3 | 4 | P40 |

EPIC 8, Federated Learning & Privacy Ledger

| ID | User story | Priority | Points | Sprint | Phases |
|---|---|---|---:|---|---|
| US-57 | Opt in to federated learning | Must | 5 | 4 | P55 |
| US-58 | Local feature extraction | Must | 8 | 4 | P44, P45, P51, P52 |
| US-59 | Local model training | Must | 8 | 4 | P46 |
| US-60 | Clipping and noise before transmission | Must | 5 | 4 | P47 |
| US-61 | Federated aggregation with minimum cohort | Must | 8 | 4 | P48, P53 |
| US-62 | Global model distribution | Must | 5 | 4 | P50, P53 |
| US-63 | Privacy Ledger | Should | 8 | 4 | P56, P57 |
| US-64 | Withdraw from federation | Must | 3 | 4 | P55 |
| US-65 | Federated Lift viewer | Should | 5 | 4 | P58 |
| US-66 | Privacy budget meter | Could | 5 | 8 | P104 |
| US-84 | Poisoning filter | Should | 3 | 4 | P49 |
| US-115 | Cohort benchmarks | Should | 5 | 8 | P102 |

EPIC 9, Platform, Quality & DevOps

| ID | User story | Priority | Points | Sprint | Phases |
|---|---|---|---:|---|---|
| US-67 | Repository and toolchain bootstrap | Must | 5 | 1 | P01, P03, P15 |
| US-68 | Docker Compose development environment | Must | 5 | 1 | P02 |
| US-69 | Database schema and migrations | Must | 8 | 1 | P04, P09 and every `feat(db)` phase |
| US-70 | CI pipeline | Must | 5 | 1 | P13 |
| US-71 | Application shell and navigation | Must | 5 | 1 | P07 |
| US-72 | Seed and demo data | Should | 3 | 2, 9 | P26, P105 |
| US-73 | Security test suite | Must | 5 | 1, 9 | P14, P109 |
| US-74 | Accessibility and responsive pass | Should | 5 | 9 | P110 |

EPIC 10, Notifications

| ID | User story | Priority | Points | Sprint | Phases |
|---|---|---|---:|---|---|
| US-76 | Notify when an application stalls | Should | 3 | 5 | P59, P60, P61, P62 |
| US-77 | Notify before an interview | Should | 3 | 5 | P59, P60, P61, P62 |
| US-78 | Notify about a missing debrief | Should | 2 | 5 | P59, P60, P61, P62 |
| US-79 | Notification bell and mark as read | Should | 3 | 5 | P59, P61, P62 |

EPIC 11, Experience and First Run

| ID | User story | Priority | Points | Sprint | Phases |
|---|---|---|---:|---|---|
| US-85 | Consistent professional interface | Should | 8 | 6 | P69, P79, P80 |
| US-86 | App shell: user menu, logout, command palette | Must | 3 | 6 | P70 |
| US-87 | Feedback and validation | Must | 3 | 6 | P71 |
| US-88 | Responsive tables, charts and privacy views | Must | 5 | 6 | P72 |
| US-116 | Sample data and guided tour | Could | 3 | 9 | P106 |
| US-117 | Designed error and edge states | Should | 3 | 9 | P107 |

EPIC 12, AI Assistance

| ID | User story | Priority | Points | Sprint | Phases |
|---|---|---|---:|---|---|
| US-102 | AI provider | Must | 5 | 8 | P89 |
| US-103 | Cohort Coach | Should | 5 | 8 | P90 |
| US-104 | Job posting parser | Should | 3 | 8 | P91 |
| US-105 | Debrief notes to questions | Should | 5 | 8 | P92 |
| US-108 | Resume and job description match | Should | 5 | 8 | P95 |
| US-109 | Mock interview | Should | 5 | 8 | P96 |
| US-111 | Interview prep pack | Should | 3 | 8 | P98 |
| US-112 | Follow-up and negotiation drafts | Could | 3 | 8 | P99 |
| US-113 | Weekly review | Could | 3 | 8 | P100 |

Total: 108 stories, 471 points.

### 10.2 Prioritisation

Priorities use MoSCoW. Within each sprint, Must stories are built before Should and Could stories, always in dependency order. Capacity is planned per department (`BUILD_PHASES_JAVA.md`, Team and ownership); the federated-learning work in Sprint 4 is the heaviest and is protected by the Sprint 3 spike.

### 10.3 Core product

The core product is every Must story plus the Should stories that carry the project's identity: US-18 (stall detection), US-38 and US-39 (topic graph and mastery decay), US-52 (root-cause routing), US-63 (Privacy Ledger) and US-65 (Federated Lift). Without them the product would be a competent tracker with generic recommendations and an invisible FL backend.

### 10.4 Out of scope

Features outside the product scope are listed once, in §31.2.

---

## 11. EPICs

| EPIC | Name | Stories | Points | Sprints | Business value |
|---|---|---|---:|---:|---|
| 1 | Authentication & User Management | 11 | 42 | 1, 5, 6 | Gate for everything; privacy foundation |
| 2 | Job Application Tracking | 16 | 64 | 1, 2, 3, 6, 7 | The core daily-use loop and the wider job-search workflow |
| 3 | Resume Management | 7 | 27 | 2, 7, 8 | Links documents to outcomes |
| 4 | Interview Tracking | 8 | 30 | 2, 8 | Captures the highest-value data in the system |
| 5 | Interview Preparation & Topic Mastery | 11 | 54 | 3, 6, 8 | Turns experience into action |
| 6 | Analytics & Pipeline Health | 10 | 45 | 3, 5, 8 | Makes performance legible |
| 7 | Explainable Recommendations | 6 | 27 | 4, 6 | The personalisation payoff |
| 8 | Federated Learning & Privacy Ledger | 12 | 68 | 4, 8 | Federated learning and privacy |
| 9 | Platform, Quality & DevOps | 8 | 41 | 1, 2, 9 | Makes the rest deliverable |
| 10 | Notifications | 4 | 11 | 5 | Surfaces what needs attention without polling the dashboard |
| 11 | Experience and First Run | 6 | 25 | 6, 9 | A product people want to use daily |
| 12 | AI Assistance | 9 | 37 | 8 | Grounded, private assistance across the whole loop |

Every user story maps to exactly one EPIC; the mapping is in §10.1. EPIC 8 combines federated learning and privacy because a privacy design specified separately from the FL design produces a privacy policy rather than a privacy architecture. EPIC 9 makes platform work visible in the backlog (§9).

---

## 12. Sprint Roadmap

Nine 2-week sprints. Each sprint lists its goal, stories, deliverables, dependencies, risks and Definition of Done; `BUILD_PHASES_JAVA.md` breaks each sprint into phases.

### Sprint 1: Foundation (see §13 for the full plan)
Goal: A running, deployable skeleton in which a user can register, log in, and manage applications end to end, plus the concept poster.
Deliverables: Compose environment, CI, migrations, auth, application CRUD with status history, app shell, poster.
Dependencies: None (greenfield).
Risks: Environment setup consumes more time than estimated on mixed OSes; the team is learning the stack. *Mitigation:* Compose parity verified by every member on day 3; a spike day is budgeted.
DoD: §30 project-wide DoD, plus: every member has merged at least one reviewed PR; `docker compose up` works on every member's machine; poster reviewed by all.

### Sprint 2: Resumes & Interview Capture (P16-P26)
Goal: Users can manage resume versions, attach them to applications, and capture structured interview experience including questions and a fast debrief.
Stories: US-16, US-17, US-20 → US-22, US-24, US-25, US-27 → US-31, US-72.
Deliverables: Resumes, Resume Detail, Interviews, Interview Detail pages; secure file upload and download with magic-byte validation; resume cloning; search and filter with combinable Specifications; seeded demo dataset.
Dependencies: Sprint 1 auth and applications.
Risks: File upload security is easy to get subtly wrong. *Mitigation:* write the malicious-upload test before the feature. Debrief UX may miss the 3-minute target. *Mitigation:* time it with 3 users mid-sprint and trim fields if needed.
DoD: Upload rejects a renamed executable; cross-user file access returns 404; debrief timed under 3 minutes with 3 users; seed data loads deterministically.

### Sprint 3: Preparation & Analytics (P27-P38)
Goal: The system turns recorded history into measured preparation progress and a legible campaign picture, with a dashboard that recommends action rather than displaying numbers.
Stories: US-18, US-35 → US-41, US-43 → US-50, US-90 (question content).
Deliverables: topic taxonomy and prerequisite graph; question bank with real, original questions for every topic; practice attempts; mastery engine with decay; preparation tasks and progress; analytics with documented formulas behind a Template Method base; Analytics page; dashboard pipeline strip and action queue; the one-day Flower spike.
Dependencies: Sprint 2 interview questions; seed data.
Risks: Analytics correctness is hard to eyeball. *Mitigation:* golden-file tests on a fixed fixture dataset, written first.
DoD: Every metric has a documented formula and a passing golden-file test; the topic graph passes cycle detection; every topic has real questions; the Flower spike is written up (ADR-011).

### Sprint 4: Recommendations & Federated Learning (P39-P58)
Goal: Explainable, evidence-tiered recommendations, and a federated learning loop with a user-visible Privacy Ledger.
Stories: US-51 → US-65, US-84.
Deliverables: recommendation generator chain, root-cause routing, evidence tiers, template registry and phrasing linter, Recommendations page; feature schema, local extraction and training, clipping and noise, Flower server with min-cohort refusal and norm filtering, model versioning; isolated FL compose profile; sanitised export from Java; FL gateway with circuit breaker; opt-in, Privacy Ledger and Federated Lift viewer.
Dependencies: Sprint 3 mastery scores and analytics (features depend on them).
Risks: Highest-risk sprint. Flower integration friction; too little real data to train on; poor convergence with non-IID synthetic clients. *Mitigations:* the Sprint 3 spike (§13.11); synthetic cohort clients; success defined as a *correct and private* pipeline rather than a high accuracy number.
DoD: A round with fewer than k clients is refused and logged; the payload schema test passes; the FL service demonstrably holds no user-schema credentials; the phrasing linter passes; the circuit-breaker fault-injection test passes; the ledger shows a real transmitted vector.

### Sprint 5: Notifications, Reports & Account (P59-P68)
Goal: In-app notifications driven by domain events, PDF and CSV report export, and complete account self-service.
Stories: US-06, US-07, US-08, US-76 → US-80, US-92.
Deliverables: notifications table, domain events, notification channel strategy and bell UI; report export with a builder and secured downloads; change password, data export, account deletion; Argon2id parameters and token lifetimes per NFR-01/02; password reset and email verification.
Dependencies: Sprint 3 stall detection and analytics; Sprint 1 auth.
Risks: Account deletion leaves orphan rows. *Mitigation:* an orphan-check test across every table.
DoD: Notifications and reports work end to end; deletion leaves no orphans; reset and verification tokens are single-use and expiring; no email enumeration.

### Sprint 6: Experience & Core Gaps (P69-P81)
Goal: One consistent, responsive interface in light and dark themes, with the interactions a job seeker expects every day.
Stories: US-16, US-17 (filter UI), US-55 (refinements), US-85 → US-91, US-93.
Deliverables: design system and themes; app shell with logout, breadcrumbs and command palette; feedback and validation layer; responsive tables, charts and privacy views; Kanban board; filter bar; question bank browser and practice logging; profile and onboarding; recommendation refinements; Google and GitHub sign-in; landing and auth page polish; consistent polish across every page; security headers, secret scan and SQL-interpolation gate.
Dependencies: Sprints 1-5 (this sprint refines every existing page).
Risks: Visual polish expands without end. *Mitigation:* one shared standard (tokens, page header, table, form, empty state, chart) applied to every page, reviewed with screenshots at two breakpoints.
DoD: Both themes and both breakpoints reviewed with screenshots; practice logging moves mastery end to end; illegal board moves refused with a reason; header test passes and CI gates fail on a planted violation.

### Sprint 7: Job-Search Workflow (P82-P88)
Goal: The networking and decision side of a job search.
Stories: US-23, US-95, US-96, US-99, US-100, US-101.
Deliverables: contacts and referrals with cold-contact nudges; company pages; offer comparison with deadline countdowns; weekly goals and activity heatmap; global search in the command palette; structured resume section editor with PDF export.
Dependencies: Sprint 5 domain events; Sprint 6 command palette and onboarding.
Risks: Global search leaking across users. *Mitigation:* search goes through the same user-scoped repositories, with a dedicated isolation test.
DoD: Every new endpoint isolation-tested; search never returns another user's data; cloned resumes carry edited sections.

### Sprint 8: AI & Intelligence (P89-P104)
Goal: AI assistance grounded in the user's own data on a local model, prep intelligence, and visible value from the federated model.
Stories: US-26, US-33, US-34, US-42, US-66, US-102 → US-115.
Deliverables: AI provider strategy (local Ollama default, optional Gemini); Cohort Coach; job-posting parser; debrief notes to tagged questions; question recommendations with embeddings; job readiness score; resume and job-description match; mock interview; STAR story bank; prep packs; email drafts; weekly review; resume performance and funnel simulator; cohort benchmarks; spaced review queue; privacy budget meter.
Dependencies: Sprints 3-4 mastery, analytics and FL; Sprint 7 resume sections and offers.
Risks: Local model too slow on team machines. *Mitigation:* timeouts, streaming, and a graceful "AI unavailable" state; every non-AI feature keeps working.
DoD: Every AI feature works with the local model and degrades gracefully when it is down; no prompt contains another user's data; the hosted-provider disclosure is shown whenever it is active.

### Sprint 9: Hardening & Delivery (P105-P110)
Goal: A demo-ready, verified product: realistic data, a first-run experience, designed error states, disclosure pages, and the final security and accessibility verification across every feature.
Stories: US-72, US-73, US-74, US-116, US-117.
Deliverables: seed data for every feature; sample data loader and guided tour; designed error, 404, offline and session-expired screens; privacy policy, AI disclosure and help pages; full security suite enumerated from the OpenAPI schema; accessibility remediation in both themes.
Dependencies: Everything.
Risks: Defects from Sprints 6-8 crowd out verification. *Mitigation:* 20% of Sprint 9 capacity is reserved for defects and not planned against stories.
DoD: Full security suite green; zero critical axe violations in both themes; sample data and tour work for a new user; demo run end to end twice without intervention.

---

## 13. Sprint 1 Detailed Plan

Sprint 1 is specified to task level.

### 13.1 Sprint goal

> *A user can register, log in, and manage their job applications with a full status history, running in a reproducible Docker environment with CI, migrations, and a concept poster, the vertical slice that proves the architecture works end to end.*

### 13.2 Stories included

US-67, US-68, US-69, US-70, US-71 (platform); US-01 → US-05 (auth); US-09 → US-15 (applications). 66 points.

The sprint is a *vertical* slice through every layer (React → API → service → repository → Postgres → migration → test → CI → container), not a horizontal one. It de-risks the architecture in week two rather than week ten.

### 13.3 Database tables required in Sprint 1

Only these seven. Everything else waits.

| Table | Purpose | Key columns |
|---|---|---|
| `users` | Accounts | `id` (UUID PK), `email` (unique, citext), `password_hash`, `display_name`, `target_role`, `graduation_year`, `timezone`, `is_active`, `fl_opt_in` (default false), `created_at`, `updated_at` |
| `refresh_tokens` | Revocable sessions | `id` PK, `user_id` FK→users, `token_hash`, `expires_at`, `revoked_at`, `created_at` |
| `companies` | Normalised employers | `id` PK, `name`, `normalized_name` (unique per user), `website`, `industry`, `created_by_user_id` FK→users |
| `applications` | Core entity | `id` PK, `user_id` FK→users (NOT NULL), `company_id` FK→companies, `job_title`, `job_description`, `job_url`, `location`, `work_mode`, `job_type`, `salary_min`, `salary_max`, `salary_currency`, `date_applied`, `status`, `current_stage`, `priority`, `source`, `is_referral`, `deadline`, `recruiter_name`, `recruiter_email`, `notes`, `resume_id` (nullable, FK added in Sprint 2), `archived_at`, `created_at`, `updated_at` |
| `application_status_history` | Immutable audit | `id` PK, `application_id` FK→applications (ON DELETE CASCADE), `from_status`, `to_status`, `note`, `changed_at` |
| `tags` | User-scoped labels | `id` PK, `user_id` FK→users, `name`, unique (`user_id`,`name`) |
| `application_tags` | Join | `application_id` FK, `tag_id` FK, composite PK |

Indexes created in Sprint 1: `applications(user_id, status)`, `applications(user_id, date_applied DESC)`, `applications(user_id, company_id)`, `application_status_history(application_id, changed_at DESC)`, `users(email)`, `refresh_tokens(token_hash)`.

Constraints: `salary_min <= salary_max`; `status` constrained to the enum; `date_applied <= current_date`; `applications.user_id` NOT NULL with no default.

### 13.4 APIs required in Sprint 1

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | No | Create account, return tokens |
| POST | `/api/v1/auth/login` | No | Authenticate, return tokens |
| POST | `/api/v1/auth/refresh` | Refresh token | Rotate access token |
| POST | `/api/v1/auth/logout` | Yes | Revoke refresh token |
| GET | `/api/v1/users/me` | Yes | Current profile |
| PATCH | `/api/v1/users/me` | Yes | Update profile |
| GET | `/api/v1/applications` | Yes | Paginated list (`page`, `size`, `sort`) |
| POST | `/api/v1/applications` | Yes | Create |
| GET | `/api/v1/applications/{id}` | Yes | Detail with history |
| PATCH | `/api/v1/applications/{id}` | Yes | Partial update |
| DELETE | `/api/v1/applications/{id}` | Yes | Delete with cascade |
| POST | `/api/v1/applications/{id}/status` | Yes | Change status, write history |
| GET | `/api/v1/applications/{id}/history` | Yes | Timeline entries |
| GET | `/api/v1/companies` | Yes | Autocomplete search |
| POST | `/api/v1/companies` | Yes | Create on demand |
| GET | `/api/v1/health` | No | Liveness + DB check |

### 13.5 Frontend pages required in Sprint 1

| Page | Route | Contents |
|---|---|---|
| Login | `/login` | Email/password form, validation, error state, link to register |
| Register | `/register` | Registration form with live password-strength feedback |
| Dashboard | `/` | Sprint 1 version: counts by status, recent applications, primary CTA, empty state (the action queue arrives in Sprint 3) |
| Applications | `/applications` | Data table, pagination, sort, create button, empty/loading/error states |
| Application Detail | `/applications/:id` | Field panel, status changer, timeline, edit and delete actions |
| Application Form | modal or `/applications/new` | Create and edit form with company autocomplete |
| Profile | `/profile` | Profile fields and save |
| 404 | `*` | Not-found page |

### 13.6 Components required in Sprint 1

`AppShell`, `Sidebar`, `TopBar`, `ProtectedRoute`, `DataTable` (generic, sortable, paginated), `StatusBadge`, `PriorityBadge`, `ConfirmDialog`, `FormField` wrappers, `EmptyState`, `LoadingSkeleton`, `ErrorState`, `Timeline`, `CompanyCombobox`, `Toast` provider, `PageHeader`.

Building `DataTable`, `EmptyState`, `LoadingSkeleton`, and `ErrorState` as reusable primitives in Sprint 1 is what prevents four divergent table implementations by Sprint 4.

### 13.7 Authentication scope for Sprint 1

In: registration, login, logout, refresh rotation, Argon2id hashing, password policy with blocklist, JWT issuing and verification, `get_current_user` dependency, protected routes, auth rate limiting, per-user query scoping.
Out (later sprints): change password, account deletion, password reset and email verification (Sprint 5), Google and GitHub sign-in (Sprint 6). MFA and an admin role are out of scope (§31.2).

### 13.8 Testing scope for Sprint 1

- Unit: password hashing and verification; token creation, expiry, and rotation; status state-machine validity; application service create/update/delete; days-in-stage helper.
- API: all 16 endpoints, happy path, validation failure, unauthenticated (401), and cross-user access (404).
- Cross-user isolation: a fixture creating users A and B; A's application must return 404 to B on GET, PATCH, DELETE, and status change. This test is written in Sprint 1 and is the template for US-73.
- Frontend (RTL): login form validation and submission; application form validation; DataTable renders rows, empty state, and loading state; ProtectedRoute redirects when unauthenticated.
- Integration: register → login → create application → change status → verify history, run against a real Postgres test container.
- Coverage gate: ≥70% backend line coverage enforced in CI from Sprint 1, not retrofitted later.

### 13.9 Documentation scope for Sprint 1

`README.md` (setup, run, test, structure); `docs/requirements.md` (FR/NFR/DR from §6-8); `docs/architecture.md` with the C4-style diagrams from §16; `docs/database.md` with the ERD; `docs/api.md` (or published OpenAPI); `docs/agile/` containing product backlog, sprint 1 plan, daily standup log, sprint review and retrospective notes; `docs/adr/` with ADRs for the first decisions (Argon2id over bcrypt; refresh-token rotation; per-user query scoping at the repository layer; Flyway from day one); `CONTRIBUTING.md` with branch and commit conventions.

### 13.10 Concept poster content (Sprint 1 deliverable)

See §29 for full layout. Sprint 1 task: produce the finished A1 poster as PDF plus source, reviewed by the whole team.

### 13.11 The FL spike (Sprint 3)

A timeboxed 1-day Flower spike (P33) runs in Sprint 3: two hard-coded clients, a trivial 3-feature logistic regression and one FedAvg round. It surfaces Flower version and API problems a full sprint before Sprint 4 depends on it.

### 13.12 Sprint 1 Definition of Done

Project-wide DoD (§30), plus:
- `docker compose up` starts Postgres, backend, and frontend on every team member's machine.
- All migrations apply cleanly from an empty database and roll back cleanly.
- CI is green on `main`; a failing test blocks merge.
- The cross-user isolation test suite passes for every Sprint 1 endpoint.
- Backend coverage ≥70%.
- A new user can register and create their first application in under 60 seconds.
- Every list and detail view has designed empty, loading, and error states.
- Concept poster complete and reviewed.
- Every team member has authored and merged at least one reviewed PR.
- Sprint review demo runs end to end without a code change.

---

## 14. Requirement Conflicts & Resolution

### C1: Privacy vs Analytics richness

Conflicting: FR-58…FR-69 (rich analytics), FR-71 (weak-topic identification) vs NFR-10, NFR-16, DR-18, DR-21.
Why they conflict: Better analytics and better models want more data, more granular data, and data pooled across users. Every one of those increases exposure and re-identification risk, and job-search data is career-sensitive.
Decision: Locality wins over richness. Personal analytics are computed only from the requesting user's own data. Cross-user learning happens only through federated model parameters, never through pooled records.
Resolution: Two-tier design, Tier 1 (personal analytics, full fidelity, never leaves the user's partition) and Tier 2 (federated learning, fixed numeric schema, clipped and noised). Feature-schema minimisation is enforced by an allow-list test (NFR-10). Data minimisation reviewed at each sprint review (NFR-16).
Impact: Personal analytics have low statistical power for new users; the system compensates with sample-size labelling (FR-67) and heuristic Tier-D recommendations rather than fabricated precision. Federated features are coarser than centralised ones, so the global model will be weaker than a centralised equivalent, an accepted, documented trade.

### C2: Centralised ML vs Federated privacy

Conflicting: Ease and accuracy of centralised training vs FR-79…FR-92, DR-21, and the course requirement to use FL.
Why they conflict: Centralised training over a single database is dramatically simpler, converges faster, and produces better metrics. FL adds a server, N client processes, orchestration, DP noise, and non-IID convergence problems, for a strictly worse accuracy number.
Decision: Use real FL with separate client data. FL is a mandatory requirement, and simulating it over pooled data would not test the privacy property.
Resolution: The prototype FL is a Flower deployment with separate data stores per client (§22): small model, few clients, few rounds, synthetic cohort peers. Success is defined as architectural correctness and demonstrable privacy, not accuracy.
Impact: Higher engineering cost, concentrated in Sprint 4, mitigated by the Sprint 3 spike. Model quality will be modest and is reported as measured, including the FL-vs-centralised gap.

### C3: Feature richness vs deliverability

Conflicting: The full FR set vs a fixed academic deadline and student capacity.
Why they conflict: The requirement list describes a product a funded team would build over months. Attempting all of it produces a broad, broken system, which is worse than a narrow, working one.
Decision: Depth over breadth. Ship a smaller set of features that are complete, tested, and polished.
Resolution: MoSCoW prioritisation (§10); a core product defined as Must plus six identity-carrying Shoulds (§10.3); an explicit out-of-scope list (§31.2); a sprint order in which every sprint ends with a working product; a strict Definition of Done that makes "nearly finished" impossible to claim.
Impact: Several attractive features (bulk operations, external recruiter feedback, an admin console, calendar and email sync) are outside the product scope. This is documented in the report as scope management.

### C4: Personalisation vs standardisation

Conflicting: FR-70…FR-77 (individually tailored recommendations) vs FR-79 (fixed feature schema) and FR-59 (comparable, standard metrics).
Why they conflict: Personalisation wants each user's idiosyncratic signal; federated learning requires every client to share an identical, fixed-length schema; and comparable metrics require standard definitions applied uniformly.
Decision: Standard schema, personal weighting. The vocabulary is shared; the emphasis is individual.
Resolution: A shared, versioned topic taxonomy and feature schema provide the common ground. Personalisation comes from the user's own mastery scores, decay state, and history determining *which* topics surface and in what order. The Federated Lift viewer (US-65) makes the boundary between the shared model and the personal ranking visible to the user.
Impact: Users cannot invent arbitrary topics that participate in federated learning; free-form topics are supported locally but excluded from the feature vector. This limitation is disclosed on the opt-in screen.

### C5: Security vs usability

Conflicting: NFR-01…NFR-09 (short tokens, strong passwords, rate limits, confirmations) vs NFR-30…NFR-35 (fast, low-friction use).
Why they conflict: Every security control adds a step. A 15-minute token naively implemented means being logged out mid-form.
Decision: Move friction to the boundary, remove it from the interior. Be strict at registration, login, and destructive actions; be invisible everywhere else.
Resolution: Silent refresh-token rotation (US-04) makes short access tokens imperceptible. The password policy is enforced with live, constructive feedback rather than a post-submit rejection. Confirmation dialogs appear only for destructive and irreversible actions, and they name the object. Rate limiting applies to auth endpoints only. FL opt-in is a single explicit decision, not a per-round prompt.
Impact: More frontend work (token refresh interceptor, request queueing during refresh) in exchange for a materially better experience. Documented as an ADR.

### C6: Stated uncertainty vs actionable confidence

Conflicting: FR-67, FR-74, DR-06, DR-08, DR-14 (statistical caution) vs G3 and NFR-30 (users want a clear answer).
Why they conflict: Hedged output is useless output. "Your graph performance may possibly be weak (n=3, not statistically significant)" helps nobody. But confident output from n=3 would be misleading.
Decision: Be decisive about the action and state the evidence plainly. Recommend one concrete next step; label the confidence behind it plainly.
Resolution: The evidence-tier system (D3/FR-73): the headline is an imperative ("Practise BFS and DFS this week"), and the tier badge plus expandable "why am I seeing this?" carries the uncertainty. Below the minimum sample threshold the system says "not enough data yet" rather than guessing. Prohibited causal phrasing is enforced by a failing test (FR-74).
Impact: Every recommendation needs both an action template and an evidence explanation, roughly doubling the copy work in EPIC 7. We accept this cost.

### C7: FL model utility vs differential privacy noise

Conflicting: FR-83/FR-86 (a useful global model) vs FR-81/FR-82/FR-90 (clipping, noise, budget) and DR-21.
Why they conflict: Noise sufficient for a meaningful epsilon can overwhelm the signal, especially with few clients and small local datasets. This is the central tension of practical DP and it does not have a clean resolution.
Decision: Privacy parameters are fixed first; utility is measured against them. We do not tune epsilon upward to make the accuracy chart look better.
Resolution: Defaults chosen from literature (clipping norm S=1.0, noise multiplier σ=1.0, k=3) and held constant. A utility-vs-privacy sweep is run as an *experiment* and reported as a finding, including the result that at small client counts the privacy cost is high relative to the utility gained. The three user-facing postures expose the trade-off rather than hiding it.
Impact: Reported model performance will be modest because of the noise. We report the privacy-utility trade-off as measured.

---

## 15. Technology Stack

### 15.1 Stack, with justification

| Layer | Technology | Reason |
|---|---|---|
| UI library | React 19 | The interface is a stateful dashboard with tables, filters, modals, and charts, the component-tree problem React solves. Largest ecosystem, so the team can find help fast. |
| Language (FE) | TypeScript | The domain has many similar-shaped entities (application, interview, resume); a compiler catches a field mismatch at build time. |
| Styling | Tailwind CSS | Utility classes keep styling colocated with markup and enforce a design-token scale (spacing, colour, type), so the UI stays consistent without a separate style guide. |
| Components | shadcn/ui | Accessible Radix primitives delivered as owned source. NFR-44…NFR-47 come largely for free, and the components can be restyled away from the default look (§2.3). |
| Charts | Recharts | Declarative React charts covering funnel, bar, line, and area needs, composable enough for the sample-size annotations FR-67 requires. |
| Backend framework | Spring Boot 3 (Java 21) | Owns everything user-facing. Mature dependency injection makes per-user scoping and every pattern in `ARCHITECTURE_REVISION_JAVA.md` §4 clean; Bean Validation satisfies FR-96; springdoc generates the OpenAPI contract. |
| Database | PostgreSQL 16 | The domain is relational: users own applications, which own interviews, which own questions. Isolation is best enforced with foreign keys and constraints. It also provides JSONB for semi-structured resume content, window functions for dwell-time and funnel queries, and role-based privilege separation, which makes the FL isolation in §22 enforceable. |
| ORM and migrations | Spring Data JPA + Hibernate; Flyway | Parameterised queries by default (NFR-03); a repository layer where per-user scoping is enforced once (FR-94); versioned migrations with rollback scripts (NFR-27). |
| Auth | Spring Security: JWT + Argon2id; OAuth2 client for Google and GitHub | JWT keeps the API stateless (NFR-23) while server-side refresh tokens keep logout revocable (FR-06). Argon2id over bcrypt: memory-hard, resistant to GPU cracking, and the current OWASP recommendation (ADR-001). |
| ML | scikit-learn | Right-sized for the task. Linear models expose `coef_` and `intercept_` as plain arrays, which is what FedAvg averages. |
| FL | Flower (flwr) in a small Python service | Framework-agnostic, works cleanly with scikit-learn, separates client and server, supports custom strategies for the min-cohort and norm-filtering rules (FR-83/FR-84), and has a simulation mode as a fallback. Flower has no Java equivalent, which is why Python is limited to this service. |
| AI | Ollama local model by default; Google Gemini optional | AI features answer from the user's own data without that data leaving the server; the hosted provider is opt-in for the operator and disclosed to users. |
| Testing | JUnit 5, MockMvc, Testcontainers; pytest; React Testing Library + Vitest | Real-Postgres integration tests, controller tests, FL unit tests, and behavioural UI tests. |
| Containers | Docker + Docker Compose | Removes environment drift (NFR-41) and makes the FL isolation demonstrable: separate containers, separate credentials, inspectable. |
| VCS and CI | Git + GitHub + GitHub Actions | Per-member contribution record, CI on every push, Projects for the sprint board. |

Full detail, including the resilience and mapping libraries, is in `ARCHITECTURE_REVISION_JAVA.md` §2.

### 15.2 Supporting libraries

| Addition | Layer | Why it is necessary |
|---|---|---|
| Vite | FE build | React needs a bundler; Vite shares config with Vitest. |
| React Router | FE routing | Multi-page SPA requires routing. |
| TanStack Query | FE data | Server-state caching, request deduplication, and a clean place for the single-flight token-refresh interceptor from US-04. |
| React Hook Form + Zod | FE forms | The application, resume, interview, and debrief forms are large; Zod schemas mirror the server-side validation rules. |
| Resilience4j | BE | Circuit breaker and fallback at the FL boundary. |
| springdoc-openapi | BE | Generated OpenAPI contract, also used to enumerate endpoints in the security suite. |
| Testcontainers | BE test | A real Postgres instance for integration tests. |
| NumPy | FL | Transitive under scikit-learn; used directly for clipping and noise arithmetic. |

### 15.3 Explicitly rejected

| Rejected | Why |
|---|---|
| Redis | Nothing needs a cache or queue at this scale; rate limiting uses an in-process store. Adding it costs a container and a failure mode for no benefit. |
| Message brokers / separate workers | FL rounds and detectors run on Spring scheduling; nothing needs a queue. |
| MinIO / S3 | Local volume storage is adequate; storage sits behind `FileStorageStrategy`, so S3 is a swap, not a rewrite. |
| Next.js | Server-side rendering brings no benefit to an authenticated dashboard and complicates the Compose setup. |
| PyTorch / TensorFlow | Over-powered for a logistic-regression-scale problem. |
| A hosted LLM as the default | Would send user data to a third party by default, undermining the privacy story; the local model is the default and the hosted provider is optional and disclosed. |
| MongoDB | The domain is relational and the isolation requirement depends on foreign keys. |

---

## 16. System Architecture

### 16.1 Context

```
   ┌───────────┐                        ┌──────────────────────┐
   │ Job seeker│──── HTTPS ────────────▶│                      │
   └───────────┘                        │   Cohort System      │
   ┌───────────┐                        │                      │
   │  Admin    │──── HTTPS ────────────▶│                      │
   └───────────┘                        └──────────────────────┘
   External calls: Google/GitHub sign-in (identity only) and, only when
   the operator enables it, a hosted AI provider (disclosed to users).
   No third-party data sharing otherwise.
```

### 16.2 Container view

```
┌──────────────────────────────────────────────────────────────────────┐
│  Browser                                                             │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ React + TypeScript SPA (Vite)                                  │  │
│  │ Pages · Components · TanStack Query · Router · Recharts        │  │
│  └────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────┬───────────────────────────────────────┘
                               │ REST / JSON  (Bearer JWT)
                               ▼
┌──────────────────────────────────────────────────────────────────────┐
│  Spring Boot API  (container: api)                                   │
│                                                                      │
│  ── API layer ────────────────────────────────────────────────────   │
│  auth · users · applications · companies · resumes · interviews ·    │
│  questions · preparation · analytics · recommendations · fl · health │
│                                                                      │
│  ── Service layer (business logic) ───────────────────────────────   │
│  AuthService · ApplicationService · ResumeService ·                  │
│  InterviewService · PreparationService · MasteryEngine ·             │
│  AnalyticsService · RecommendationEngine · FLCoordinator             │
│                                                                      │
│  ── Repository layer (ALL queries scoped by user_id) ─────────────   │
│                                                                      │
│  ── Cross-cutting: JWT auth · Bean Validation · rate limiting        │
│     structured logging · error handling · file storage adapter       │
└───────────┬───────────────────────────────────┬──────────────────────┘
            │ JPA                               │ HTTP (control plane
            ▼                                   │  only, no user data)
┌────────────────────────┐                      │
│ PostgreSQL (app)       │                      │
│ schema: app            │                      │
│ users, applications,   │                      │
│ resumes, interviews,   │                      │
│ questions, tasks,      │                      │
│ recommendations,       │                      │
│ fl_rounds, fl_models,  │                      │
│ privacy_ledger         │                      │
└────────────────────────┘                      │
                                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    ═══ PRIVACY BOUNDARY ═══                          │
│                                                                      │
│  ┌────────────────────┐  gRPC (parameters only)  ┌────────────────┐  │
│  │ FL Client 1        │◀────────────────────────▶│ Flower Server  │  │
│  │  · own DB schema   │                          │ (container:    │  │
│  │  · local extractor │  ┌────────────────────┐  │  fl-server)    │  │
│  │  · sklearn trainer │◀▶│ FL Client 2 … N    │◀▶│                │  │
│  │  · clip + noise    │  │  (same shape)      │  │ FedAvg         │  │
│  └────────┬───────────┘  └─────────┬──────────┘  │ + min-cohort k │  │
│           │                        │             │ + norm filter  │  │
│           ▼                        ▼             │                │  │
│  ┌────────────────┐    ┌────────────────┐        │ NO DATABASE    │  │
│  │ Postgres       │    │ Postgres       │        │ CREDENTIALS    │  │
│  │ schema: client1│    │ schema: clientN│        └───────┬────────┘  │
│  │ (isolated,     │    │ (isolated)     │                │           │
│  │  own role)     │    │                │                ▼           │
│  └────────────────┘    └────────────────┘        ┌────────────────┐  │
│                                                  │ Global model   │  │
│  Raw records NEVER cross this line ─────────────▶│ artifact store │  │
│  Only: clipped + noised float vectors            └────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

The language boundary between the Spring Boot API and the Python fl-service, including the sanitised export from Java and the circuit breaker, is drawn in `ARCHITECTURE_REVISION_JAVA.md` §3.

### 16.3 Component responsibilities

| Component | Responsibility | Explicitly not responsible for |
|---|---|---|
| SPA | Rendering, client-side validation, routing, server-state caching, token refresh | Authorisation decisions, business rules, any computation the API can be trusted to do |
| API layer | HTTP concerns: routing, deserialisation, status codes, auth extraction | Business logic, direct database access |
| Service layer | Business rules, orchestration, state machine, scoring, metric computation | HTTP concerns, SQL construction |
| Repository layer | Data access, always parameterised, always scoped to `user_id` | Business decisions |
| MasteryEngine | Topic mastery scoring with decay; prerequisite traversal | Persistence, presentation |
| AnalyticsService | Funnel, conversions, dwell times, distributions, sample-size annotation | Cross-user computation of any kind |
| RecommendationEngine | Candidate generation, evidence tiering, ranking, template rendering | Model training, causal claims |
| FLCoordinator | Round scheduling, model version registry, ledger writes, serving the global model to the app | Training; touching client raw data |
| FL Client | Local feature extraction, local training, clipping, noise, transmission | Sending anything but parameters |
| Flower Server | FedAvg aggregation, min-cohort enforcement, norm filtering, round metadata | Reading user data (it has no credentials to do so) |

### 16.4 Data flow: two loops

Loop A: normal request. Browser sends a request with a bearer token → API validates the token, resolves `current_user` → the controller calls a service → the service calls repositories, which inject `user_id` into every query → Postgres returns only that user's rows → the service computes → a mapper builds the response DTO → the browser renders. *User data never leaves the app boundary.*

Loop B: federated round. FLCoordinator opens round *r* and publishes global model *M(r-1)* → each opted-in client fetches *M(r-1)* → each client reads only its own schema, extracts a fixed 24-float feature matrix locally, and fits a local model → computes the update Δ, clips it to ‖Δ‖₂ ≤ S, adds Gaussian noise N(0, σ²S²I) → sends the noised vector over gRPC → the Flower server counts participants, refuses if < k, filters over-norm updates, computes the FedAvg mean → *M(r)* is versioned and stored → FLCoordinator writes round metadata and a per-user ledger entry → clients fetch *M(r)*. *No record, string, or identifier crosses the boundary at any point.*

### 16.5 Security boundaries

| Boundary | Control |
|---|---|
| Browser ↔ API | HTTPS in deployment; bearer JWT; CORS restricted to the known frontend origin; security headers |
| API ↔ Database | Least-privilege application role; no DDL rights at runtime; connection string only from environment |
| User ↔ User | Enforced at the repository layer, not in route handlers; 404 rather than 403 on foreign resources; verified by an OpenAPI-enumerated test suite (US-73) |
| API ↔ FL server | Control-plane HTTP carrying round configuration and model artifacts only |
| FL client ↔ FL server | gRPC carrying fixed-schema float vectors only; payload asserted against an allow-list in tests |
| FL client ↔ its data | Each client's database role can read exactly one schema and nothing else |
| Files ↔ Web | Uploads stored outside any served directory, generated names, served only through an authenticated, expiring endpoint |

### 16.6 Privacy boundaries: what crosses what

| Data | Stays on client | Processed locally | In model update | Aggregated | Ever shown globally |
|---|---|---|---|---|---|
| Email, password hash | Yes | Yes | No | No | No |
| Company names | Yes | Yes | No | No | No |
| Job descriptions, notes | Yes | Yes | No | No | No |
| Resume files and content | Yes | Yes | No | No | No |
| Interview question text | Yes | Yes | No | No | No |
| Interviewer names | Yes | Yes | No | No | No |
| Individual outcomes | Yes | Yes | No | No | No |
| Bucketed topic mastery (derived) | Yes | Yes | Yes (as a feature) | Yes | No |
| Normalised practice counts (derived) | Yes | Yes | Yes | Yes | No |
| Model parameters, clipped + noised | none | Yes | Yes | Yes | Only as the global model |
| Round metadata (count, timestamp) | none | none | none | Yes | Yes (aggregate only) |

Poster sentence: only clipped, noised model parameters ever cross the boundary, and every user can read their own outgoing vector in the Privacy Ledger.

---

## 17. Database Design

### 17.1 Entity-relationship overview

```
users 1───∞ applications ∞───1 companies
  │              │
  │              ├──1───∞ application_status_history
  │              ├──1───∞ interviews ──1───∞ interview_questions ──∞───1 topics
  │              │            └──1───1 interview_feedback
  │              ├──∞───1 resumes
  │              └──∞───∞ tags (via application_tags)
  │
  ├──1───∞ resumes ──1───∞ resume_sections
  ├──1───∞ questions (own bank) ──∞───1 topics
  ├──1───∞ practice_attempts ──∞───1 questions
  ├──1───∞ preparation_tasks ──∞───1 topics
  ├──1───∞ user_topic_mastery ──∞───1 topics
  ├──1───∞ recommendations ──1───∞ recommendation_evidence
  ├──1───1 fl_client_state
  └──1───∞ privacy_ledger_entries ──∞───1 fl_rounds

topics ∞───∞ topics  (topic_prerequisites, DAG)
fl_rounds ∞───1 fl_models
```

### 17.2 Table definitions

Common to every table: `id UUID PK DEFAULT gen_random_uuid()`, `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`, and `updated_at` on mutable tables.

users, `email CITEXT UNIQUE NOT NULL`, `password_hash TEXT NOT NULL`, `display_name`, `target_role`, `graduation_year SMALLINT`, `timezone`, `role` (`user`|`admin`), `is_active BOOLEAN DEFAULT true`, `fl_opt_in BOOLEAN NOT NULL DEFAULT false`, `fl_privacy_posture` (`strict`|`balanced`|`contributor`, default `balanced`), `epsilon_spent NUMERIC DEFAULT 0`.

refresh_tokens, `user_id FK→users ON DELETE CASCADE`, `token_hash UNIQUE`, `expires_at`, `revoked_at NULL`. Index on `token_hash`.

companies, `name`, `normalized_name`, `website`, `industry`, `created_by_user_id FK→users`. Unique (`created_by_user_id`, `normalized_name`). *Design note:* companies are user-scoped rather than global. A global company table would create a subtle cross-user inference channel (company creation timestamps leak who is applying where) and would need deduplication logic we do not have time to write. Scoping per user costs some duplication and buys isolation. Recorded as ADR-004.

applications, `user_id FK→users NOT NULL ON DELETE CASCADE`, `company_id FK→companies NOT NULL`, `resume_id FK→resumes ON DELETE SET NULL`, `job_title NOT NULL`, `job_description`, `job_url`, `location`, `work_mode` (`onsite`|`hybrid`|`remote`), `job_type` (`internship`|`full_time`|`part_time`|`contract`), `salary_min NUMERIC`, `salary_max NUMERIC`, `salary_currency CHAR(3)`, `date_applied DATE NOT NULL`, `status` (enum, §17.5), `current_stage`, `priority` (`low`|`medium`|`high`), `source`, `is_referral BOOLEAN`, `deadline DATE`, `recruiter_name`, `recruiter_email`, `notes TEXT`, `cover_letter TEXT`, `archived_at`, `stage_entered_at TIMESTAMPTZ NOT NULL`.
Constraints: `CHECK (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)`, `CHECK (date_applied <= CURRENT_DATE)`.
Indexes: `(user_id, status)`, `(user_id, date_applied DESC)`, `(user_id, company_id)`, `(user_id, archived_at)`.

application_status_history, `application_id FK ON DELETE CASCADE`, `from_status`, `to_status NOT NULL`, `note`, `changed_at NOT NULL`. No `updated_at`, no update endpoint, append-only by design. Index `(application_id, changed_at DESC)`. This table is the source of truth for the funnel (§20.7): current status alone cannot tell you an application *reached* the interview stage before being rejected.

tags / application_tags, `tags(user_id FK, name)` unique per user; join table with composite PK.

resumes, `user_id FK NOT NULL`, `name NOT NULL`, `version_label`, `target_role`, `summary`, `is_default BOOLEAN`, `parent_resume_id FK→resumes NULL` (lineage), `file_path`, `file_original_name`, `file_size_bytes`, `file_mime`, `file_uploaded_at`. Partial unique index enforcing one default per user.

resume_sections, `resume_id FK ON DELETE CASCADE`, `section_type` (`experience`|`education`|`project`|`achievement`|`skill`), `title`, `organisation`, `start_date`, `end_date`, `description`, `sort_order`, `metadata JSONB`. *Design note:* one flexible table rather than five near-identical ones, because the sections share a shape and JSONB absorbs the differences.

interviews, `application_id FK ON DELETE CASCADE NOT NULL`, `user_id FK NOT NULL` (denormalised deliberately, so every isolation query is a single-table filter and can never be forgotten in a join), `scheduled_at`, `completed_at`, `round_number SMALLINT NOT NULL`, `round_type` (enum), `duration_minutes`, `mode` (`onsite`|`video`|`phone`), `interviewer_name`, `interviewer_role`, `outcome` (`passed`|`failed`|`pending`|`unknown`), `perceived_difficulty SMALLINT 1-5`, `notes`. Unique (`application_id`, `round_number`).

interview_questions, `interview_id FK ON DELETE CASCADE`, `user_id FK`, `question_text NOT NULL`, `topic_id FK→topics NULL`, `category` (enum), `difficulty` (`easy`|`medium`|`hard`), `self_rating SMALLINT 1-5`, `time_taken_minutes`, `linked_question_id FK→questions NULL`, `notes`.

interview_feedback, `interview_id FK UNIQUE ON DELETE CASCADE`, `self_rating SMALLINT 1-5 NOT NULL`, `what_went_well`, `what_went_poorly`, `improvement_areas`, `external_feedback`, `submitted_at`, `is_draft BOOLEAN`. One-to-one with interview.

topics, `name UNIQUE`, `slug`, `category` (enum), `description`, `default_difficulty`, `taxonomy_version SMALLINT`. Seeded, system-owned.

topic_prerequisites, `topic_id FK`, `prerequisite_topic_id FK`, composite PK. Must remain acyclic, enforced by a seed-time cycle-detection test, since Postgres cannot express this as a constraint.

questions (bank), `user_id FK NULL` (NULL = system-seeded), `topic_id FK`, `category`, `difficulty`, `question_text NOT NULL`, `expected_answer`, `source`, `is_system BOOLEAN`, `forked_from_id FK→questions NULL`. Index `(user_id, topic_id)`, `(category, difficulty)`.

practice_attempts, `user_id FK NOT NULL`, `question_id FK NOT NULL`, `status` (`not_started`|`in_progress`|`confident`|`needs_review`), `confidence SMALLINT 1-5`, `time_taken_minutes`, `notes`, `attempted_at NOT NULL`. Append-only, repeated attempts create rows, giving a confidence trajectory rather than a single overwritten value. Index `(user_id, question_id, attempted_at DESC)`.

user_topic_mastery, `user_id FK`, `topic_id FK`, `mastery_score NUMERIC(4,3)` in [0,1], `raw_score`, `last_practiced_at`, `last_interviewed_at`, `observation_count`, `computed_at`. Unique (`user_id`, `topic_id`). A materialised cache recomputed on write to practice or interview questions; the formula is in §21.4.

preparation_tasks, `user_id FK`, `topic_id FK NULL`, `title NOT NULL`, `description`, `due_date`, `estimated_minutes`, `is_completed`, `completed_at`, `source_recommendation_id FK→recommendations NULL`, `priority`.

recommendations, `user_id FK`, `type` (`weak_topic`|`prerequisite_gap`|`stalled_application`|`resume_pattern`|`prep_cadence`), `title`, `body`, `evidence_tier CHAR(1)` in (A,B,C,D), `observation_count`, `score NUMERIC`, `target_topic_id FK NULL`, `target_application_id FK NULL`, `model_version` NULL, `state` (`active`|`accepted`|`snoozed`|`dismissed`), `snoozed_until`, `dismissed_reason`, `generated_at`.

recommendation_evidence, `recommendation_id FK ON DELETE CASCADE`, `evidence_type`, `description`, `source_table`, `source_id`, `weight`. This is what makes "why am I seeing this?" a database-backed answer rather than a generated sentence.

fl_models, `version INT UNIQUE`, `model_type`, `feature_schema_version`, `parameters BYTEA` (serialised coefficients), `metrics JSONB`, `client_count`, `is_active BOOLEAN`, `created_at`.

fl_rounds, `round_number INT UNIQUE`, `model_version_in FK→fl_models`, `model_version_out FK→fl_models NULL`, `participant_count`, `min_cohort_required`, `clipping_norm NUMERIC`, `noise_multiplier NUMERIC`, `status` (`pending`|`running`|`aggregated`|`refused`|`failed`), `refusal_reason`, `started_at`, `completed_at`.

fl_client_state, `user_id FK UNIQUE`, `client_id`, `last_round_participated`, `local_model_version`, `last_trained_at`, `local_sample_count`, `is_enabled`.

privacy_ledger_entries, `user_id FK`, `fl_round_id FK`, `feature_schema_version`, `raw_update_norm NUMERIC`, `clipped_norm NUMERIC`, `noise_multiplier NUMERIC`, `epsilon_consumed NUMERIC`, `cohort_size INT`, `transmitted_vector JSONB`, `created_at`. Append-only, never updated or deleted except by account deletion. This table is the product feature described in D2.

Further tables. `notifications` and `report_exports` are defined in `ARCHITECTURE_REVISION_JAVA.md` §5. Single-use auth tokens and OAuth identities support US-92 and US-93 (P68, P78); contacts, company notes, offers and goals support US-95, US-96, US-100 and US-101 (P82). Every one carries `user_id NOT NULL` and goes through `UserScopedRepository`.

### 17.3 Relationships

- User → Applications (1:∞). The central ownership edge. `user_id` is NOT NULL with no default and is set server-side from the token; the client can never supply it. Every repository method for applications takes `user_id` as a required parameter, a query that forgets it cannot be written, because the method signature requires it.
- Application → Company (∞:1). Many applications may target the same company (DR-03), so uniqueness is never enforced on (user, company).
- Application → Resume (∞:1, nullable). One resume per application (FR-34); one resume serves many applications, which is what makes §20.7's resume outcome analysis possible. `ON DELETE SET NULL` so deleting a resume does not destroy application history.
- Application → Interviews (1:∞, cascade). Interviews have no meaning without their application, so cascade is correct. `round_number` unique within the application enforces DR-04's variable-length rounds without imposing a fixed count.
- Interview → Questions (1:∞) and → Feedback (1:1). Questions are the atomic unit of topic attribution (DR-09), which is why topic linkage lives here rather than on the interview.
- User → Recommendations → Evidence. Every recommendation carries its provenance rows, satisfying FR-73 structurally rather than by convention.
- User → PrivacyLedger → Round. Each entry is scoped to one user and one round; a user sees only their own entries, and even an administrator sees only aggregate round metadata.

### 17.4 How isolation is enforced (three independent layers)

1. Schema: `user_id NOT NULL` on every owned table, foreign-keyed with cascade.
2. Repository: a `UserScopedRepository` base class whose every method requires `user_id`; controllers cannot reach the persistence layer directly; they call services only.
3. Test: the US-73 suite enumerates every endpoint from the OpenAPI schema and asserts a 404 for cross-user access, so a newly added endpoint that forgets scoping fails CI rather than shipping.

Any one of these can fail without a breach.

### 17.5 Application status enumeration and state machine

Statuses: `Saved`, `Applied`, `OA`, `Interview`, `Final Round`, `Offer`, `Rejected`, `Withdrawn`, `Accepted`, `Declined`.

Ordered progression (used for stage attainment in §20.7): `Saved(0) < Applied(1) < OA(2) < Interview(3) < Final Round(4) < Offer(5)`.
Terminal statuses: `Rejected`, `Withdrawn`, `Accepted`, `Declined`.

```
 Saved ──▶ Applied ──▶ OA ──▶ Interview ──▶ Final Round ──▶ Offer ──┬──▶ Accepted
             │          │         │              │            │     └──▶ Declined
             │          │         │              │            │
             └──────────┴─────────┴──────────────┴────────────┴──▶ Rejected
             └──────────┴─────────┴──────────────┴────────────┴──▶ Withdrawn

 Forward skips are permitted (Applied ──▶ Interview) because employers
 skip stages (DR-04).
 Backward transitions are rejected with 409, except one documented
 exception: any status may move to a *previous* status if a correction
 note is supplied, because users do mis-click. The correction is written
 to history like any other change, so the audit trail stays truthful.
 Terminal ──▶ non-terminal is rejected outright.
```

Encoding this as an explicit transition table (rather than free-text statuses) is what makes FR-21, the funnel, and the dwell-time analytics all computable.

---

## 18. API Design

Base path `/api/v1`. All responses JSON. Errors use a consistent envelope: `{"error": {"code": "...", "message": "...", "field_errors": {...}}}`. All list endpoints support `page`, `size`, `sort`. All authenticated endpoints require `Authorization: Bearer <token>`.

### 18.1 Authentication

| Endpoint | Purpose | Auth | Request | Success | Errors |
|---|---|---|---|---|---|
| `POST /auth/register` | Create account | No | `{email, password, display_name}` | 201 `{user, access_token, refresh_token}` | 400 validation; 409 generic conflict (no enumeration) |
| `POST /auth/login` | Authenticate | No | `{email, password}` | 200 `{user, access_token, refresh_token}` | 401 invalid credentials; 429 rate limited |
| `POST /auth/refresh` | Rotate tokens | Refresh token | `{refresh_token}` | 200 `{access_token, refresh_token}` | 401 invalid/expired/revoked |
| `POST /auth/logout` | End session | Yes | `{refresh_token}` | 204 | 401 |

*Design note:* register and login return the same shape and comparable timing whether or not the email exists, to avoid account enumeration (FR-02).

### 18.2 Users

| Endpoint | Purpose | Auth | Notes |
|---|---|---|---|
| `GET /users/me` | Current profile | Yes | 200 user object |
| `PATCH /users/me` | Update profile | Yes | Partial; email uniqueness checked; 409 on conflict |
| `POST /users/me/password` | Change password | Yes | Requires `current_password`; revokes other sessions |
| `GET /users/me/export` | Full data export | Yes | 200 JSON of everything owned |
| `DELETE /users/me` | Delete account | Yes | Requires typed confirmation; 204 |

### 18.3 Applications

| Endpoint | Purpose | Request / params | Success | Errors |
|---|---|---|---|---|
| `GET /applications` | List | `page, size, sort, status[], stage[], priority[], tag[], work_mode[], q, date_from, date_to, archived` | 200 `{items, total, page, size}` | 401, 422 |
| `POST /applications` | Create | Application payload; `user_id` ignored if supplied | 201 application | 400, 401, 422 |
| `GET /applications/{id}` | Detail | none | 200 with company, resume, history, interviews | 401, 404 for foreign or missing |
| `PATCH /applications/{id}` | Update | Partial payload | 200 | 400, 401, 404, 422 |
| `DELETE /applications/{id}` | Delete | none | 204, cascades | 401, 404 |
| `POST /applications/{id}/status` | Change status | `{to_status, note?}` | 200 with new history entry | 401, 404, 409 invalid transition |
| `GET /applications/{id}/history` | Timeline | none | 200 list, newest first | 401, 404 |

Filtering is server-side. Returning everything and filtering in the browser would violate NFR-20 and leak the full dataset into memory.

### 18.4 Resumes

`GET /resumes` · `POST /resumes` · `GET /resumes/{id}` · `PATCH /resumes/{id}` · `DELETE /resumes/{id}` · `POST /resumes/{id}/file` (multipart, magic-byte validated, 413 over 5 MB, 415 wrong type) · `GET /resumes/{id}/file` (authenticated, expiring, `Content-Disposition: attachment`) · `POST /resumes/{id}/clone` · `GET /resumes/{id}/performance` (returns counts, rates, `sample_size`, and `sufficient_data: bool`; when false, rates are omitted entirely rather than shown with a warning, because a shown number cannot be unseen).

### 18.5 Interviews & questions

`GET /interviews` (filter `application_id`, `upcoming`, `round_type`) · `POST /interviews` · `GET|PATCH|DELETE /interviews/{id}` · `GET|POST /interviews/{id}/questions` · `PATCH|DELETE /interview-questions/{id}` · `GET|PUT /interviews/{id}/feedback` (PUT is upsert, supports draft) · `POST /interview-questions/{id}/promote` (adds to personal bank) · `GET /interviews/{id}/topics` (derived).

### 18.6 Question bank & preparation

`GET /questions` (filter `category`, `topic_id`, `difficulty`, `is_system`, `q`) · `POST /questions` · `PATCH|DELETE /questions/{id}` (own only) · `POST /questions/{id}/fork` · `POST /questions/{id}/attempts` · `GET /questions/{id}/attempts` · `GET /topics` · `GET /topics/graph` (nodes + prerequisite edges) · `GET /me/mastery` (per-topic scores with decay applied and `observation_count`) · `GET|POST /preparation/tasks` · `PATCH|DELETE /preparation/tasks/{id}` · `GET /preparation/progress`.

### 18.7 Analytics

Analytics uses resource-scoped endpoints rather than a single `GET /analytics`: one endpoint returning everything is slow, uncacheable, hard to test, and forces the client to over-fetch for a single chart.

| Endpoint | Returns |
|---|---|
| `GET /analytics/funnel` | Stage counts (ever-reached), still-open count, conversion rates, denominators |
| `GET /analytics/conversions` | The five rates with `numerator`, `denominator`, `sufficient_data` |
| `GET /analytics/timeline` | Weekly application and interview counts |
| `GET /analytics/rejections` | Rejection-stage distribution, separating "no response" |
| `GET /analytics/dwell-times` | Median and IQR per stage, censored count |
| `GET /analytics/topics` | Per-topic performance with question counts |
| `GET /analytics/resumes` | Per-resume outcomes, sample-size gated |
| `GET /analytics/preparation` | Practice volume and mastery trend |
| `GET /analytics/summary` | Small composite for the dashboard strip |

All accept `date_from`, `date_to`, `tag[]`. Every response includes a `sample_size` and a `sufficient_data` boolean per metric (FR-67). The sample-size requirement is enforced in the response schema, not left to the UI.

### 18.8 Recommendations

| Endpoint | Purpose |
|---|---|
| `GET /recommendations` | Ranked active recommendations; `?federated=true\|false` toggles the model contribution and returns `rank_delta` for the lift viewer (US-65) |
| `POST /recommendations/generate` | Force regeneration (normally triggered by data change) |
| `GET /recommendations/{id}/evidence` | The evidence rows behind it |
| `POST /recommendations/{id}/accept` | Creates a linked preparation task |
| `POST /recommendations/{id}/snooze` | `{days}` |
| `POST /recommendations/{id}/dismiss` | `{reason}` |

Every recommendation object carries `evidence_tier`, `observation_count`, `explanation`, and `model_version` (null for Tier A/B/D).

### 18.9 Federated learning & privacy

| Endpoint | Purpose | Auth | Notes |
|---|---|---|---|
| `GET /fl/status` | User's participation state, current model version, last round | User | |
| `POST /fl/opt-in` | `{enabled, posture}`, informed consent | User | Default off |
| `POST /fl/opt-out` | Withdraw | User | Effective next round |
| `GET /fl/ledger` | The user's ledger entries, paginated | User | Includes transmitted vectors |
| `GET /fl/ledger/export` | JSON export | User | |
| `GET /fl/model` | Current global model metadata and parameters | Client/admin | Never contains user data |
| `POST /fl/rounds` | Start a round | Admin | Returns 409 if a round is running |
| `GET /fl/rounds` | Round history with participant counts | User (metadata only) | |
| `GET /fl/schema` | The current feature schema, human-readable | Public | Shown on the opt-in screen; the schema is documentation |

`POST /fl/update` is intentionally absent. Client updates travel over Flower's gRPC channel to the Flower server, not through the application API. Routing updates through the app backend would put user-linked model updates in the same process that holds database credentials and weaken the boundary the design relies on. Recorded as ADR-007.

### 18.10 Further endpoints

Notifications, reports, account recovery, social sign-in, contacts, companies, offers, goals, search and AI features follow the same conventions (base path, error envelope, pagination, bearer auth, 404 on foreign resources). The generated OpenAPI schema is the complete list, and the security suite (P109) enumerates it.

---

## 19. Frontend Architecture

### 19.1 Structure

Feature-first, not type-first. `features/applications/` holds that feature's pages, components, hooks, and API client together; shared primitives live in `components/ui/`. This keeps related code adjacent and makes parallel work on different features nearly conflict-free, which matters directly for the GitHub contribution requirement.

### 19.2 State strategy

| Kind of state | Handled by |
|---|---|
| Server data | TanStack Query (caching, invalidation, background refetch) |
| Auth session | React Context + secure token storage |
| Form state | React Hook Form + Zod |
| Filter/sort/pagination | URL search params (bookmarkable, shareable, survives refresh) |
| Ephemeral UI (modals, toasts) | Local `useState` / a small context |

No Redux. There is no state complex enough to justify it.

### 19.3 Pages

| Page | Route | Purpose | Main components | Data required | User actions |
|---|---|---|---|---|---|
| Login | `/login` | Authenticate | `AuthCard`, `LoginForm` | none | Submit, link to register |
| Register | `/register` | Create account | `AuthCard`, `RegisterForm`, `PasswordStrength` | none | Submit |
| Dashboard | `/` | Answer "what should I do today?" | `PipelineStrip`, `ActionQueue`, `UpcomingInterviews`, `RecentActivity` | `/analytics/summary`, stalled apps, pending debriefs, due tasks, upcoming interviews | Act on a queue item, quick-add application |
| Applications | `/applications` | Manage the campaign | `DataTable`, `FilterBar`, `StatusBadge`, `ApplicationFormModal` | Paginated applications, companies | Create, filter, search, sort, open detail |
| Application Detail | `/applications/:id` | Everything about one application | `DetailHeader`, `FieldPanel`, `StatusChanger`, `Timeline`, `InterviewList`, `ResumeChip`, `NotesPanel` | Application with relations | Edit, change status, add interview, attach resume, delete |
| Resumes | `/resumes` | Version library | `ResumeCard` grid, `UploadDropzone`, `VersionBadge` | Resumes with usage counts | Create, upload, clone, set default, delete |
| Resume Detail | `/resumes/:id` | One version | `SectionEditor`, `FilePreview`, `UsageList`, `PerformancePanel` | Resume, sections, linked applications, performance | Edit sections, replace file, download, clone |
| Interviews | `/interviews` | Upcoming and past | `InterviewTable`, `UpcomingPanel`, `DebriefPrompt` | Interviews with application context | Create, open, start debrief |
| Interview Detail | `/interviews/:id` | One round | `InterviewHeader`, `QuestionList`, `QuestionForm`, `FeedbackPanel`, `TopicChips` | Interview, questions, feedback, derived topics | Add questions, complete debrief, promote question to bank |
| Interview Prep | `/prep` | Practice hub | `TopicGrid`, `MasteryLegend`, `TaskList`, `ProgressBars`, `ReviewQueue` | Mastery scores, tasks, progress | Start practice, complete task, open topic |
| Question Bank | `/questions` | Browse and practise | `QuestionTable`, `CategoryFilter`, `PracticeModal`, `ConfidenceRating` | Questions, attempts | Filter, practise, record attempt, add/fork |
| Analytics | `/analytics` | Campaign performance | `FunnelChart`, `ConversionCards`, `TimelineChart`, `RejectionChart`, `TopicPerformance`, `SampleSizeBadge`, `DateRangePicker` | All analytics endpoints | Change range, filter by tag, drill into a stage |
| Recommendations | `/recommendations` | What to do next | `RecommendationCard`, `EvidenceTierBadge`, `EvidenceDrawer`, `FederatedLiftToggle` | Recommendations with evidence | Accept, snooze, dismiss, toggle federated, expand evidence |
| Privacy & FL | `/privacy` | Control and verify | `OptInPanel`, `PostureSelector`, `PrivacyLedgerTable`, `VectorInspector`, `BudgetMeter`, `RoundHistory`, `WhatWasNotSent` | FL status, ledger, rounds, schema | Opt in/out, choose posture, inspect a vector, export ledger |
| Profile | `/profile` | Account | `ProfileForm`, `PasswordChange`, `DataExport`, `DangerZone` | Current user | Update, change password, export, delete account |
| Board | `/applications/board` | Kanban view of the pipeline | `KanbanColumn`, `ApplicationCard` | Applications by status | Drag between legal stages |
| Companies, Contacts, Offers, Goals | `/companies/:id`, `/contacts`, `/offers`, `/goals` | Job-search workflow (EPIC 2) | Company page, contact list, offer comparison, goal tracker and heatmap | The user's own records | Create, edit, compare, track |
| Coach and AI tools | `/coach` and in-context panels | AI assistance (EPIC 12) | Chat, parser, match, mock interview, story bank, prep pack, drafts | The user's own records | Ask, accept or discard suggestions |
| Notifications, Reports | `/notifications`, `/reports` | EPIC 10 and US-80 | Notification list, export form | Notifications, export history | Mark read, export, download |
| Landing, onboarding, help | `/welcome`, `/onboarding`, `/help`, `/privacy-policy` | First run and disclosure | Landing, onboarding steps, tour, help and policy pages | none | Sign up, set goals, load sample data |
| 404 | `*` | Not found | `NotFound` | none | Return home |

### 19.4 Cross-cutting frontend concerns

- Route protection: `ProtectedRoute` wrapper redirects unauthenticated users, preserving the intended destination.
- Token refresh: a single-flight interceptor in the TanStack Query client; concurrent 401s queue behind one refresh.
- Error boundaries: one global boundary plus per-route boundaries so a broken chart does not blank the page.
- Loading: skeletons matching final layout, not spinners, because layout shift is the main cause of perceived slowness.
- Optimistic updates: on status change and task completion, with rollback on failure.
- Type safety: API types generated from the backend OpenAPI schema into `types/api.ts`, regenerated in CI so drift breaks the build rather than production.

---

## 20. UI/UX Plan

### 20.1 Design direction

The UI is dense and plain, like an internal dashboard rather than a landing page. Concretely:

- Neutral base with a single accent. Slate/zinc greys carrying the interface; one accent colour used only for primary actions and the active nav item. Status colour is confined to badges.
- A typographic scale. A distinctive text face (e.g. Söhne, Geist, or IBM Plex Sans) paired with a monospace face for numbers, dates, and IDs, tabular figures throughout so columns of numbers align.
- Sharp, not soft. Small radii (4-6 px), hairline borders, near-flat surfaces. No large shadows, no gradients, no glassmorphism.
- Density by default with a comfortable-mode toggle. The primary user has 100+ applications; a card grid with 3 items per row is the wrong shape for that.
- Charts styled down. Muted palette, no drop shadows, gridlines at 10% opacity, direct labelling in preference to legends.

### 20.2 Navigation

Persistent left sidebar, collapsible to icons, collapsing to a bottom bar below 768 px:

```
  Cohort
  ------------------
  Dashboard
  Applications        12
  Resumes
  Interviews           3
  Interview Prep
  Analytics
  Recommendations      5
  Privacy / FL
  ------------------
  Profile
```

Counts are live: open applications, upcoming interviews, active recommendations.

### 20.3 Component patterns

| Pattern | Specification |
|---|---|
| Data table | Sticky header, zebra-free with hairline row borders, sortable headers with direction indicator, row hover, click opens a detail drawer (list context preserved) with a "full page" affordance, 25 rows per page |
| Status badge | Text + subtle background + a leading dot; never colour alone (NFR-47). Saved (grey), Applied (blue), OA (indigo), Interview (violet), Final (amber), Offer (green), Rejected (red), Withdrawn (grey outline) |
| Filter bar | Inline chips above the table showing active filters, each removable; "Clear all"; state in the URL |
| Dashboard pipeline strip | Single horizontal band of stage counts with connecting arrows and a subtle width-proportional fill, the funnel as a header, not a chart |
| Action queue | Ranked list, each row an icon, one-line description, and a single button. Max 7 items. Empty state: "Nothing needs your attention today." |
| Progress bar | Track + fill + numeric label; on the topic grid, mastery shown as a filled ring with an accessible numeric value |
| Forms | Single column, logical grouping, inline validation on blur, disabled submit while pending, field-level server errors, unsaved-changes guard |
| Modals | Focus trapped and restored, Esc closes, backdrop click closes non-destructive dialogs only, max 560 px wide |
| Confirmation dialogs | Name the object ("Delete your application to Stripe, Backend Engineer?"), state the consequence, destructive button on the right and styled as destructive |
| Empty states | Illustration or icon, one sentence of explanation, one primary action. Different copy for "no data yet" vs "no results for this filter" |
| Loading | Skeletons matching final layout; charts show axes with a shimmering plot area |
| Error states | What happened, what to do, a retry button. Never a raw status code |
| Toasts | Bottom-right, auto-dismiss at 4 s, with an undo action where the operation is reversible |

### 20.4 Main screens

1. Dashboard action queue: the product acts as a tool, not a report.
2. Application detail timeline: history is captured, not overwritten.
3. Topic mastery grid: a colour-and-value grid of ~50 topics, with prerequisite edges revealed on hover.
4. Recommendation card with the evidence drawer open.
5. Privacy Ledger with a vector expanded.

### 20.5 Responsive behaviour

| Breakpoint | Behaviour |
|---|---|
| ≥1280 px | Full sidebar, full table columns, side-by-side detail drawer |
| 768-1279 px | Icon sidebar, secondary table columns dropped, drawer becomes full-width sheet |
| <768 px | Bottom nav, table becomes a card list, charts stack, filters move into a sheet |

### 20.6 Accessibility commitments

WCAG 2.1 AA contrast; full keyboard operability with visible focus; labelled inputs with `aria-describedby` error linkage; charts accompanied by a data table view; status conveyed by text as well as colour; `prefers-reduced-motion` respected; axe-core in CI with zero critical violations on the main pages.

### 20.7 Analytics: metric definitions

Every displayed number needs an unambiguous formula, or the analytics are not verifiable. These definitions are pinned by golden-file tests.

Stage attainment. An application has *reached* stage S if `application_status_history` contains any row with `to_status = S` or a status ordered after S. Computed from history, never from current status: an application currently `Rejected` that previously reached `Interview` must count in the interview stage, or the funnel is meaningless (DR-01).

Funnel counts.
- `applied = COUNT(applications WHERE status != 'Saved')`
- `reached_oa = COUNT(applications that ever reached OA or beyond)`
- `reached_interview`, `reached_final`, `reached_offer`, same construction.

Conversion rates. Each rate is *reached next stage ÷ reached this stage*, over resolved applications only. An application still awaiting a response is neither a success nor a failure and is reported separately as `still_open` (DR-02, DR-05).
- `application_to_oa = reached_oa / applied_resolved`
- `oa_to_interview = reached_interview / reached_oa_resolved`
- `interview_to_final = reached_final / reached_interview_resolved`
- `final_to_offer = reached_offer / reached_final_resolved`
- `overall_success = reached_offer / applied_resolved`

Every rate returns `{value, numerator, denominator, sufficient_data}`, where `sufficient_data = denominator >= 5`. When false, the UI shows the counts and suppresses the percentage (FR-67).

Days in stage. `now() - stage_entered_at`, where `stage_entered_at` is the `changed_at` of the most recent history row.

Median dwell time per stage. Median of `(next_transition_time - stage_entry_time)` over completed transitions only. Applications currently sitting in a stage are right-censored and excluded from the median, reported separately as `censored_count`. Reported with IQR; suppressed below 5 completed observations.

Stall flag. An application is stalled when `days_in_stage > max(user_median_dwell[stage] × 1.5, system_default[stage])`, requiring ≥5 observations for the personal median; otherwise the system default applies and the UI says which basis was used.

Rejection stage distribution. For each rejected application, the last stage reached before the rejection row. Applications with no terminal status and no activity for 60 days are reported as `no_response`, never merged into rejections (DR-02).

Topic performance. For topic *t*: recency-weighted mean of `self_rating` across `interview_questions` with `topic_id = t`, weight `exp(-λ · days_ago)` with λ chosen for a 90-day half-life. Requires ≥3 questions; otherwise reported as insufficient data.

Resume outcome rate. For resume *r*: `applications using r that reached ≥ Interview ÷ resolved applications using r`. Suppressed below 5 (DR-14, FR-37), and always displayed with the standing note that differences may reflect role, timing, or company mix rather than the document.

Preparation progress. Per category: `questions with ≥1 attempt at confidence ≥4 ÷ total questions attempted in that category`. Measured against attempted rather than available questions, so the denominator is the user's own scope of work, not the size of the seed bank.

---

## 21. ML Recommendation Engine

### 21.1 Design philosophy

The engine is rules-first with a learned re-ranker, not a black box. Three reasons:

1. Explainability is a requirement, not a nice-to-have (FR-73). A rule that fires on identifiable evidence rows can explain itself exactly; a learned model can only be rationalised after the fact.
2. Cold start is the normal case. A new user has zero interviews. A pure ML system produces nothing useful for weeks; rules produce something sensible on day one and improve as evidence accumulates.
3. The FL contribution stays visible. Because the learned model only *re-ranks* candidates that the rules generated, the federated contribution can be isolated, measured, and toggled (US-65). If ML generated the candidates, the lift viewer would be impossible to build.

### 21.2 Pipeline

```
 [1] Candidate generation   rules over the user's own data
             │              → 10-30 candidates, each with evidence rows
             ▼
 [2] Evidence tiering       A (n≥5 personal) · B (2-4 personal)
             │              C (federated model) · D (heuristic)
             ▼
 [3] Base scoring           severity × recency × actionability × tier weight
             │
             ▼
 [4] Federated re-rank      global model adjusts scores  ← toggleable
             │              (skipped entirely when federated=false)
             ▼
 [5] Diversity filter       max 2 per type, max 1 per topic family
             │
             ▼
 [6] Template rendering     copy from the registry, phrasing-linted
             │
             ▼
 [7] Top 5 + evidence drawer
```

### 21.3 Candidate generators

| Generator | Fires when | Evidence collected |
|---|---|---|
| `WeakTopicGenerator` | A topic's decayed mastery < 0.4 with ≥2 observations | Interview questions with low self-rating; low-confidence practice attempts |
| `PrerequisiteGapGenerator` | A weak topic has a prerequisite with lower mastery | The prerequisite chain plus both mastery scores |
| `DecayGenerator` | Mastery has fallen below 0.5 purely through time decay | Last-practised date, current vs peak score |
| `CoverageGenerator` | An upcoming interview's likely topics have no practice history | The scheduled interview, topics with zero attempts |
| `StalledApplicationGenerator` | Days in stage exceeds the threshold (§20.7) | The application, dwell time, personal median, sample size |
| `MissingDebriefGenerator` | An interview date has passed with no feedback recorded | The interview record |
| `CadenceGenerator` | Application volume in the last 14 days is far below the user's own median | Weekly application counts |
| `ResumePatternGenerator` | Resume outcome rates differ and both versions clear the sample threshold | Per-resume counts and rates |
| `BaselineGenerator` (Tier D) | The user has fewer than 3 interviews logged | None, heuristic, and says so |

### 21.4 Topic mastery formula

For user *u* and topic *t*:

Raw evidence score. Combine two sources, each normalised to [0,1]:

```
practice_component(u,t) = Σ_a w_a · (confidence_a - 1)/4      /   Σ_a w_a
interview_component(u,t) = Σ_q w_q · (self_rating_q - 1)/4     /   Σ_q w_q

where w = exp(-ln(2) · days_ago / H)      (exponential recency weight)
      H = 90 days for practice, 120 days for interview evidence
```

Blend, favouring real interviews over self-directed practice, because interview evidence is scarcer and more diagnostic:

```
raw(u,t) = 0.4 · practice_component + 0.6 · interview_component
           (falls back to whichever component has evidence when only one does)
```

Time decay on the blended score, so a topic untouched for months is not reported as mastered (DR-16):

```
Δ = days since last evidence of any kind for (u,t)
mastery(u,t) = raw(u,t) · exp(-ln(2) · Δ / H_diff)

H_diff = 60 days (hard topics) · 90 (medium) · 120 (easy)
```

Confidence gating. With `observation_count < 2`, the score is stored but reported as *insufficient evidence* rather than as a number. This prevents a single bad question from branding a topic as a weakness.

The whole function is pure, seeded, and pinned by golden-file tests against a fixed fixture (NFR-51), so a refactor that changes recommendations by accident fails CI.

### 21.5 Prerequisite root-cause routing

```
routeToRootCause(topic t, mastery M, graph G, depth d = 0):
    if d >= 2: return t                       # cap for comprehensibility
    weakest_prereq = argmin{ M[p] : p ∈ G.prerequisites(t) }
    if weakest_prereq exists and M[weakest_prereq] < M[t] - 0.15:
        return routeToRootCause(weakest_prereq, M, G, d+1)
    return t
```

The 0.15 margin prevents routing on noise. The rendered recommendation shows the chain: *"Shortest-path questions have gone poorly (3 rounds). Your graph-traversal mastery is lower still (0.31 vs 0.46), so start there, BFS and DFS before Dijkstra."*

### 21.6 Evidence tiers and scoring

```
score = severity × recency × actionability × tier_weight × federated_adjustment

severity           = 1 - mastery                       ∈ [0,1]
recency            = exp(-ln(2) · days_since_evidence / 45)
actionability      = 1.0 (specific topic) | 0.6 (broad) | 0.8 (application action)
tier_weight        = A:1.0 · B:0.7 · C:0.6 · D:0.4
federated_adjust   = 1.0 when federated ranking is off
                   = 0.75 + 0.5 · σ(global_model · features) when on
```

The federated adjustment is bounded in [0.75, 1.25]. This lets the shared model reorder recommendations but never create or suppress one, and a user's own evidence always dominates. It also keeps the movement shown by the lift viewer small.

### 21.7 Template registry and phrasing enforcement

All user-facing recommendation copy lives in one registry module. Each template declares its type, required evidence fields, and both the headline (imperative) and explanation (associative) strings.

The test `test_no_causal_language` iterates every template and fails on any match against the prohibited list: *guarantees, will get you, ensures, because of this you were rejected, caused, proven to, always leads to, you failed because*. A second test asserts every template renders with its declared evidence fields present. A third asserts no template renders without an explanation.

The phrasing rule is enforced as a build gate, so a banned phrase cannot reach a release.

### 21.8 The machine-learning model

| Property | Choice |
|---|---|
| Task | Binary classification: given a user's preparation state before an interview round, predict whether that round was self-rated ≥4 |
| Framing | The model predicts self-reported confidence, not hiring success. DR-08 makes this distinction mandatory, and it is stated in the UI and the report. |
| Model | `sklearn.linear_model.SGDClassifier(loss='log_loss')`, logistic regression |
| Why linear | Parameters are a flat, fixed-length float array, which is what FedAvg averages; coefficients are inspectable, so the global model can be discussed rather than merely reported; it works with very little data per client, where anything deeper would simply memorise |
| Training example | One completed interview round |
| Features | The 24 features in §22.2, computed as of that round's date |
| Point-in-time correctness | Feature extraction takes an `as_of` timestamp and excludes all later records. Without this, mastery computed today would leak the outcome into a round from three months ago (label leakage) |
| Parameters exchanged | `coef_` (24 floats) + `intercept_` (1 float) = 25 floats per round, per client |

### 21.9 Evaluation

- Offline: stratified k-fold on the synthetic cohort, reporting AUC, accuracy, and Brier score against a majority-class baseline. If the model does not beat the baseline, that is reported as the finding, not hidden.
- Ablation: rules-only vs rules + local model vs rules + federated model, which is the quantitative backing for the Federated Lift feature.
- Ranking quality: overlap between the top-5 recommendations and the topics that actually appeared in the user's subsequent interviews, measured on held-out synthetic timelines.
- Reporting: the report states sample sizes, states that synthetic data validates the *mechanism* rather than the *finding*, and does not present synthetic-data metrics as evidence about real job seekers.

---

## 22. Federated Learning Architecture

### 22.1 The learning task

What is learned: the association between a user's *preparation state* (a 24-dimensional derived feature vector) and *self-reported interview round performance*.

Why this task. It satisfies DR-19: it is learnable from a fixed numeric vector, needs no raw text and requires no cross-user joins. Knowing which preparation patterns correlate with better-rated rounds is a cross-user insight that no single user can obtain alone, since each has only a handful of interviews. The label is self-reported, which is disclosed wherever the model is surfaced.

Rejected alternatives, and why: learning over question *text* (would require raw text transmission, violates the core constraint); predicting hiring outcomes (labels too sparse, too confounded, and would invite exactly the causal claims §14/C6 forbids); learning company-specific patterns (would identify companies and, transitively, users).

### 22.2 Feature schema v1: everything that leaves a client

Fixed length, all `float32`, all bounded to [0,1] or [-1,1], all derived locally, all bucketed or normalised so that no raw count is recoverable. Published at `GET /fl/schema` and shown verbatim on the opt-in screen.

| # | Feature | Definition | Bounding |
|---:|---|---|---|
| 1 | `mastery_mean` | Mean decayed mastery over topics with evidence | [0,1] |
| 2 | `mastery_min` | Minimum mastery among tested topics | [0,1] |
| 3 | `mastery_std` | Standard deviation of mastery | scaled [0,1] |
| 4 | `practice_volume` | `log1p(attempts)/log1p(500)` | clipped [0,1] |
| 5 | `topic_breadth` | Distinct topics practised ÷ taxonomy size | [0,1] |
| 6 | `practice_recency` | `exp(-days_since_last_attempt/30)` | [0,1] |
| 7 | `practice_consistency` | Fraction of the last 8 weeks with ≥1 attempt | [0,1] |
| 8 | `confidence_mean` | Mean confidence over the last 10 attempts, rescaled | [0,1] |
| 9 | `confidence_trend` | Clipped slope of confidence over the last 20 attempts | [-1,1] |
| 10 | `hard_ratio` | Share of attempts on hard questions | [0,1] |
| 11 | `share_dsa` | Category mix, DSA | [0,1] |
| 12 | `share_technical` | Category mix, technical | [0,1] |
| 13 | `share_behavioural` | Category mix, behavioural | [0,1] |
| 14 | `share_system_design` | Category mix, system design | [0,1] |
| 15 | `task_completion_rate` | Completed ÷ created preparation tasks | [0,1] |
| 16 | `prep_lead_time` | `min(days of prep before this round, 60)/60` | [0,1] |
| 17 | `interview_experience` | `log1p(prior rounds)/log1p(50)` | [0,1] |
| 18 | `rounds_per_application` | Mean rounds per application ÷ 6 | clipped [0,1] |
| 19 | `recent_self_rating` | Mean self-rating over the last 5 rounds, rescaled | [0,1] |
| 20 | `coverage_ratio` | Share of this round's topics practised beforehand | [0,1] |
| 21 | `repeat_topic_ratio` | Share of this round's topics seen in earlier rounds | [0,1] |
| 22 | `application_volume` | `log1p(applications)/log1p(300)` | clipped [0,1] |
| 23 | `resume_variants` | `min(resume versions, 5)/5` | [0,1] |
| 24 | `round_type_ordinal` | Encoded round type (OA…final), normalised | [0,1] |

Not included: no strings of any kind, no user or record identifiers, no company names, no job titles, no dates or timestamps, no free text, no interviewer names, no resume content, no question text, no salary, no location. The serialisation test asserts the payload is exactly 24 floats with these names, in this order, and fails on any addition.

Bucketing and log-scaling. A raw count of 137 applications is close to a fingerprint; `log1p(137)/log1p(300)` is not. Every count-like feature is compressed for this reason, not for model performance.

### 22.3 Client architecture and isolation

In the implemented architecture the Java API exports the allow-listed features into the fl-service's own store, and the fl-service's database role can write only the three FL bookkeeping tables (`ARCHITECTURE_REVISION_JAVA.md` §3). The isolation properties below hold for each client process inside that service.

Each FL client is a separate container running its own process, with:

- Its own Postgres schema (`client_1`, `client_2`, …) and its own database role granted `USAGE` on that schema alone. This is enforced by Postgres, not by application code.
- Its own feature extractor, reading only from its own schema.
- Its own model instance and training loop.
- Its own clipping and noise step.
- A gRPC connection to the Flower server carrying parameters only.

The Flower server container receives no `DATABASE_URL`, no credentials, and no volume mount of user data. A CI job asserts this by inspecting the container's environment and failing if any database variable is present (NFR-11). This makes the privacy claim a testable property (D1).

### 22.4 The algorithm

```
Round r, coordinated by the Flower server:

  1. Server publishes global parameters θ(r-1) to selected clients.
  2. Each opted-in client i, locally and in isolation:
       a. X_i, y_i ← extract_features(own schema only, point-in-time correct)
       b. if n_i < 5 examples:  abstain from this round, log the reason
       c. θ_i ← fit(θ(r-1), X_i, y_i)          # warm-started local training
       d. Δ_i ← θ_i - θ(r-1)
       e. Δ̂_i ← Δ_i · min(1, S / ‖Δ_i‖₂)      # L2 clipping, S = 1.0
       f. Δ̃_i ← Δ̂_i + N(0, σ²S²I)             # Gaussian noise, σ = 1.0
       g. transmit Δ̃_i  (25 floats)  and write a Privacy Ledger entry
  3. Server:
       a. if |participants| < k (k = 3): REFUSE the round, log the reason,
          keep θ(r-1) active. No aggregation over a cohort too small to hide in.
       b. discard any Δ̃_i with ‖Δ̃_i‖₂ > S · τ  (τ = 3, poisoning defence)
       c. θ(r) ← θ(r-1) + (1/|participants|) · Σ Δ̃_i     # uniform-weight FedAvg
       d. version, persist, and activate θ(r); record round metadata
```

Why uniform weighting rather than sample-count weighting. Standard FedAvg weights each client by its number of local examples. That would require every client to transmit its exact sample count, a real leak, since sample count is job-search volume, which is personal. Uniform weighting costs some statistical efficiency and buys the removal of an entire side channel. Trade-off recorded as ADR-009.

Parameters and their justification: S = 1.0 (bounds any single client's influence, which is simultaneously the DP sensitivity bound and the poisoning bound); σ = 1.0 (a conservative default from the DP-SGD literature, held fixed per §14/C7, not tuned to improve the accuracy chart); k = 3 (the minimum at which averaging provides any anonymity at all; production would use 50+, and the report says so); τ = 3 (rejects updates more than 3× the clipping norm, which after clipping should be impossible for a well-behaved client).

### 22.5 Prototype FL vs production FL

| Dimension | This project | Production |
|---|---|---|
| Clients | 5-8 containers: 1 bound to the demo user, 4-7 synthetic-cohort clients | Thousands of real user devices or per-tenant enclaves |
| Client location | Docker containers on one host, with separate schemas and roles | Browser (WASM) or user-controlled agent |
| Data isolation | Postgres schema + role separation, verified in CI | Physical device isolation |
| Selection | All eligible clients each round | Random sampling with availability and fairness constraints |
| Rounds | 10-30, manually or schedule triggered | Continuous, automated |
| Privacy | Local DP (clip + Gaussian noise), min cohort k=3 | Secure aggregation (SecAgg) + central DP + formal accounting with a privacy engine |
| Aggregation trust | Server sees individual noised updates | Server sees only the encrypted sum; individual updates are cryptographically hidden |
| Poisoning defence | Norm filtering | Krum / trimmed mean aggregation + client reputation |
| Model | Logistic regression, 25 parameters | Larger models, personalisation layers, per-cohort specialisation |
| Accounting | Approximate per-round ε, displayed as approximate | Rigorous RDP composition with a formal ε-δ guarantee |

The prototype reduces scale, not fidelity. The architectural properties that matter are present: data never moves, updates are clipped and noised, small cohorts are refused, and the server has no data access. What is reduced is the number of clients, the size of the model and the cryptographic sophistication.

### 22.6 How the prototype demonstrates the concept without distributed infrastructure

The obstacle is real: a student project has one real user and no cohort. Three mechanisms handle this.

1. Synthetic cohort clients. A seeded generator produces 4-7 job-search histories with different distributions (a DSA-heavy candidate, a behavioural-heavy candidate, a high-volume applicant, a low-volume one). This makes the data non-IID, matching DR-20, and gives a realistic convergence curve. Each history is loaded into its own client schema. Everything is labelled synthetic in the UI, the report and the poster.
2. Containerised separation. The clients are separate processes with separate credentials, so the privacy property can be checked directly. The demo includes opening a shell in the FL server container and showing there are no database credentials in it.
3. Flower simulation mode as a fallback. If containerised deployment cannot be finished before the deadline, `flwr.simulation` runs the same client code in-process. This is a documented degradation: the report would state that the isolation property was shown in the containerised configuration and the convergence results in simulation.

The five-minute demo sequence: show the Privacy page and a user's feature vector → start round *r* → watch clients train locally → show the server refusing a 2-client round → show a 5-client round aggregating → show the new model version live → open the Privacy Ledger and expand the exact 25 floats that were sent → toggle Federated Lift and watch the recommendations re-rank.

### 22.7 Threat model

Defended:
- A compromised FL server cannot read user data; it has no credentials and no data path (D1, NFR-11).
- Network interception yields clipped, noised parameters, not records.
- A single malicious client's influence is bounded by the clipping norm and filtered by τ.
- Small-cohort deanonymisation is prevented by the k-refusal rule.
- Gradient inversion is materially impeded by clipping plus noise (though not eliminated, see below).

Not defended:
- The server sees individual noised updates. Without secure aggregation, a curious server could attempt inference on a single client's update; the noise makes this hard, not impossible. SecAgg is Future Scope.
- Sybil clients. Many colluding fake clients could still bias the model. Real deployments need identity and reputation systems.
- Epsilon accounting is approximate. Per-round ε is reported under stated assumptions, without a rigorous composition engine. The UI and the report both say "approximate".
- A compromised application backend would expose user data directly. FL protects the cross-user learning channel, not the application itself; that is what §23's controls are for.
- Membership inference over many rounds remains theoretically possible with a fixed cohort. Mitigated by the budget cap, not eliminated.

### 22.8 Evaluation plan

Report: global model AUC per round; per-client AUC (to show non-IID divergence, per DR-20); a convergence curve over 20+ rounds; an ablation of noise levels σ ∈ {0, 0.5, 1.0, 2.0} against utility, which is the privacy-utility trade-off from §14/C7 made concrete; a centralised-training upper bound for comparison; and refused-round counts. Every number is labelled as derived from synthetic cohorts.

---

## 23. Privacy & Security

### 23.1 Data classification

See §16.6 for the full crossing matrix. Summarised by sensitivity:

| Class | Contents | Handling |
|---|---|---|
| Critical | Password hashes, resume files, interview question text, notes, interviewer names | Never leaves the app boundary; never in any model feature; never logged; encrypted at rest in deployment |
| Sensitive | Applications, companies, statuses, outcomes, feedback | User-scoped only; never aggregated across users; excluded from FL |
| Derived-shareable | Bucketed, normalised numeric features (§22.2) | Only after clipping and noise; only with opt-in; user-inspectable in the ledger |
| Operational | Round metadata, participant counts, model versions | Visible as aggregate only; contains no user reference |

### 23.2 Security controls by layer

| Layer | Controls |
|---|---|
| Identity | Argon2id (m=64 MiB, t=3, p=4); ≥10-char policy with a blocklist; no enumeration on register or login; auth rate limiting |
| Session | Access token ≤15 min; refresh token ≤7 days, hashed at rest, rotated on use, revocable; logout revokes server-side; password change revokes all other sessions |
| Authorisation | Repository-layer `user_id` scoping; 404 on foreign resources; admin role cannot read user content, only aggregates |
| Input | Bean Validation rejecting unknown fields; length caps on every text field; enum validation; server-side authority over all client validation |
| Files | 5 MB cap; extension + MIME + magic-byte validation; UUID filenames; stored outside served paths; authenticated expiring download; no server-side rendering or parsing of uploads |
| Database | Parameterised queries only (a CI grep gate rejects f-string SQL); least-privilege app role; FL clients on separate schemas with separate roles |
| Transport | HTTPS in deployment; HSTS; CORS restricted to the known origin |
| Headers | CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy |
| Logging | Structured logs with an explicit redaction list (password, token, email, file path); no request bodies logged on auth routes |
| Secrets | Environment only; `.env` git-ignored; `.env.example` committed with placeholders; CI secret scanning |
| FL | Opt-in default off; clipping; noise; min cohort; norm filtering; no server credentials; ledger |

### 23.3 Security risk register

| Risk | Likelihood | Impact | Mitigation | Verified by |
|---|---|---|---|---|
| Unauthorised access to another user's data | Medium | Critical | Repository-layer scoping; 404 responses; three-layer enforcement (§17.4) | US-73 suite enumerated from OpenAPI |
| Broken object-level authorisation on a new endpoint | Medium-high (the most likely real breach) | Critical | Test suite generated from the schema, so a new endpoint without scoping fails CI | Automated |
| Weak user passwords | High | High | Policy + blocklist + strength meter; Argon2id limits offline cracking | Unit test |
| JWT theft or replay | Low | High | Short access TTL; refresh rotation with reuse detection; server-side revocation; HTTPS | Integration test |
| Malicious resume upload | Medium | High | Magic-byte validation; size cap; UUID names; stored outside web root; never parsed or rendered server-side | Test uploading a renamed executable |
| SQL injection | Low | Critical | ORM parameterisation; CI gate against raw SQL interpolation | CI gate + review |
| XSS | Low | High | React escaping by default; no `dangerouslySetInnerHTML`; CSP; server-side length and content validation | Review + CSP header test |
| CSRF | Low | Medium | Bearer tokens in headers rather than cookies for the API; SameSite on any cookie used | Review |
| API abuse / brute force | Medium | Medium | Rate limiting on auth and generation endpoints; pagination caps | Integration test |
| Account enumeration | Medium | Medium | Identical responses and comparable timing for existing and non-existing emails | Test |
| Data leakage through analytics | Low | High | All analytics user-scoped; no cross-user endpoint exists to leak through | Review + test |
| Gradient inversion of FL updates | Low-medium | High | Clipping + Gaussian noise; small model; bucketed features; disclosed as impeded-not-eliminated | Ablation experiment |
| Deanonymisation via small cohort | Medium | High | k-refusal rule (k=3) enforced server-side | Unit test: a 2-client round is refused |
| Model poisoning | Low (closed cohort) | Medium | Norm filtering at τ=3; clipping bounds any single client; disclosed as insufficient against Sybils | Unit test with a crafted large update |
| Sample-count leakage via FedAvg weighting | Medium | Medium | Uniform weighting instead of count weighting (ADR-009) | Review + payload test |
| Secret committed to the repository | Medium | High | `.gitignore`, `.env.example` only, CI secret scan, pre-commit hook | CI |
| Dependency vulnerability | Medium | Medium | Dependabot; `pip-audit` and `npm audit` in CI; pinned versions | CI |
| Orphaned data after account deletion | Medium | High | Cascade constraints + a post-deletion orphan-check test across every table | Integration test |

### 23.4 Explicitly out of scope

Multi-factor authentication; secure aggregation; formal DP accounting; penetration testing by a third party; SOC2/GDPR certification (though the design follows GDPR principles: minimisation, purpose limitation, right to export, right to erasure). Each is listed in Future Scope.

---

## 24. Testing Strategy

### 24.1 Shape of the suite

Mostly unit tests over pure logic (mastery, scoring, state machine, metrics, FL arithmetic), then API tests over the real contract, a few integration tests over critical journeys, and browser automation limited to Playwright screenshots for the responsive review (P72). Manual scripted walkthroughs cover the remaining journeys.

### 24.2 By category

Unit (backend). Password hashing and verification; token creation, expiry, rotation, reuse detection; status state machine (parametrised over every transition pair, valid and invalid); dwell-time and days-in-stage helpers; funnel stage attainment from synthetic history; every conversion rate against hand-computed fixtures; mastery scoring including decay and the insufficient-evidence gate; prerequisite routing including the depth cap and the margin; recommendation scoring and diversity filtering; template rendering and the prohibited-phrasing linter; feature extraction including point-in-time correctness (a test that fails if any post-`as_of` record influences the vector); clipping arithmetic; noise application; FedAvg averaging against a hand-computed example; the min-cohort refusal; the norm filter.

API. Every endpoint × {happy path, validation failure, unauthenticated, cross-user}. Pagination boundaries. Filter combinations. Upload rejection cases. Invalid status transitions returning 409. Response schemas validated against the OpenAPI spec so drift is caught automatically.

Frontend (RTL + Vitest). Form validation and submission for login, register, application, interview, and debrief; `DataTable` rendering, sorting, pagination, and empty state; `StatusBadge` rendering every status with text as well as colour; `ProtectedRoute` redirect; `ConfirmDialog` requiring explicit confirmation; error and loading states rendering distinctly; the recommendation card rendering its tier badge and evidence drawer; the ledger vector inspector rendering values.

Integration. Register → login → create application → change status → verify history. Create application → attach resume → add interview → add questions → complete debrief → verify derived topics. Practice attempts → mastery recomputation → recommendation generation → accept → task created. A full FL round: seed clients → extract → train → clip → noise → aggregate → version → ledger entries written. Account deletion → orphan check across every table. All run against a real Postgres via testcontainers, never SQLite, because the constraints under test exist only in Postgres.

Security. The US-73 suite, enumerated from the OpenAPI schema so coverage cannot silently lapse: unauthenticated access to every non-public endpoint returns 401; cross-user access to every owned resource returns 404; the client cannot set `user_id` on create; the malicious-upload test; the SQL-injection-shaped input test; the auth rate-limit test; the no-enumeration test; the FL server credential-absence check.

FL-specific. Payload schema conformance (exactly 24 named floats, no strings); opted-out clients produce no update; a client with fewer than 5 examples abstains; clipped norms respect S; noised vectors differ from raw; a 2-client round is refused and logged; a crafted over-norm update is discarded; a failed round leaves the previous model active and servable; deterministic results under a fixed seed.

### 24.3 What is tested in each sprint

| Sprint | Focus | Gate |
|---|---|---|
| 1 | Auth unit + API tests; application CRUD; the cross-user isolation pattern established here; RTL basics; register→create→status integration | ≥70% backend coverage; CI green |
| 2 | Upload validation (malicious-upload test first); interview and question CRUD; debrief flow; resume-application association | Malicious upload rejected; cross-user file access 404 |
| 3 | Mastery golden-file tests; every analytics formula against hand-computed fixtures; question bank and practice; chart component tests | Analytics golden files pinned |
| 4 | Recommendation logic; phrasing linter; feature extraction and point-in-time correctness; the full FL round; refusal and filtering; circuit-breaker fault injection | Payload schema and refusal tests passing |
| 5 | Event detection and notification channels; report builder fixtures; deletion orphan check; token reuse and expiry | Orphan check passing; no email enumeration |
| 6 | RTL in both themes; screenshots at 390 px and 1440 px; board moves against the state machine; header and CI-gate tests | No raw enum strings; illegal moves refused |
| 7 | Isolation tests for every new endpoint; search scoping | Search never returns foreign data |
| 8 | Contract tests against stubbed AI providers; fixture-based extraction tests; grounding tests; deterministic ranking | No prompt contains another user's data |
| 9 | Full security suite from the OpenAPI schema; accessibility scan in both themes; sample-data removal | Suite green; zero critical axe violations |

### 24.4 Standing rules

Every bug fix ships with a regression test that fails before the fix. Every PR touching a service adds or updates tests. Coverage never decreases. The suite runs offline and deterministically (NFR-50) with all randomness seeded. The full backend suite stays under 3 minutes (NFR-52), because a slow suite is one people stop running.

---

## 25. GitHub / SCRUM Workflow

### 25.1 Repository

A single monorepo. Rationale: shared types, atomic cross-stack changes, one CI configuration, one place to look. Splitting frontend and backend would create version-coordination overhead that a student team has no reason to pay.

### 25.2 Branching

```
main            protected · always deployable · squash-merge only
  └── develop   integration branch · CI must be green
        ├── feature/US-09-create-application
        ├── feature/US-58-fl-feature-extraction
        ├── fix/US-14-status-transition-validation
        ├── docs/sprint-2-retrospective
        └── chore/upgrade-flyway
```

Branch names carry the story ID, so the board, branches, commits and PRs can be traced to each other and to individual contributors.

Protection on `main`: no direct pushes; PR required; at least one approving review from someone other than the author; all CI checks passing; branch up to date before merge.

### 25.3 Commits

Conventional Commits with the story ID:

```
feat(applications): add status change endpoint with history (US-14)
fix(auth): prevent token refresh race on concurrent requests (US-04)
test(fl): assert payload contains no string fields (US-58)
docs(adr): record uniform-weight FedAvg decision (ADR-009)
chore(ci): add coverage gate at 70%
```

Types: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`, `perf`, `style`.

Commit hygiene rules: commit at working-increment granularity (several per day, not one per week); never commit under another member's account; never let one person merge everyone's work; write messages that describe intent rather than mechanics. A history of 400 commits from one account and 6 from another does not show individual contribution.

### 25.4 Pull requests

Template requires: linked story ID, what changed and why, how it was tested, screenshots for UI changes, a self-checklist against the DoD, and any follow-up work noted. Small PRs (target under 400 changed lines). Review within 24 hours, because a PR that sits for three days blocks a teammate. Reviewers check correctness, scoping (`user_id` present on every query), tests, and DoD compliance. Authors never approve their own PR, no matter how small the change or how close the deadline.

### 25.5 Issues and board

Every user story becomes a GitHub Issue with the story ID in the title, acceptance criteria as a task list, and labels for epic, priority, points, and sprint. Tasks within a story become sub-issues or checklist items. GitHub Projects provides the board: `Backlog → Sprint Backlog → In Progress → In Review → Testing → Done`. WIP limit of 2 per person in "In Progress", the discipline that prevents five half-finished features at sprint end. Sprint milestones group issues and give a burndown for free.

### 25.6 Ceremonies

| Ceremony | Cadence | Duration | Output |
|---|---|---|---|
| Sprint planning | Start of sprint | 90 min | Sprint backlog, assignments, sprint goal |
| Daily standup | Daily (async in a Slack/Discord thread is fine and often better for students) | 10 min | Blockers surfaced within 24 h |
| Backlog refinement | Mid-sprint | 60 min | Next sprint's stories estimated, ACs expanded, assumptions from §5.3 validated |
| Sprint review | End of sprint | 60 min | Working demo, stakeholder feedback, updated backlog |
| Retrospective | End of sprint | 45 min | 3 keeps, 3 changes, 1 action item with an owner |

Every ceremony produces a dated markdown file in `docs/agile/sprint-N/`. These files are the evidence that the process happened, and they cost ten minutes each to write.

### 25.7 Dividing work

The team of ten works in three departments: Backend (Anuj Madhani, Harshit Goyal, Achintya Chandwani, Dev Bhavsar), Frontend (Anshul Rai, Chinmay Dak) and AI/ML (Aaditya Kaushik, Nirmal Nakum, Ved Dhanani, Abhishek Singh). Each phase in `BUILD_PHASES_JAVA.md` names its owning department, and each person tests their own department's work. Departments work in parallel on independent phases, respecting each phase's Depends on column.

Role rotation across sprints (a member takes a different hat each sprint):

| Hat | Responsibility |
|---|---|
| Integrator | CI health, merge conflicts, release tagging, keeping `develop` green |
| Quality lead | Coverage gates, review depth, test debt |
| Documentation lead | Ceremony notes, ADRs, README, requirement traceability |
| Domain owner | The sprint's headline feature area |

Regardless of department: everyone writes tests for their own code, everyone reviews someone else's, everyone can explain the FL design. Concentrated FL knowledge in one head is a project risk (§28, R6).

---

## 26. Project Folder Structure

The full package-level structure is in `ARCHITECTURE_REVISION_JAVA.md` §7. At the top level:

```
cohort/
├── README.md  LICENSE  CONTRIBUTING.md  .gitignore  .env.example
├── docker-compose.yml               # dev: db + api + web
├── docker-compose.fl.yml            # FL profile: fl-service with isolated roles
├── .github/workflows/ci.yml         # build · tests · coverage gate · FL isolation check
├── api/                             # Spring Boot application (Java 21, Maven)
├── fl-service/                      # Python: Flower + scikit-learn, thin control plane
│   └── app/common/schema.py         # FEATURE_SCHEMA_V1, single source of truth
├── frontend/                        # React + TypeScript SPA, feature-first
└── docs/
    ├── architecture.md  poster.html  implementation-overview.md
    ├── adr/                         # one file per decision
    └── plan/                        # this document set
```

Two layout decisions. `fl-service/` sits outside `api/` because it is a different trust zone and should not share imports or configuration with the API. `fl-service/app/common/schema.py` is the single source of truth for the feature schema, used by the extractor, the serialisation test, and the `/fl/schema` endpoint, so the opt-in screen cannot drift out of sync with what is actually sent.

---

## 27. Deployment Plan

### 27.1 Principle

Local first, containerised from day one, cloud only if needed. The failure mode to avoid is a team spending the final sprint fighting a hosting provider instead of hardening the product.

### 27.2 Local development (Sprint 1 onward)

```
docker compose up
  ├── db      postgres:16-alpine · volume · healthcheck · port 5432
  ├── api     api/Dockerfile · Spring Boot · Flyway migrations on start
  └── web     frontend/Dockerfile · vite dev · mounts ./frontend
```

`docker compose up` is the only command a new team member needs. Migrations run automatically on API start in dev via an entrypoint script that waits on the db healthcheck. `make seed` loads demo data. Each member verifies this on their own OS in Sprint 1 (NFR-41) so that environment problems surface early.

### 27.3 FL profile (Sprint 4 onward)

```
docker compose -f docker-compose.yml -f docker-compose.fl.yml up
  ├── fl-server     NO database environment variables at all
  ├── fl-client-1   DATABASE_URL scoped to schema client_1, role client_1
  ├── fl-client-2 … each with its own schema and role
```

Kept in a separate compose file so the FL stack starts only when needed and the credential difference between server and clients is visible in one file.

### 27.4 Production profile

A production deployment profile with scheduled backups is outside the product scope (§31.2). The demo runs on the local Compose stack; all secrets come from the environment.

### 27.5 Hosting options

| Option | Effort | Cost | When to choose |
|---|---|---|---|
| Local only, demo on a laptop | None | Free | Default. The lowest-risk choice for a live demo. |
| Single VPS (Hetzner, DigitalOcean, campus VM) | Low | ~$5/mo | If a public URL is needed. One host, docker compose, Caddy or nginx for TLS. Recommended upgrade. |
| Split PaaS (Railway/Render API + Neon/Supabase Postgres + Vercel/Netlify frontend) | Medium | Free tiers | If a permanent public link matters more than the FL demo. Free tiers make it hard to run the FL client containers. |
| Kubernetes | High | Real money | No. Nothing here justifies it. |

Choice: local Compose is the demo path. The FL demonstration always runs locally, because it includes the credential-isolation walkthrough.

### 27.6 Pre-demo checklist

Fresh clone → `docker compose up` → migrations apply → seed loads → register a new user → complete the scripted path → run the FL round. Rehearsed twice, end to end, by two different people, at least three days before the presentation.

---

## 28. Risks & Mitigation

### 28.1 Project risks

| ID | Risk | Prob. | Impact | Mitigation | Early warning sign |
|---|---|---|---|---|---|
| R1 | Scope creep, the backlog grows faster than velocity | High | High | MoSCoW priorities and a fixed out-of-scope list (§31.2); no story enters a sprint mid-sprint; new ideas go to the backlog, not the branch | Sprint 2 finishes under 80% of committed points |
| R2 | FL complexity overruns Sprint 4 | High | High | The 1-day Flower spike in Sprint 3 (§13.11); simulation-mode fallback; success defined as correct behaviour, not accuracy | The spike takes more than one day |
| R3 | Insufficient data to train anything | Certain | Medium | Planned for, not discovered: synthetic non-IID cohort generator built in Sprint 3; results reported as mechanism validation | Accepted and designed around |
| R4 | Analytics silently wrong | Medium | High | Formulas defined in §20.7 before implementation; golden-file tests written before the code; hand-computed fixtures | A metric changes after an unrelated refactor |
| R5 | Time pressure from other coursework | High | High | 20% of Sprint 9 reserved for defects; the out-of-scope list; async standups; small PRs so work is never stuck in one head | Two consecutive missed standups |
| R6 | Knowledge silos, especially FL | Medium | High | Role rotation; cross-area review; a mandatory FL walkthrough session for the whole team in Sprint 4 | Only one person can answer a question about a subsystem |
| R7 | Integration problems late | Medium | High | Vertical slice in Sprint 1 exercises every layer; `develop` kept green; generated API types break the build on drift | A PR sits unmergeable for over 48 h |
| R8 | Testing deferred to the end | Medium | High | Coverage gate live in CI from Sprint 1; DoD blocks merge without tests | Coverage trends down two sprints running |
| R9 | Uneven contribution between members | Medium | High | Vertical-slice ownership; per-member PR requirement in Sprint 1 DoD; contribution reviewed at every retrospective while there is still time to correct | One member's PR count is an outlier by Sprint 2 |
| R10 | Environment differences across the team | Medium | Medium | Compose parity verified by every member in Sprint 1; no host-installed dependencies | Anyone says "it only works if I run it outside Docker" |
| R11 | Team member unavailable (illness, withdrawal) | Low-medium | High | No single-owner subsystem; documentation in-repo; work sliced so it can be handed over | none |
| R12 | Database schema churn late in the project | Medium | Medium | Full schema designed up front (§17); Flyway from day one; only additive migrations after Sprint 3 | A Sprint 4 story requires altering a Sprint 1 table |
| R13 | Demo fails live | Medium | High | Rehearsed twice from a fresh clone; seeded deterministic data; a recorded backup video | Any rehearsal needs a code change to complete |

### 28.2 Security risks

Covered in full in §23.3 with likelihood, impact, mitigation, and verification method for each.

### 28.3 Priority risks

If attention is limited, spend it here: R2 (FL overrun, mitigated by the Sprint 3 spike, which costs one day and buys a sprint), R1 (scope, mitigated by a fixed out-of-scope list), and R9 (contribution, mitigated by noticing in Sprint 2, when it is fixable, rather than in the final sprint, when it is not).

---

## 29. Concept Poster

### 29.1 Purpose and constraints

A1 portrait, readable from two metres, viewed for roughly ninety seconds. That constraint governs everything: roughly 60% visual, 40% text, no paragraph longer than three lines, no code, no ER diagram, no API table. Delivered as PDF plus editable source in `docs/poster/`.

### 29.2 Layout

```
┌──────────────────────────────────────────────────────────────┐
│  COHORT, Smart Job Application Tracker with                 │
│  Federated Interview Preparation                             │
│  Team names · Course · Institution · Year                    │
├───────────────────────────────┬──────────────────────────────┤
│  THE PROBLEM                  │  OUR SOLUTION                │
│  · 100+ applications tracked  │  Close the loop:             │
│    in a spreadsheet           │  track → interview →         │
│  · Interview memories decay   │  capture → target prep →     │
│    within 48 hours            │  measure                     │
│  · Preparation is generic,    │  Plus: learn across users     │
│    not targeted               │  without collecting their    │
│                               │  data                        │
├───────────────────────────────┴──────────────────────────────┤
│  SYSTEM ARCHITECTURE                                         │
│  [Simplified §16.2 diagram, 6 boxes maximum,                │
│   with the PRIVACY BOUNDARY drawn as a heavy line]           │
├──────────────────────────────────────────────────────────────┤
│  FEDERATED LEARNING                                           │
│                                                              │
│   User 1        User 2        User 3                         │
│   private       private       private                        │
│   data          data          data                           │
│     │             │             │                            │
│   local        local         local        ← training happens │
│   training     training      training       where the data   │
│     │             │             │           already is       │
│     └──── 25 floats, clipped ───┘                            │
│           and noised                                         │
│                   ▼                                          │
│           FEDERATED SERVER                                   │
│      aggregate → global model → back to clients              │
│                                                              │
│   ! RAW USER DATA NEVER LEAVES THE USER'S CLIENT             │
├──────────────────────┬───────────────────────────────────────┤
│  KEY FEATURES        │  PRIVACY BY DESIGN                    │
│  - Application       │  - Opt-in, default off                │
│    tracking with     │  - Only 24 numeric features leave     │
│    full history      │  - Clipped + noised (local DP)        │
│  - Structured        │  - Never fewer than 3 clients         │
│    interview debrief │  - Server holds no data credentials   │
│  - Topic mastery     │  - Privacy Ledger: every user sees    │
│    graph with decay  │    every value they ever sent         │
│  - Funnel analytics  │                                       │
│    with sample        │  [Screenshot: Privacy Ledger with a   │
│    sizes             │   vector expanded]                    │
│  - Explainable       │                                       │
│    recommendations   │                                       │
├──────────────────────┴───────────────────────────────────────┤
│  TECH STACK                                                  │
│  React · TypeScript · Tailwind · shadcn/ui · Recharts        │
│  Spring Boot · Java · PostgreSQL · Flyway · JWT/Argon2id     │
│  Python · scikit-learn · Flower · Docker · GitHub Actions    │
├──────────────────────┬───────────────────────────────────────┤
│  EXPECTED BENEFITS   │  FUTURE SCOPE                         │
│  · One source of     │  · Secure aggregation (SecAgg)        │
│    truth             │  · Browser-native FL clients          │
│  · Preparation       │  · Formal DP accounting               │
│    targeted at       │  · Calendar and email integration     │
│    measured gaps     │  · Mobile app                         │
│  · Collective        │  · Opt-in institutional cohort        │
│    insight without   │    analytics for career services      │
│    collective risk   │                                       │
└──────────────────────┴───────────────────────────────────────┘
```

### 29.3 Design notes

One accent colour is used only for the privacy boundary and the FL arrows. At most two screenshots: the dashboard action queue and the Privacy Ledger. The FL panel is at the visual centre, and the sentence *"raw user data never leaves the user's client"* is the most legible line on the sheet.

---

## 30. Definition of Done

### 30.1 Story-level DoD (every story, no exceptions)

- Acceptance criteria met and demonstrated.
- Code follows project conventions; lint, format, and type checks pass.
- Unit tests written for new logic; API tests for new endpoints; component tests for new UI.
- All tests pass locally and in CI; coverage has not decreased.
- Cross-user isolation verified for any endpoint touching user-owned data.
- Input validation present on both client and server.
- UI has empty, loading, and error states, and is keyboard-navigable.
- Reviewed and approved by someone other than the author.
- Merged to `develop` with a conventional commit referencing the story ID.
- Documentation updated where behaviour or contract changed.
- The GitHub issue is closed and the board card moved to Done.

### 30.2 Sprint-level DoD

- Every committed story meets the story-level DoD, or is explicitly returned to the backlog with a reason (never silently carried).
- `develop` merged to `main`; CI green; a fresh clone runs with `docker compose up`.
- Sprint review demo completed from seeded data without a code change.
- Retrospective held and written up in `docs/agile/sprint-N/`.
- Backlog refined for the next sprint; relevant §5.3 assumptions validated or escalated.
- Every team member has authored and merged at least one reviewed PR.

### 30.3 Sprint-specific additions

Sprint 1: Compose verified on every member's machine; migrations apply and roll back cleanly; the cross-user isolation suite passes for every endpoint; ≥70% backend coverage; concept poster complete and reviewed.
Sprint 2: A renamed executable is rejected by upload validation; cross-user file access returns 404; the debrief flow is timed under 3 minutes with 3 users; seed data loads deterministically.
Sprint 3: Every analytics metric has a documented formula in §20.7 and a passing golden-file test; the topic graph passes cycle detection; every topic has real questions; the Flower spike is complete and written up.
Sprint 4: The FL payload schema test passes (24 named floats, no strings); a 2-client round is refused and logged; the FL service is verified to hold no user-content credentials; the phrasing linter passes; the circuit-breaker fault-injection test passes; a Privacy Ledger entry displays a real transmitted vector.
Sprint 5: Notifications and reports work end to end; account deletion leaves no orphan rows; reset and verification tokens are single-use and expiring.
Sprint 6: Both themes and both breakpoints reviewed with screenshots; practice logging moves mastery end to end; security headers present on every response.
Sprint 7: Every new endpoint isolation-tested; search never returns another user's data.
Sprint 8: Every AI feature works on the local model and degrades gracefully when it is down; no prompt contains another user's data.
Sprint 9: Full security suite green; zero critical axe violations; the demo runs end to end twice without intervention.

### 30.4 Project-level DoD

All stories in §10.1 done; every NFR verified by its phase or marked "documented, not measured" in the traceability table (`BUILD_PHASES_JAVA.md`); full documentation set complete; poster delivered; deployment working; demo rehearsed; every team member able to explain the architecture, the analytics formulas, and the FL privacy design.

---

## 31. Product Scope

### 31.1 In scope

Authentication and account: registration, login, logout, refresh rotation, profile, onboarding, Argon2id, per-user isolation, password change and reset, email verification, Google and GitHub sign-in, data export, account deletion.
Applications and workflow: full CRUD, table and Kanban views, search, filter, sort, status changes with immutable history, timeline, days-in-stage, stall detection, company records and pages, contacts and referrals, offer comparison, weekly goals and activity heatmap, global search.
Resumes: versioned records, structured section editor, PDF export, validated file upload and secure download, application association, cloning, sample-size-gated performance comparison.
Interviews: creation against applications, multiple rounds, question capture with topic and difficulty, structured debrief under three minutes, derived topics, STAR story bank, prep packs, mock interviews.
Preparation: question bank with real questions for every topic, personal questions, practice attempts with confidence history, topic taxonomy with prerequisites, mastery with time decay, preparation tasks, progress views, spaced review, question recommendations, job readiness.
Analytics: funnel from history-based stage attainment, five conversion rates with denominators, activity over time, rejection distribution separated from no-response, dwell times, topic performance, resume performance, funnel simulator, sample sizes shown throughout.
Recommendations: rule-based generation, prerequisite root-cause routing, evidence tiers A-D, plain-language explanations, enforced non-causal phrasing, accept/snooze/dismiss, stall follow-ups.
Federated learning: opt-in default off, 24-feature extraction with point-in-time correctness, local scikit-learn training, L2 clipping, Gaussian noise, Flower FedAvg with a minimum cohort of 3 and norm filtering, versioned global models, Privacy Ledger with inspectable vectors, withdrawal, Federated Lift viewer, cohort benchmarks, privacy budget meter, credential-free FL service verified in CI.
Notifications and reports: in-app notifications from domain events; PDF and CSV export with secured downloads.
AI assistance: local-model provider with an optional disclosed hosted provider, Cohort Coach, job-posting parser, debrief extraction, resume and job-description match, email drafts, weekly review.
Platform: Docker Compose, Flyway migrations, CI with build, tests and coverage gate, security headers, secret and SQL-interpolation scans, security test suite, accessibility pass, seed and sample data, guided tour, designed error states, privacy and AI disclosure pages, documentation, concept poster.

### 31.2 Out of scope

The product does not include:

- Tag management and bulk status updates (FR-24).
- External recruiter feedback capture (FR-44).
- An admin console or administrator role (FR-11).
- An email or push notification channel (the channel interface allows one; none is built).
- A calendar view, .ics export, or two-way calendar or email sync.
- CSV import of applications.
- Job-board scraping, integrations or auto-apply; a browser extension.
- Multi-factor authentication.
- Sharing or collaboration between users of any kind.
- Native mobile applications (the web app is fully responsive).
- A production deployment profile with scheduled backups and restore.
- Structured operations logging and observability.
- CI quality gates beyond the coverage gate: lint, type and import-boundary gates.
- Load testing and a cross-browser test matrix.
- Secure aggregation, formal DP accounting, real-user federation at scale, and browser-based FL clients.

### 31.3 One-sentence summary

*A privacy-first job-search tracker that turns each interview into targeted, evidence-tiered preparation, assists with AI that runs on a local model and answers only from the user's own data, and learns which preparation patterns work across users through a federated learning deployment in which raw data never leaves the user's own partition, and every user can inspect exactly what did.*

---

## 32. Future Scope

| Area | Enhancement | Value | Effort |
|---|---|---|---|
| FL | Secure aggregation (SecAgg) so the server sees only the encrypted sum | Removes the largest acknowledged gap in the threat model (§22.7) | High |
| FL | Browser-native WASM clients | True on-device training; no server-side client containers | High |
| FL | Formal RDP composition and a real ε-δ guarantee | Turns "approximate" epsilon into a defensible number | Medium |
| FL | Aggregation robust to poisoning (Krum, trimmed mean) + client reputation | Sybil and poisoning resistance at scale | Medium |
| FL | Personalisation layers over the global model | Better fit under non-IID data (DR-20) | Medium |
| ML | Survival analysis for stage transitions | Proper handling of right-censored applications; better stall prediction | Medium |
| ML | Sequence models over interview history | Captures trajectory rather than a snapshot | High |
| Product | Calendar integration for interview scheduling | Removes duplicate entry | Medium |
| Product | Email parsing to auto-detect application updates | Largest possible reduction in data-entry burden, and the largest new privacy surface, which is why it is outside the product scope | High |
| Product | Browser extension to capture a listing in one click | Removes the biggest friction point in adoption | Medium |
| Product | Mobile application | Debrief immediately after leaving the room, when recall is best | High |
| Analytics | Cohort benchmarking for institutions, opt-in and differentially private | Brings career-services staff in as a stakeholder (see §4) | High |
| Platform | S3-compatible object storage | Already abstracted behind the storage interface, a swap, not a rewrite | Low |
| Platform | Observability: metrics, tracing, alerting | Operational maturity | Medium |
| Security | MFA, third-party penetration test | Closes the §23.4 gaps | Medium |
| Accessibility | Screen-reader-optimised chart alternatives beyond data tables | Beyond AA | Medium |

---

## 33. Key Design Decisions

Seven decisions that shape the system, each with its reason.

1. No `POST /fl/update` endpoint in the application API.
Routing model updates through the application backend would put user-linked updates in the same process that holds database credentials, weakening the boundary the FL design exists to create. The app API keeps only the control-plane endpoints (`/fl/status`, `/fl/opt-in`, `/fl/ledger`, `/fl/model`, `/fl/rounds`). Recorded as ADR-007.

2. Nine resource-scoped analytics endpoints instead of one `GET /analytics`.
One endpoint returning everything is slow, uncacheable, hard to test, and forces over-fetching. Splitting also lets each metric carry its own `sample_size` and `sufficient_data`, so FR-67 is enforced by the contract rather than by UI discipline.

3. A platform, quality and DevOps epic.
Infrastructure work that is not in the backlog is unestimated and gets skipped. As stories it also gives every member reviewable Sprint 1 work.

4. Argon2id over bcrypt.
Argon2id is memory-hard and the current OWASP recommendation (ADR-001).

5. A dedicated hardening sprint.
Sprint 9 is a real sprint with real stories, a Definition of Done, and 20% capacity reserved for defects, so hardening is never squeezed to nothing.

6. The Flower spike in Sprint 3.
One day in Sprint 3 surfaces Flower's API friction before Sprint 4 depends on it.

7. Companies scoped per user rather than shared globally.
A global company table creates a cross-user inference channel and needs deduplication logic. Per-user scoping costs some duplication and buys isolation (ADR-004).
