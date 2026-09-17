# Smart Job Application Tracker — Project Plan

**Status:** PLANNING ONLY. No code written, no files created, no dependencies installed, no database configured.
**Working title:** *Cohort* — Smart Job Application Tracker with Federated Interview Prep
**Document version:** 1.0 (awaiting approval)

---

## 1. Repository Assessment

### 1.1 What I inspected

I examined the working environment and the upload directory before writing anything:

| Check | Result |
|---|---|
| Uploaded files | None (`/mnt/user-data/uploads` empty) |
| Existing project directory | None |
| Framework in place | None |
| `package.json` / `requirements.txt` / `pyproject.toml` | None |
| Database configuration | None |
| `.env` / `.env.example` | None |
| README | None |
| Test suite | None |
| Git configuration / history | None |
| Docker files | None |
| Code to preserve | None |

**Nothing was modified.**

### 1.2 Conclusion

The repository is **empty**. This is a clean greenfield build, which is good news: there is no legacy structure to work around, no framework mismatch, and no migration cost. The entire architecture below is proposed from scratch.

### 1.3 What this means for the plan

- Every folder, file, and dependency in this document is **proposed**, not existing.
- Sprint 1 must include repository bootstrapping (git init, `.gitignore`, branch protection, CI skeleton, Docker Compose) as real, pointed work — it is not free.
- There is no prior art in the repo constraining naming conventions, so the conventions defined in §25 become the project standard from commit one.

### 1.4 Assumptions I am making (flag any that are wrong before approval)

| # | Assumption | Why it matters | How to validate |
|---|---|---|---|
| A1 | Team size is 3–5 students | Drives velocity and sprint scope | Confirm with me |
| A2 | Sprints are 2 weeks; ~5 sprints total (~10–12 weeks) | Roadmap shape | Confirm academic calendar |
| A3 | Each student can deliver 12–15 story points per 2-week sprint | Backlog sizing | Recalibrate after Sprint 1 |
| A4 | Deployment target is local Docker Compose + optional single free-tier cloud host | Avoids over-engineering | Confirm evaluator expectation |
| A5 | Evaluators care about *process artifacts* (backlog, sprints, conflicts, poster) at least as much as the running product | Documentation weight | Confirm rubric |
| A6 | Real multi-user data will not exist during the project | FL demo must work with synthetic cohorts | Accept as design constraint |
| A7 | Python 3.11+, Node 20+ available on team machines | Toolchain | Confirm |

---

## 2. Project Overview

### 2.1 The problem

A student applying for internships or graduate roles typically runs 40–150 applications across a season. The state of that campaign lives in a spreadsheet, a notes app, three email threads, and memory. Three specific failures follow:

1. **Pipeline amnesia.** Applications stall silently. Nobody follows up because nobody notices.
2. **Post-interview evaporation.** The single most valuable artifact a candidate produces is the memory of what was actually asked, and it decays within 48 hours. It is almost never captured in a structured way, so it cannot inform the next round of preparation.
3. **Blind preparation.** Students prepare by grinding a generic list, not by targeting the topics that actually cost them interviews.

Existing tools solve (1) as a Kanban board and ignore (2) and (3) entirely.

### 2.2 The solution

A web application that closes the loop: **track → interview → capture what happened → convert that into a targeted preparation plan → measure whether it worked**. Then, and this is the innovative part, learn *which preparation patterns are associated with better outcomes across many users* — without any user's raw data ever leaving their own data partition, using Federated Learning.

### 2.3 Why this project will not look like everyone else's

This is the section you specifically asked about, so it gets real attention. The default version of this project — the one a dozen other teams will submit — is a CRUD board with status columns, four stat cards, a pie chart, and an "AI" feature that is a single call to a hosted LLM. Below are seven design decisions that make this build structurally different. Each one is cheap enough for a student team and each one is visible in a five-minute demo.

---

**D1 — The Federated Learning is real, not simulated on pooled data.**

The common shortcut is to keep all user data in one database, then run a script that *pretends* to split it into clients, average some weights, and call it FL. That demonstrates nothing, because the privacy property was never tested.

In this design each FL client is a **separate OS process with its own isolated data store** (its own Postgres schema, credentials scoped to that schema only). The Flower server has **no database credentials at all** — architecturally it cannot read user data even if compromised. The only thing crossing the boundary is a fixed-length float vector of clipped, noised model parameters. When the demo runs, you can `docker compose stop` the server, dump its container filesystem, and show the evaluator there is no user data in it. That is a demonstrable claim, not a slide.

**D2 — The Privacy Ledger (glass-box FL).**

Every user gets a page that is not a privacy *policy* but a privacy *log*. For each federated round the user's client participated in, it shows: round ID, timestamp, the exact feature schema that was used, the L2 norm of the update before and after clipping, the noise multiplier applied, the number of peer clients in the cohort, and a "what was NOT sent" list. The user can expand any round to see the actual 24-float vector that left their machine. They can revoke participation per round, and export the whole ledger as JSON.

I have not seen a student project do this, and it inverts the usual dynamic where privacy is asserted rather than shown. It also makes an excellent poster panel.

**D3 — Evidence-tiered recommendations with honest uncertainty.**

Every recommendation carries a visible evidence badge, and the language changes with the tier:

| Tier | Source | Example phrasing |
|---|---|---|
| Tier A — Your data | ≥5 of the user's own observations | "In 6 of your last 8 technical rounds, graph questions were marked low-confidence." |
| Tier B — Your data, thin | 2–4 observations | "Early signal only (2 rounds): dynamic programming may be a gap." |
| Tier C — Federated | Global model, cohort ≥ k | "Across the federated cohort, users who logged ≥10 practice reps on a weak topic before a repeat round reported higher confidence. This is an association, not a guarantee." |
| Tier D — Heuristic | Rules, no data yet | "You have no interviews logged. Start with a broad baseline." |

The system is explicitly forbidden from causal phrasing. There is a linter test in the suite (`test_no_causal_language`) that fails the build if a recommendation template contains banned strings like "will get you", "guarantees", "because of this you were rejected". Encoding intellectual honesty as a **failing unit test** is a genuinely unusual thing to show an examiner.

**D4 — Topic Mastery Graph with time decay, not a checkbox list.**

Prep topics are nodes in a directed prerequisite graph (Arrays → Two Pointers → Sliding Window; Recursion → Trees → Graphs → Shortest Path). Each user has a **mastery score per topic in [0,1]** that:
- rises with successful practice reps and good interview outcomes on that topic,
- decays exponentially with time since last touch (a forgetting curve, half-life configurable per difficulty),
- **propagates weakness backwards**: if you fail Shortest Path, the engine checks whether Graphs and BFS/DFS mastery are the actual root cause and recommends the prerequisite rather than the symptom.

Root-cause routing is the differentiator. Everyone else recommends "practice graphs". This recommends "your shortest-path failures trace back to weak BFS traversal — start two levels down."

**D5 — Pipeline health: stall detection, not just a funnel.**

Every other project ships the same static funnel bar chart. This one also computes, per stage, the **empirical time-to-transition distribution from the user's own history** and flags applications that have exceeded their stage's median dwell time. The dashboard surfaces "3 applications are stalled past your usual OA turnaround" with a suggested follow-up action, and does so with explicit sample-size caveats when history is thin. Analytics that produce an *action* rather than a *number*.

**D6 — Federated Lift viewer.**

A side-by-side control on the Recommendations page: **"Local only"** vs **"Local + Federated"**. Toggling it re-ranks the recommendations live and shows which items moved and by how much. This makes the FL contribution *visible and measurable* instead of an invisible backend claim, and it is the single best 30 seconds of the demo.

**D7 — Privacy budget as a first-class UI object.**

Differential privacy epsilon is not buried in a config file. The user sees a budget meter: total ε allocated, ε spent per round, rounds remaining before the budget is exhausted, and what happens when it is (the client stops contributing and says so). Users can choose a privacy posture — Strict / Balanced / Contributor — which sets the noise multiplier and clipping norm.

---

**Anti-similarity checklist.** The team should deliberately avoid these commodity patterns:

- ❌ Trello-clone drag-and-drop board as the primary UI (use a dense, sortable, filterable data table with a detail drawer instead)
- ❌ Four generic stat cards across the top of a dashboard (use one pipeline-health strip + one action queue)
- ❌ A pie chart of application statuses
- ❌ "AI resume review" that is one LLM prompt
- ❌ Purple gradient hero, default shadcn theme, Inter everywhere
- ❌ Recommendations phrased as prophecy
- ❌ FL as a diagram on a slide with no running processes behind it

### 2.4 Naming

`Cohort` works on three levels: the peer cohort you're job hunting alongside, the FL client cohort, and the minimum-cohort-size privacy guarantee. Alternatives if you prefer: *Loop*, *Postmortem*, *Aperture*, *Throughline*. The name is cosmetic — say the word and I'll switch it everywhere.

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

### 3.2 Academic goals

| ID | Goal |
|---|---|
| A1 | Demonstrate full Agile/SCRUM practice with real sprint artifacts |
| A2 | Produce complete requirements documentation (FR/NFR/DR, elicitation, conflict analysis) |
| A3 | Show genuine individual contribution through GitHub history |
| A4 | Deliver a working, deployable, tested system — not a prototype |
| A5 | Demonstrate a technically meaningful innovation (FL) rather than a buzzword |

### 3.3 Explicit non-goals

- Not a job board or scraper. We do not ingest listings from third-party sites (ToS and scope risk).
- Not an LLM wrapper. No generative resume rewriting in the MVP.
- Not a social network. No user-to-user visibility of any kind, ever.
- Not a production multi-tenant SaaS. Single-region, single-deployment, Compose-based.

---

## 4. Stakeholders

| ID | Stakeholder | Role | Interest | Requirements they bring | Influence | Elicitation technique |
|---|---|---|---|---|---|---|
| S1 | **Students / job seekers** (primary end user) | Daily user; owns all data | Manage campaign, prepare better, get offers | Fast data entry, mobile-legible, honest analytics, confidence that private notes stay private | **High** — they define the product; low authority over scope | Semi-structured interviews (6–8), survey (30+), observation of current spreadsheet workflow |
| S2 | **Project team / developers** | Build, test, document, present | Deliverable scope is achievable; clean architecture; individual contribution is visible | Modular boundaries, CI, seed data, clear DoD | **High** | Sprint planning, retrospectives, brainstorming |
| S3 | **Faculty / project evaluators** | Assess against rubric | Correct SE process, documentation completeness, technical merit, individual effort | SCRUM artifacts, requirement traceability, poster, per-member commit history | **Very high** — they hold acceptance authority | Document analysis of the project brief; clarification meetings |
| S4 | **System administrator** (operational role, played by team) | Deploy, monitor, back up, manage user accounts | Reliable, observable, recoverable system | Health endpoint, structured logs, migrations, backup/restore path, admin ability to disable an account | Medium | Brainstorming; analysis of ops runbooks |
| S5 | **ML / FL administrator** (operational role, played by team) | Configure and run federated rounds | Rounds converge; privacy parameters are correct; poisoning is contained | Round configuration, min-cohort enforcement, clipping/noise settings, round audit log, model rollback | Medium–high (owns the innovation) | Document analysis of FL literature; prototype experimentation |
| S6 | **Database / system maintainer** (operational role, played by team) | Schema, migrations, integrity, performance | Normalized schema, safe migrations, indexed queries | Alembic migrations, FK constraints, per-user isolation enforced in the data layer | Medium | Design walkthroughs |
| S7 | **University / institution** (indirect) | Provides infrastructure, governs conduct | Academic integrity; no misuse of student data; licensing | Data handling within institutional norms; open-source licence compliance | Low–medium, but a hard constraint | Document analysis of institutional policy |

**Deliberately excluded** (and why — do not pad the list): *Recruiters and employers* are not stakeholders; they never touch the system and have no interface to it. *Career services staff* would be a plausible future stakeholder for a cohort-analytics dashboard, but that feature is out of MVP scope, so including them now would generate requirements we will not build. Both are recorded in Future Scope (§32) instead.

---

## 5. Requirement Elicitation

### 5.1 Strategy overview

Requirements come from three asymmetric sources: the project brief (authoritative, fixed), end users (rich, but must be sampled), and the FL/ML literature (technical constraints we do not get to negotiate). The strategy weights techniques accordingly.

### 5.2 Techniques

| Technique | Used with | Gathers | Why suitable | Expected output |
|---|---|---|---|---|
| **Document analysis** | S3, S7, FL/DP literature, competitor tools (Teal, Huntr, Simplify) | Mandatory deliverables; grading criteria; established FL algorithms (FedAvg), DP mechanisms, secure-aggregation constraints; feature baselines and their gaps | The brief is non-negotiable and must be traced; FL correctness cannot be invented from intuition | Requirement traceability matrix; FL parameter defaults; competitive gap analysis feeding §2.3 |
| **Semi-structured interviews** (6–8 × 30 min) | S1 students who are actively applying | The real workflow, the actual pain, what they currently record after interviews, what they refuse to type into a computer, privacy attitudes | Depth on motivations and privacy sensitivity, which surveys cannot reach | Interview transcripts; pain-point inventory; a "sensitive data" list that constrains the FL feature schema |
| **Survey** (target 30–50 responses) | S1 broader student population | Frequency distributions: applications per season, tools used, drop-off stages, willingness to share anonymised model updates, feature demand ranking | Cheap validation of whether interview findings generalise; gives quantitative backing for prioritisation | Prioritised feature demand table; the willingness-to-federate percentage, which directly justifies opt-in defaults |
| **Observation / artefact study** | 3–4 S1 volunteers | How their existing spreadsheet or Notion board is actually structured; which columns they created themselves | Users under-report their own workarounds; the columns they invented *are* requirements | Field-derived list of application attributes → validates the §21 data model |
| **Brainstorming workshops** | S2 team, with S5 hat on | Architecture options, FL task selection, conflict identification, risk register | Design decisions need divergent generation then convergent selection | Decision log (ADRs); requirement conflict list (§14); risk register (§28) |
| **Prototype feedback / walkthrough** | S1 (5 users), S3 | Reaction to clickable wireframes and to the Privacy Ledger concept specifically | Privacy UX is impossible to specify correctly without showing people something | Sprint 2+ backlog refinements; ledger content validation |

### 5.3 Known requirements vs assumptions needing validation

This distinction matters and is frequently missed. Everything below is tracked in a live document.

**KNOWN — from the brief, authoritative, not up for debate:**
- The stack (React/TS/Tailwind/shadcn/Recharts; FastAPI; PostgreSQL/SQLAlchemy; JWT + Argon2/bcrypt; scikit-learn; Flower; Pytest/RTL; Docker/Compose/Git/GitHub)
- FL is mandatory and raw data must not reach the central server
- Required documentation set and Agile/SCRUM process
- GitHub development with per-member contribution
- Concept poster in Sprint 1

**KNOWN — from domain logic, low risk:**
- Application lifecycle stages and the fact that they are non-linear (rejection can occur at any stage)
- Users need multiple resume versions
- Interviews belong to applications and have rounds
- Users must never see each other's data

**ASSUMPTION — must be validated before the dependent sprint:**

| ID | Assumption | Depends on | Validate by | Deadline |
|---|---|---|---|---|
| V1 | Users will complete a structured post-interview debrief if it takes <3 min | Sprint 2 (EPIC 4) | Interviews + prototype walkthrough | End Sprint 1 |
| V2 | Users will opt in to federated contribution when shown the ledger | Sprint 4 (EPIC 8) | Survey question + prototype reaction | End Sprint 2 |
| V3 | Self-rated confidence per topic is a usable label for the ML task | Sprint 4 (EPIC 7) | Interviews; check whether users can rate consistently | End Sprint 2 |
| V4 | Users track enough applications (≥20) for personal analytics to be meaningful | Sprint 3 (EPIC 6) | Survey | End Sprint 1 |
| V5 | Users want resume-version performance comparison despite weak statistical power | Sprint 3 (EPIC 3/6) | Interviews | End Sprint 2 |
| V6 | Tagging free-text notes is acceptable; users won't demand full-text search | Sprint 2 | Survey | End Sprint 1 |

Any assumption that fails validation triggers a backlog refinement, recorded in the sprint review notes. This is exactly the kind of traceability evaluators look for.

---

## 6. Functional Requirements

Priority: **M** = Must (MVP), **S** = Should, **C** = Could, **F** = Future.

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
| FR-11 | The system shall support an administrator role able to disable accounts and view aggregate system health, but never able to read another user's application, resume, or interview content. | C |

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
| FR-24 | The system shall allow bulk status update for multiple selected applications. | C |
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
| FR-43 | The system shall allow recording structured post-interview feedback: overall self-rating (1–5), what went well, what went poorly, improvement areas, perceived difficulty, and outcome. | M |
| FR-44 | The system shall allow recording externally provided recruiter feedback separately from self-evaluation. | C |
| FR-45 | The system shall provide a guided debrief flow completable in under 3 minutes. | S |
| FR-46 | The system shall automatically suggest an application status change when an interview outcome is recorded, requiring user confirmation. | C |
| FR-47 | The system shall derive the set of topics tested in an interview from its recorded questions. | M |

### 6.5 Interview Preparation

| ID | Requirement | Priority |
|---|---|---|
| FR-48 | The system shall provide a question bank containing both seeded system questions and user-created questions. | M |
| FR-49 | The system shall categorise questions by category (DSA, Technical, Behavioural, HR, System Design, Company-specific), topic, and difficulty (Easy/Medium/Hard). | M |
| FR-50 | The system shall allow a user to create, edit, and delete their own questions; system-seeded questions shall be read-only but forkable. | M |
| FR-51 | The system shall allow a user to record a practice attempt on a question, capturing practice status, self-confidence (1–5), time taken, and notes. | M |
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
| NFR-27 | Database migrations are versioned and reversible | Alembic up/down test in CI |
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
| NFR-36 | Backend layered as routers → services → repositories → models, with no layer skipping | Import-boundary lint rule |
| NFR-37 | Backend line coverage ≥70%; services and analytics/recommendation modules ≥85% | `pytest --cov` gate in CI |
| NFR-38 | All Python public functions typed and passing `mypy`; frontend `strict: true` with no `any` in committed code | CI gate |
| NFR-39 | Linting and formatting enforced (Ruff + Black; ESLint + Prettier) and run on every PR | CI gate |
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
| NFR-47 | Status is never conveyed by colour alone — badges carry text | UI review |
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

Domain requirements are constraints imposed by the **problem domain itself** — recruitment, interviewing, and federated learning. They are not features anyone requested and they are not implementation choices; they are truths about the domain that the system must respect or it will be wrong. Where a functional requirement says *what the system does*, a domain requirement says *what the world is like*.

### 8.1 Job applications & recruitment

| ID | Domain requirement | Rationale / consequence |
|---|---|---|
| DR-01 | An application progresses through discrete stages, but the progression is **not strictly linear** — rejection or withdrawal may occur at any stage, and stages may be skipped entirely. | The status model must be a state machine permitting terminal transitions from every non-terminal state, not an ordered pipeline with fixed steps. |
| DR-02 | The **absence of a response is the most common outcome**, and it is not an event — no employer sends a "we are ignoring you" signal. | "Ghosted" must be inferable from elapsed time rather than recorded, and any funnel that treats missing outcomes as failures will misreport. |
| DR-03 | A single application belongs to exactly one company and one role, but a user may hold several concurrent applications with the same company. | Company must be a separate normalised entity; uniqueness must not be enforced on (user, company). |
| DR-04 | Recruitment stages are **not standardised across employers**; one company's "OA" is another's "screening test", and round counts vary from 1 to 7. | The canonical stage set must be small and abstract, with a free-text round label carried alongside it. |
| DR-05 | Timelines are long and irregular — weeks between stages is normal, and outcomes can arrive months later. | Time-to-transition must be modelled as a distribution, and analytics must handle right-censored (still-open) applications rather than counting them as failures. |
| DR-06 | Outcomes are influenced by many factors outside the system's observation: referrals, headcount freezes, timing, luck. | The system must never attribute an outcome to a single tracked variable. This constraint flows directly into FR-74. |

### 8.2 Interviews

| ID | Domain requirement | Rationale / consequence |
|---|---|---|
| DR-07 | Interview content is recalled from memory and decays rapidly; recall is partial and often wrong on detail. | Debrief capture must be fast, allow partial records, and never require fields the user cannot reliably produce. |
| DR-08 | Self-assessment of interview performance is **systematically biased** and only weakly correlated with the actual result. | Self-rating is a legitimate input signal but must not be treated as ground truth; models must be described as predicting *self-reported* confidence, not success. |
| DR-09 | A single interview round typically tests multiple topics simultaneously, so credit assignment between topics is ambiguous. | Topic attribution must be at question level, not round level; round-level attribution would produce false weak-topic signals. |
| DR-10 | Interview questions are frequently under NDA or otherwise confidentiality-constrained. | Question text is private by default, never shared to any global surface, and never included in any federated payload. |
| DR-11 | Interviewer identity is personal data about a third party who has not consented to being in the system. | Interviewer fields are optional, stored as free text with no separate entity, excluded from all analytics and all model features, and purged on account deletion. |

### 8.3 Resumes

| ID | Domain requirement | Rationale / consequence |
|---|---|---|
| DR-12 | A resume is a versioned document tailored per role; a job seeker legitimately maintains several simultaneously. | Versioning with lineage is a domain necessity, not a convenience feature. |
| DR-13 | Resume files contain **directly identifying personal data** — full name, phone, address, sometimes date of birth. | Resume files are the highest-sensitivity artefact in the system: never publicly served, never parsed into shared storage, never used to derive federated features. |
| DR-14 | The number of applications per resume version is small (typically 5–40), giving very low statistical power for outcome comparison. | Resume performance comparison must display sample size and be suppressed below threshold (FR-37). Reporting "Resume B is 12% better" from n=6 would be statistically dishonest. |

### 8.4 Interview preparation

| ID | Domain requirement | Rationale / consequence |
|---|---|---|
| DR-15 | Technical topics have **genuine prerequisite structure** — shortest-path algorithms cannot be learned before graph traversal. | The topic taxonomy must be a DAG with prerequisite edges, and recommendations must respect it (FR-72). |
| DR-16 | Skill decays with disuse; a topic practised four months ago is not currently ready. | Mastery must carry a time-decay term (FR-53). A static "completed" checkbox misrepresents the domain. |
| DR-17 | Preparation effort competes with academic and personal commitments; capacity is scarce and lumpy. | Recommendations must be few and ranked, with realistic effort estimates. A 40-item plan will be ignored. |

### 8.5 Privacy & Federated Learning

| ID | Domain requirement | Rationale / consequence |
|---|---|---|
| DR-18 | Job-search data is **career-sensitive**: disclosure that a user is interviewing elsewhere can cause real-world harm. Users know this and behave accordingly. | Privacy is not a compliance checkbox but a functional prerequisite for the product working at all; a user who does not trust the system will not enter honest data, which destroys the analytics. |
| DR-19 | Federated learning requires the learning task to be **learnable from a fixed-schema numeric vector**; anything requiring raw text or cross-user joins is out of scope for FL. | Task selection is constrained before design begins — this rules out, for example, federated learning over question text. |
| DR-20 | Federated data is **non-IID**: each user's distribution reflects their own field, seniority, and geography. | FedAvg convergence will be uneven; evaluation must report per-client as well as global metrics, and the plan must not assume clean convergence curves. |
| DR-21 | Model updates can leak information about training data (gradient inversion, membership inference); transmitting raw updates is not automatically private. | Clipping plus calibrated noise is mandatory, not optional. "We use FL therefore it is private" is a false claim and must not appear in the report or poster. |
| DR-22 | A federated cohort of fewer than a few clients provides effectively no anonymity — averaging two updates barely hides either. | Minimum cohort size *k* must be enforced server-side (FR-83). |
| DR-23 | Any client in an open federated system may be malicious and submit crafted updates. | Norm-based filtering is required (FR-84); the threat model must be stated honestly, including what this design does *not* defend against. |

---

## 9. User Stories

Format: **As a [role], I want [goal], so that [benefit].**
Roles used: *job seeker* (primary), *administrator*, *FL administrator*, *developer*.
Points use a modified Fibonacci scale (1, 2, 3, 5, 8, 13) where **1 point ≈ 2–3 focused hours** for a student developer.

Acceptance criteria are given in full for Sprint 1 and Sprint 2 stories, and condensed (the 3 highest-value checks) for later sprints — those will be expanded during backlog refinement in the sprint before they are pulled, which is normal SCRUM practice rather than an omission.

### EPIC 1 — Authentication & User Management

**US-01 — Register an account** · Priority: Must · Points: 5 · Sprint 1 · Deps: US-67, US-68
> As a job seeker, I want to register with my email and a password, so that I have a private account for my job-search data.

Acceptance criteria:
- Given valid email, password (≥10 chars), and display name, the account is created and I am redirected to the dashboard.
- The password is stored as an Argon2id hash; the plaintext appears in no database column, no log line, and no API response.
- Registering with an already-used email returns a generic failure message that does not confirm the address is registered.
- A password shorter than 10 characters or present in the common-password blocklist is rejected with an inline field error.
- Email format is validated on both client and server; the server is authoritative.
- A newly created user has `fl_opt_in = false`.

**US-02 — Log in** · Must · 3 · Sprint 1 · Deps: US-01
> As a job seeker, I want to log in, so that I can reach my own data.

- Correct credentials return an access token (≤15 min) and a refresh token; I land on the dashboard.
- Incorrect credentials return 401 with a message that does not reveal whether the email exists.
- After 10 failed attempts from one IP within 15 minutes, further attempts are rate-limited.
- A valid session survives a page refresh.

**US-03 — Log out** · Must · 2 · Sprint 1 · Deps: US-02
> As a job seeker, I want to log out, so that my data is not reachable from a shared machine.

- Logging out clears client-side credentials and revokes the refresh token server-side.
- The revoked refresh token cannot mint a new access token.
- Protected routes redirect to login after logout.

**US-04 — Session persistence and expiry** · Must · 3 · Sprint 1 · Deps: US-02
> As a job seeker, I want my session refreshed silently while I work and ended when it truly expires, so that I am neither logged out mid-task nor left logged in indefinitely.

- An expired access token is transparently refreshed once, and the original request is retried.
- If the refresh token is expired or revoked, I am redirected to login with an explanatory message.
- Concurrent requests during a refresh do not trigger multiple refresh calls.

**US-05 — View and edit my profile** · Must · 3 · Sprint 1 · Deps: US-02
> As a job seeker, I want to maintain my profile, so that the system can tailor defaults to my situation.

- I can view and update display name, target role, graduation year, and timezone.
- Changes persist and are reflected immediately in the header.
- I cannot change my email to one already in use.
- Validation errors appear inline against the offending field.

**US-06 — Change my password** · Should · 3 · Sprint 5 · Deps: US-05
> As a job seeker, I want to change my password, so that I can respond to a suspected compromise.
- Requires current password; all other refresh tokens are revoked on success; new password re-validated against the policy.

**US-07 — Export all my data** · Should · 3 · Sprint 5 · Deps: EPIC 2–5
> As a job seeker, I want to export everything I have entered as JSON, so that I am not locked in.
- Export includes applications, resumes (metadata + files), interviews, questions, tasks, and the privacy ledger; it contains no other user's data; it completes in ≤10 s for 500 applications.

**US-08 — Delete my account** · Should · 5 · Sprint 5 · Deps: US-07
> As a job seeker, I want to permanently delete my account, so that I control my own footprint.
- Requires typed confirmation; removes every row referencing my ID plus uploaded files and FL client state; a post-deletion orphan-check query returns zero rows.

---

### EPIC 2 — Job Application Tracking

**US-09 — Create an application** · Must · 5 · Sprint 1 · Deps: US-02, US-68
> As a job seeker, I want to record a job application, so that I stop losing track of where I applied.

- The form requires company, job title, status, and date applied; all other fields are optional.
- Typing a company name offers existing companies and creates a new one if none matches.
- On save I return to the list with the new row visible and a success toast shown.
- Server-side validation rejects a future `date_applied` and an unknown status.
- The created record is owned by me; the owner ID is set server-side from the token and can never be supplied by the client.

**US-10 — List my applications** · Must · 5 · Sprint 1 · Deps: US-09
> As a job seeker, I want a table of all my applications, so that I can see my whole campaign at once.

- Columns: company, role, status badge, stage, days in stage, date applied, priority.
- Paginated at 25 rows with page-size options; total count displayed.
- Sortable by date applied, company, status, and days in stage.
- The table shows only my applications; another user's records are never returned by the endpoint.
- An empty state with a "Create your first application" action appears when I have none.
- Loading and error states are distinct from the empty state.

**US-11 — View application detail** · Must · 3 · Sprint 1 · Deps: US-09
> As a job seeker, I want a detail view for an application, so that I can see everything about it in one place.

- Shows all stored fields, the linked resume, the status timeline, and related interviews.
- Requesting an application ID belonging to another user returns 404.
- A non-existent ID returns 404 with a friendly not-found screen.

**US-12 — Edit an application** · Must · 3 · Sprint 1 · Deps: US-11
> As a job seeker, I want to edit an application, so that I can correct and enrich it as I learn more.
- Every field is editable; `updated_at` changes; unchanged fields are untouched; concurrent edit does not silently discard data (last-write-wins is acceptable but must be documented).

**US-13 — Delete an application** · Must · 3 · Sprint 1 · Deps: US-11
> As a job seeker, I want to delete an application, so that mistakes and duplicates do not pollute my analytics.
- A confirmation dialog names the company and role; deletion cascades to interviews, questions, and status history; the row disappears without a full page reload; deleting another user's record is impossible (404).

**US-14 — Change status and record history** · Must · 5 · Sprint 1 · Deps: US-11
> As a job seeker, I want to update an application's status with each change recorded, so that I keep an accurate history rather than only a current state.

- Status can be changed from the list row and the detail view.
- Every change writes an immutable `application_status_history` row: previous status, new status, timestamp, optional note.
- History rows cannot be edited or deleted independently of the application.
- The state machine rejects invalid transitions with a clear message.
- Changing to a terminal status prompts for an optional reason.

**US-15 — See an application timeline** · Must · 3 · Sprint 1 · Deps: US-14
> As a job seeker, I want a chronological timeline on the application detail page, so that I can see how it progressed.
- Newest-first vertical timeline combining status changes and interviews; each entry shows relative and absolute time; empty state when only the creation event exists.

**US-16 — Search applications** · Must · 3 · Sprint 2 · Deps: US-10
> As a job seeker, I want to search by company or role, so that I can find one application among hundreds.
- Case-insensitive substring match across company name and job title; debounced at 300 ms; result count shown; a no-results state distinct from the empty state.

**US-17 — Filter and sort applications** · Must · 5 · Sprint 2 · Deps: US-10
> As a job seeker, I want to combine filters, so that I can narrow to exactly the slice I care about.
- Filters on status, stage, priority, tag, work mode, and date range, combinable with AND semantics; active filters shown as removable chips; filter state encoded in the URL so a view can be bookmarked and shared with myself; "Clear all" resets.

**US-18 — Days-in-stage and stall flag** · Should · 5 · Sprint 3 · Deps: US-14
> As a job seeker, I want stalled applications flagged, so that I follow up before an opportunity dies quietly.
- Days-in-stage computed from the latest history entry; an application is flagged when it exceeds my own median dwell time for that stage (min 5 observations) or a documented default; the flag explains its own threshold and sample size on hover.

**US-19 — Tag and bulk-update applications** · Could · 5 · Sprint 5 · Deps: US-17
> As a job seeker, I want to tag applications and update several at once, so that managing a large campaign is not tedious.
- Free-form tags with autocomplete; multi-select in the table; bulk status change writes individual history rows for each affected application.

---

### EPIC 3 — Resume Management

**US-20 — Create a resume record** · Must · 3 · Sprint 2 · Deps: US-02
> As a job seeker, I want to register a resume version, so that I can distinguish the variants I send out.
- Name, version label, target role, and summary captured; one resume can be marked default; versions belong only to me.

**US-21 — Upload a resume file** · Must · 5 · Sprint 2 · Deps: US-20
> As a job seeker, I want to attach the actual PDF, so that I know exactly which document I sent.

- Accepts PDF and DOCX up to 5 MB.
- Validation checks extension, declared MIME type, **and magic bytes**; a `.exe` renamed to `.pdf` is rejected.
- Files are stored outside any statically served directory under a generated UUID filename; the original name is stored only as metadata.
- Upload shows progress, and failure shows a specific, actionable error.

**US-22 — Download my resume file** · Must · 3 · Sprint 2 · Deps: US-21
> As a job seeker, I want to download a stored resume, so that I can reuse it.
- Download requires authentication; the URL is not guessable and expires; requesting another user's file returns 404; the correct `Content-Disposition` and content type are set.

**US-23 — Structured resume content** · Should · 5 · Sprint 2 · Deps: US-20
> As a job seeker, I want to record skills, experience, education, and projects, so that the system has structure to reason about, not just a binary blob.
- Repeatable sections with add/remove/reorder; skills are tags; content saves independently of the file.

**US-24 — Attach a resume to an application** · Must · 3 · Sprint 2 · Deps: US-20, US-09
> As a job seeker, I want to record which resume I used for an application, so that I can later compare outcomes.
- Selectable from application create and edit forms; exactly one resume per application; the association is visible from both sides; changing it is allowed and logged in `updated_at`.

**US-25 — Clone a resume into a new version** · Should · 3 · Sprint 3 · Deps: US-23
> As a job seeker, I want to duplicate a resume as a new version, so that I can tailor it without starting over.
- Clone copies structured content, not the file; `parent_resume_id` records lineage; version label auto-increments.

**US-26 — Compare resume outcomes** · Should · 5 · Sprint 3 · Deps: US-24, US-43
> As a job seeker, I want to see how applications using each resume turned out, so that I can form a hypothesis about which is working.
- Per-resume counts and rates with explicit sample size; comparison is suppressed below 5 applications per version with an explanatory message; the panel carries a standing note that observed differences are associations and may reflect role, timing, or company mix.

---

### EPIC 4 — Interview Tracking

**US-27 — Schedule/record an interview** · Must · 5 · Sprint 2 · Deps: US-09
> As a job seeker, I want to record an interview against an application, so that my rounds are tracked in context.
- Date/time, round number, round type, duration, mode, and interviewer are captured; the interview must belong to one of my own applications (server-enforced); round numbers within an application are unique.

**US-28 — List and view interviews** · Must · 3 · Sprint 2 · Deps: US-27
> As a job seeker, I want to see all my interviews, upcoming and past, so that I can prepare and review.
- Split into upcoming and completed; each row links to its application; empty and loading states present; only my interviews are returned.

**US-29 — Record questions asked** · Must · 5 · Sprint 2 · Deps: US-27
> As a job seeker, I want to log the questions I was asked, so that I capture the most valuable output of the interview before I forget it.
- Multiple questions per interview, each with text, category, topic, difficulty, and a self-rating of how well I answered; questions can be added inline without leaving the page; question text is stored privately and never appears in any shared or federated surface.

**US-30 — Post-interview debrief** · Must · 5 · Sprint 2 · Deps: US-27
> As a job seeker, I want a guided debrief, so that I capture a structured reflection quickly rather than writing nothing.
- Captures overall self-rating (1–5), what went well, what went poorly, improvement areas, perceived difficulty, and outcome; partial completion is saved as a draft; completion in ≤3 minutes verified in usability testing; a prompt appears on the dashboard for interviews whose date has passed without a debrief.

**US-31 — Edit and delete an interview** · Must · 3 · Sprint 2 · Deps: US-27
> As a job seeker, I want to correct interview records, so that my history is accurate.
- All fields editable; deletion confirmed and cascades to its questions and feedback; cross-user access returns 404.

**US-32 — Record external recruiter feedback** · Could · 2 · Sprint 5 · Deps: US-30
> As a job seeker, I want to store feedback I was actually given, so that I can separate it from my own impressions.
- Stored in a distinct field from self-evaluation and visually distinguished; excluded from any federated feature.

**US-33 — Interview outcome suggests a status change** · Could · 3 · Sprint 3 · Deps: US-30, US-14
> As a job seeker, I want the app to suggest updating the application status after I record an outcome, so that I do not have to remember to do it twice.
- Suggestion is a prompt, never automatic; accepting writes a normal history entry; dismissing is remembered for that interview.

**US-34 — Derive tested topics from an interview** · Must · 3 · Sprint 3 · Deps: US-29
> As a job seeker, I want the topics I was tested on inferred from my recorded questions, so that preparation can target them without extra data entry.
- Topic set is the distinct topics of the interview's questions; displayed on the interview detail; feeds the mastery calculation.

---

### EPIC 5 — Interview Preparation & Topic Mastery

**US-35 — Browse the question bank** · Must · 5 · Sprint 3 · Deps: US-02
> As a job seeker, I want a searchable bank of practice questions, so that I have a starting point for preparation.
- Seeded with ≥150 questions across all categories; filter by category, topic, and difficulty; my own questions and seeded ones are visually distinguished.

**US-36 — Add my own questions** · Must · 3 · Sprint 3 · Deps: US-35
> As a job seeker, I want to add questions I encountered, so that my bank reflects my real interviews.
- Create/edit/delete my own; seeded questions are read-only but can be forked into a personal copy; questions recorded in an interview can be promoted into the bank in one click.

**US-37 — Record a practice attempt** · Must · 5 · Sprint 3 · Deps: US-35
> As a job seeker, I want to log practising a question, so that my progress is measured rather than remembered.
- Captures status (not started / in progress / confident / needs review), confidence 1–5, time taken, and notes; repeated attempts are appended, not overwritten, so a confidence trajectory exists.

**US-38 — Topic taxonomy with prerequisites** · Should · 5 · Sprint 3 · Deps: none (data work)
> As a developer, I want a topic graph with prerequisite edges, so that recommendations can identify root causes rather than symptoms.
- ~40–60 topics across categories, seeded and versioned; edges form a DAG with a cycle-detection test; every question links to exactly one topic.

**US-39 — Topic mastery score with decay** · Should · 8 · Sprint 3 · Deps: US-37, US-38, US-34
> As a job seeker, I want a mastery score per topic that fades if I stop practising, so that my readiness reflects reality rather than a checkbox I ticked in March.
- Score in [0,1] combining practice confidence and interview question ratings, with exponential time decay; the formula is documented, unit-tested against fixed fixtures, and explained in the UI on hover.

**US-40 — Create preparation tasks** · Must · 3 · Sprint 3 · Deps: US-38
> As a job seeker, I want a task list for preparation, so that intentions become scheduled work.
- Title, linked topic, due date, estimated effort, completion state; overdue tasks are visually marked; completion updates progress.

**US-41 — Preparation progress view** · Must · 5 · Sprint 3 · Deps: US-37, US-40
> As a job seeker, I want to see progress by topic and category, so that I know where I stand.
- Progress bars per category; a topic grid coloured by mastery with an accessible non-colour indicator; drill-down from category to topic to questions.

**US-42 — Spaced review queue** · Could · 5 · Sprint 5 · Deps: US-39
> As a job seeker, I want topics resurfaced as my mastery decays, so that I retain what I learned.
- Queue ordered by decayed mastery and interview relevance; capped at 5 items to stay actionable; items can be deferred.

---

### EPIC 6 — Analytics & Pipeline Health

**US-43 — Application funnel** · Must · 5 · Sprint 3 · Deps: US-14
> As a job seeker, I want a funnel from applications through to offers, so that I can see where my campaign leaks.
- Stages Applied → OA → Interview → Final → Offer with counts based on *stages ever reached* (from history), not current status; open applications are shown as still in flight rather than counted as failures; each stage links to its filtered application list.

**US-44 — Conversion rates** · Must · 5 · Sprint 3 · Deps: US-43
> As a job seeker, I want stage-to-stage conversion rates, so that I know which step to work on.
- All five rates computed per the definitions in §20.7; every rate displays its denominator; rates from fewer than 5 observations are marked low confidence and visually de-emphasised.

**US-45 — Activity over time** · Must · 3 · Sprint 3 · Deps: US-09
> As a job seeker, I want applications and interviews plotted over time, so that I can see my own consistency.
- Weekly buckets, applications and interviews as separate series; date-range selector; empty state for users with no history.

**US-46 — Rejection stage distribution** · Should · 3 · Sprint 3 · Deps: US-14
> As a job seeker, I want to see at which stage I am rejected, so that I can target the right skill.
- Distribution of the last stage reached before rejection; separates "rejected" from "no response" and never conflates them.

**US-47 — Topic performance** · Should · 5 · Sprint 4 · Deps: US-29, US-38
> As a job seeker, I want per-topic interview performance, so that I can see which topics actually hurt me in real rounds.
- Mean self-rating per topic weighted by recency, with question counts; topics with fewer than 3 questions are grouped as "insufficient data" rather than shown as extremes.

**US-48 — Stage dwell times** · Should · 3 · Sprint 4 · Deps: US-18
> As a job seeker, I want median time spent at each stage, so that I know what "normal" looks like for me.
- Median and interquartile range per stage from completed transitions only; right-censored applications excluded from the median and reported separately.

**US-49 — Dashboard pipeline strip and action queue** · Must · 8 · Sprint 3 · Deps: US-43, US-18
> As a job seeker, I want the dashboard to tell me what to do today, so that the app is a tool rather than a report.
- Top: compact pipeline strip with live counts. Below: a ranked action queue mixing stalled applications, missing debriefs, upcoming interviews, and due prep tasks; every item is one click from its action; the queue is capped at 7 items.

**US-50 — Sample-size honesty across analytics** · Must · 3 · Sprint 3 · Deps: US-44
> As a job seeker, I want to know how much data each number is based on, so that I do not over-read noise.
- Every rate, average, and median displays n; a shared low-confidence badge component is used consistently; a golden-file test pins outputs for a fixed fixture dataset.

---

### EPIC 7 — Explainable Recommendations

**US-51 — Weak-topic recommendations** · Must · 8 · Sprint 4 · Deps: US-39, US-47
> As a job seeker, I want to be told which topics to work on, so that preparation is targeted instead of generic.
- Ranked list of at most 5; each cites the observations behind it; topics with no supporting evidence are never recommended.

**US-52 — Root-cause prerequisite routing** · Should · 5 · Sprint 4 · Deps: US-51, US-38
> As a job seeker, I want to be pointed at the underlying gap rather than the symptom, so that I fix the cause.
- When a weak topic has a prerequisite with lower mastery, the recommendation targets the prerequisite and explains the chain; routing depth is capped at 2 to stay comprehensible.

**US-53 — Evidence tiers and explanations** · Must · 5 · Sprint 4 · Deps: US-51
> As a job seeker, I want to know where each recommendation came from and how confident it is, so that I can judge it myself.
- Every recommendation carries tier A/B/C/D, an observation count, and a plain-language "why am I seeing this?" expansion; a unit test asserts no recommendation renders without an explanation.

**US-54 — Non-causal language enforcement** · Must · 3 · Sprint 4 · Deps: US-53
> As a job seeker, I want the app to be honest about correlation, so that I am not misled about my own chances.
- All recommendation copy lives in a template registry; an automated test fails the build if any template matches the prohibited-phrasing list; associative phrasing is verified by review.

**US-55 — Act on a recommendation** · Should · 3 · Sprint 4 · Deps: US-51, US-40
> As a job seeker, I want to convert a recommendation into a task, snooze it, or dismiss it, so that the list stays relevant.
- Accept creates a linked preparation task; snooze hides it for 7 days; dismiss records a reason and suppresses that recommendation type for 30 days.

**US-56 — Stalled-application follow-up recommendations** · Should · 3 · Sprint 4 · Deps: US-18
> As a job seeker, I want to be nudged to follow up on stalled applications, so that opportunities do not lapse silently.
- Generated from the stall flag; includes the specific application and the dwell time; capped at 3 concurrently.

---

### EPIC 8 — Federated Learning & Privacy Ledger

**US-57 — Opt in to federated learning** · Must · 5 · Sprint 4 · Deps: US-05
> As a job seeker, I want to explicitly choose whether to contribute to the shared model, so that participation is my decision.
- Default off for every new user; the opt-in screen states in plain language what is sent and what is not, showing the actual feature schema; a test asserts that an opted-out client produces no update.

**US-58 — Local feature extraction** · Must · 8 · Sprint 4 · Deps: US-39, US-47
> As an FL administrator, I want a fixed numeric feature schema derived entirely on the client, so that no raw data is ever a candidate for transmission.
- Fixed-length, versioned, all-numeric vector; a serialisation test asserts the payload matches the allow-listed schema exactly and contains no strings; extraction runs against the client's own store only.

**US-59 — Local model training** · Must · 8 · Sprint 4 · Deps: US-58
> As an FL administrator, I want each client to train on its own data, so that learning happens without data movement.
- scikit-learn model trained in the client process; training examples never leave; per-client metrics logged locally; deterministic under a fixed seed.

**US-60 — Clipping and noise before transmission** · Must · 5 · Sprint 4 · Deps: US-59
> As a job seeker, I want my model update masked before it leaves my client, so that it cannot be reverse-engineered into my data.
- Update is L2-clipped to a configured norm, then Gaussian noise is added; pre- and post-values recorded in the ledger; a test asserts the transmitted vector differs from the raw update and respects the norm bound.

**US-61 — Federated aggregation with minimum cohort** · Must · 8 · Sprint 4 · Deps: US-60
> As an FL administrator, I want FedAvg aggregation that refuses small cohorts, so that averaging provides real anonymity.
- Flower server aggregates ≥k clients (default 3) and refuses below k with a logged reason; round metadata is persisted; the server holds no user-schema database credentials, verified in CI.

**US-62 — Global model distribution** · Must · 5 · Sprint 4 · Deps: US-61
> As a job seeker, I want my client to use the improved shared model, so that I benefit from collective experience.
- Clients fetch the current global model version; the version in use is displayed in the UI; a failed round leaves the previous model servable.

**US-63 — Privacy Ledger** · Should · 8 · Sprint 4 · Deps: US-60
> As a job seeker, I want a log of everything my client has ever sent, so that I can verify the privacy claim instead of trusting it.
- One entry per round: round ID, timestamp, schema version, pre/post-clip norm, noise multiplier, cohort size, epsilon consumed, and the exact transmitted vector, expandable; a "what was not sent" list is shown; entries are immutable.

**US-64 — Withdraw from federation** · Must · 3 · Sprint 4 · Deps: US-57
> As a job seeker, I want to stop contributing at any time, so that consent is reversible.
- Takes effect from the next round; historical contributions are explained honestly as already aggregated and non-retractable; ledger remains visible after withdrawal.

**US-65 — Federated Lift viewer** · Should · 5 · Sprint 4 · Deps: US-62, US-51
> As a job seeker, I want to compare my recommendations with and without the shared model, so that I can see what federation actually contributes.
- A toggle re-ranks recommendations live; changed items are highlighted with their rank delta; the comparison works offline against a cached global model.

**US-66 — Privacy budget meter** · Could · 5 · Sprint 5 · Deps: US-63
> As a job seeker, I want to see my cumulative privacy budget, so that I understand the cost of participating.
- Displays total and spent epsilon and remaining rounds; participation halts at exhaustion with a clear explanation; posture selection (Strict/Balanced/Contributor) adjusts parameters and shows the trade-off.

---

### EPIC 9 — Platform, Quality & DevOps

*Justification for adding a ninth EPIC beyond the eight suggested:* infrastructure, CI, seeding, and security hardening are real work that consumes real capacity. If they are not in the backlog they are invisible, velocity is overstated, and they get skipped under deadline pressure — the single most common failure mode in student projects. Making them first-class stories also gives every team member a legitimate, reviewable contribution surface in Sprint 1.

**US-67 — Repository and toolchain bootstrap** · Must · 5 · Sprint 1
> As a developer, I want a configured repository, so that the team can work in parallel without conflicts.
- Monorepo structure, `.gitignore`, `.env.example`, licence, README skeleton, branch protection on `main`, PR template, commit convention documented; every member can clone and run.

**US-68 — Docker Compose development environment** · Must · 5 · Sprint 1
> As a developer, I want one-command startup, so that "works on my machine" never happens.
- `docker compose up` starts Postgres, backend, and frontend with hot reload; verified independently by every team member on their own OS; documented in the README.

**US-69 — Database schema and migrations** · Must · 8 · Sprint 1
> As a developer, I want versioned migrations, so that schema changes are reproducible and reversible.
- Alembic configured; Sprint 1 tables created by migration, not by `create_all`; up and down both tested in CI.

**US-70 — CI pipeline** · Must · 5 · Sprint 1
> As a developer, I want automated checks on every PR, so that quality is enforced rather than hoped for.
- GitHub Actions runs lint, type-check, backend tests with coverage, and frontend tests; a failing check blocks merge; the run completes in under 5 minutes.

**US-71 — Application shell and navigation** · Must · 5 · Sprint 1
> As a job seeker, I want consistent navigation and layout, so that the product feels coherent.
- Sidebar with the nine sections, responsive collapse below 768 px, active-route highlighting, protected route wrapper, 404 page, global error boundary, toast system.

**US-72 — Seed and demo data** · Should · 3 · Sprint 2
> As a developer, I want realistic seed data, so that features can be demonstrated and analytics tested without hand-entry.
- One command seeds a demo user with ~60 applications, 20 interviews, 3 resumes, and practice history; data is deterministic under a fixed seed and clearly marked synthetic.

**US-73 — Security test suite** · Must · 5 · Sprint 5 · Deps: EPIC 1–5
> As a developer, I want automated cross-user access tests, so that isolation is proven, not assumed.
- For every owned resource type, a test asserts that user B receives 404 for user A's object; unauthenticated access to every non-public endpoint returns 401; the suite is enumerated from the OpenAPI schema so new endpoints cannot be forgotten.

**US-74 — Accessibility and responsive pass** · Should · 5 · Sprint 5
> As a job seeker with accessibility needs, I want the app to be keyboard and screen-reader usable, so that I can actually use it.
- axe-core reports zero critical violations on the main pages; full keyboard walkthrough passes; layouts verified at 360/768/1280/1920 px.

**US-75 — Deployment and demo readiness** · Must · 5 · Sprint 5
> As a developer, I want a rehearsed deployment and demo path, so that the final presentation does not fail on logistics.
- Production Compose profile builds and runs; backup and restore rehearsed; a scripted demo path is timed and practised end to end.

---

## 10. Product Backlog

### 10.1 Master backlog

| ID | User Story (abbrev.) | EPIC | Priority | Points | Sprint |
|---|---|---|---|---:|---|
| US-67 | Repository & toolchain bootstrap | 9 | Must | 5 | 1 |
| US-68 | Docker Compose dev environment | 9 | Must | 5 | 1 |
| US-69 | Database schema & migrations | 9 | Must | 8 | 1 |
| US-70 | CI pipeline | 9 | Must | 5 | 1 |
| US-71 | App shell & navigation | 9 | Must | 5 | 1 |
| US-01 | Register an account | 1 | Must | 5 | 1 |
| US-02 | Log in | 1 | Must | 3 | 1 |
| US-03 | Log out | 1 | Must | 2 | 1 |
| US-04 | Session persistence & expiry | 1 | Must | 3 | 1 |
| US-05 | View & edit profile | 1 | Must | 3 | 1 |
| US-09 | Create an application | 2 | Must | 5 | 1 |
| US-10 | List applications | 2 | Must | 5 | 1 |
| US-11 | View application detail | 2 | Must | 3 | 1 |
| US-12 | Edit an application | 2 | Must | 3 | 1 |
| US-13 | Delete an application | 2 | Must | 3 | 1 |
| US-14 | Change status & record history | 2 | Must | 5 | 1 |
| US-15 | Application timeline | 2 | Must | 3 | 1 |
| **Sprint 1 total** | | | | **71** | |
| US-16 | Search applications | 2 | Must | 3 | 2 |
| US-17 | Filter & sort applications | 2 | Must | 5 | 2 |
| US-20 | Create a resume record | 3 | Must | 3 | 2 |
| US-21 | Upload a resume file | 3 | Must | 5 | 2 |
| US-22 | Download resume file | 3 | Must | 3 | 2 |
| US-23 | Structured resume content | 3 | Should | 5 | 2 |
| US-24 | Attach resume to application | 3 | Must | 3 | 2 |
| US-27 | Record an interview | 4 | Must | 5 | 2 |
| US-28 | List & view interviews | 4 | Must | 3 | 2 |
| US-29 | Record questions asked | 4 | Must | 5 | 2 |
| US-30 | Post-interview debrief | 4 | Must | 5 | 2 |
| US-31 | Edit & delete an interview | 4 | Must | 3 | 2 |
| US-72 | Seed & demo data | 9 | Should | 3 | 2 |
| **Sprint 2 total** | | | | **51** | |
| US-18 | Days-in-stage & stall flag | 2 | Should | 5 | 3 |
| US-25 | Clone resume version | 3 | Should | 3 | 3 |
| US-26 | Compare resume outcomes | 3 | Should | 5 | 3 |
| US-33 | Outcome suggests status change | 4 | Could | 3 | 3 |
| US-34 | Derive tested topics | 4 | Must | 3 | 3 |
| US-35 | Browse question bank | 5 | Must | 5 | 3 |
| US-36 | Add own questions | 5 | Must | 3 | 3 |
| US-37 | Record practice attempt | 5 | Must | 5 | 3 |
| US-38 | Topic taxonomy with prerequisites | 5 | Should | 5 | 3 |
| US-39 | Topic mastery with decay | 5 | Should | 8 | 3 |
| US-40 | Preparation tasks | 5 | Must | 3 | 3 |
| US-41 | Preparation progress view | 5 | Must | 5 | 3 |
| US-43 | Application funnel | 6 | Must | 5 | 3 |
| US-44 | Conversion rates | 6 | Must | 5 | 3 |
| US-45 | Activity over time | 6 | Must | 3 | 3 |
| US-46 | Rejection stage distribution | 6 | Should | 3 | 3 |
| US-49 | Dashboard strip & action queue | 6 | Must | 8 | 3 |
| US-50 | Sample-size honesty | 6 | Must | 3 | 3 |
| **Sprint 3 total** | | | | **80** | |
| US-47 | Topic performance analytics | 6 | Should | 5 | 4 |
| US-48 | Stage dwell times | 6 | Should | 3 | 4 |
| US-51 | Weak-topic recommendations | 7 | Must | 8 | 4 |
| US-52 | Root-cause prerequisite routing | 7 | Should | 5 | 4 |
| US-53 | Evidence tiers & explanations | 7 | Must | 5 | 4 |
| US-54 | Non-causal language enforcement | 7 | Must | 3 | 4 |
| US-55 | Act on a recommendation | 7 | Should | 3 | 4 |
| US-56 | Stall follow-up recommendations | 7 | Should | 3 | 4 |
| US-57 | Opt in to federated learning | 8 | Must | 5 | 4 |
| US-58 | Local feature extraction | 8 | Must | 8 | 4 |
| US-59 | Local model training | 8 | Must | 8 | 4 |
| US-60 | Clipping & noise | 8 | Must | 5 | 4 |
| US-61 | Aggregation with min cohort | 8 | Must | 8 | 4 |
| US-62 | Global model distribution | 8 | Must | 5 | 4 |
| US-63 | Privacy Ledger | 8 | Should | 8 | 4 |
| US-64 | Withdraw from federation | 8 | Must | 3 | 4 |
| US-65 | Federated Lift viewer | 8 | Should | 5 | 4 |
| **Sprint 4 total** | | | | **90** | |
| US-06 | Change password | 1 | Should | 3 | 5 |
| US-07 | Export my data | 1 | Should | 3 | 5 |
| US-08 | Delete my account | 1 | Should | 5 | 5 |
| US-19 | Tags & bulk update | 2 | Could | 5 | 5 |
| US-32 | External recruiter feedback | 4 | Could | 2 | 5 |
| US-42 | Spaced review queue | 5 | Could | 5 | 5 |
| US-66 | Privacy budget meter | 8 | Could | 5 | 5 |
| US-73 | Security test suite | 9 | Must | 5 | 5 |
| US-74 | Accessibility & responsive pass | 9 | Should | 5 | 5 |
| US-75 | Deployment & demo readiness | 9 | Must | 5 | 5 |
| **Sprint 5 total** | | | | **43** | |
| | | | **TOTAL** | **335** | |

### 10.2 Capacity check — read this before approving

335 points across 5 sprints averages to **67 points per sprint**, but it isn't evenly spread — Sprint 4 is the heaviest at **90 points**. At 12–15 points per person per 2-week sprint:

| Team size | Capacity/sprint | Verdict |
|---:|---:|---|
| 2 | 24–30 | Not viable. Cut to Sprints 1–3 + a minimal FL demo; drop EPIC 7 sophistication. |
| 3 | 36–45 | Tight. Drop all *Could* items and US-23, US-26, US-42, US-66, US-19 (≈28 pts). Feasible. |
| 4 | 48–60 | Workable with Sprint 3 and 4 rebalanced (move US-47/US-48 to Sprint 5). |
| 5 | 60–75 | Comfortable; the plan as written fits. |

**Tell me the team size and I will rebalance the sprints before implementation begins.** Sprints 3 and 4 as written are the overloaded ones; the release valve is the *Could* column plus US-23, US-26, US-42, US-66, and US-19, in that order.

### 10.3 MVP definition

The MVP is everything marked **Must**, plus these **Should** items that carry the project's identity: US-18, US-38, US-39, US-52, US-63, US-65.

Rationale: a Must-only build would be a competent tracker with generic recommendations and an invisible FL backend — exactly the commodity project §2.3 sets out to avoid. The six additions are the differentiators, and they are also the cheapest possible versions of them.

### 10.4 Future scope (not in MVP)

Anything marked *Future* in §32, plus: US-19, US-32, US-42, US-66 if capacity forces cuts.

---

## 11. EPICs

| EPIC | Name | Stories | Points | Primary sprints | Business value |
|---|---|---|---:|---|---|
| 1 | Authentication & User Management | 8 | 27 | 1, 5 | Gate for everything; privacy foundation |
| 2 | Job Application Tracking | 11 | 45 | 1, 2, 3, 5 | The core daily-use loop |
| 3 | Resume Management | 7 | 27 | 2, 3 | Links documents to outcomes |
| 4 | Interview Tracking | 8 | 29 | 2, 3, 5 | Captures the highest-value data in the system |
| 5 | Interview Prep & Topic Mastery | 8 | 39 | 3, 5 | Turns experience into action |
| 6 | Analytics & Pipeline Health | 8 | 35 | 3, 4 | Makes performance legible |
| 7 | Explainable Recommendations | 6 | 27 | 4 | The personalisation payoff |
| 8 | Federated Learning & Privacy Ledger | 10 | 60 | 4, 5 | The innovation and the privacy story |
| 9 | Platform, Quality & DevOps | 9 | 46 | 1, 2, 5 | Makes the rest deliverable |

Deviations from the suggested EPIC list, with reasons:
- **EPIC 5 renamed** from "Interview Preparation" to "Interview Prep & Topic Mastery" because the mastery graph is the substantive content, not a sub-feature.
- **EPIC 6 renamed** to "Analytics & Pipeline Health" to make stall detection a first-class concern rather than a stray chart.
- **EPIC 8 merged** FL and Privacy, because a privacy design specified separately from the FL design produces a privacy policy rather than a privacy architecture. They are one system.
- **EPIC 9 added** — justified above in §9.

Every user story maps to exactly one EPIC; the mapping is in the §10.1 table.

---

## 12. Sprint Roadmap

Assumption: five 2-week sprints, roughly 10–12 weeks including a hardening finish.

### Sprint 1 — Foundation (see §13 for the full plan)
**Goal:** A running, deployable skeleton in which a user can register, log in, and manage applications end to end — plus the concept poster.
**Deliverables:** Compose environment, CI, migrations, auth, application CRUD with status history, app shell, poster.
**Dependencies:** None (greenfield).
**Risks:** Environment setup consumes more time than estimated on mixed OSes; the team is learning the stack. *Mitigation:* Compose parity verified by every member on day 3; a spike day is budgeted.
**DoD:** §30 project-wide DoD, plus: every member has merged at least one reviewed PR; `docker compose up` works on every member's machine; poster reviewed by all.

### Sprint 2 — Resumes & Interview Capture
**Goal:** Users can manage resume versions, attach them to applications, and capture structured interview experience including questions and a fast debrief.
**Stories:** US-16, US-17, US-20 → US-24, US-27 → US-31, US-72.
**Tasks:** File-upload pipeline with magic-byte validation; storage abstraction; resume schema and endpoints; interview and question models; debrief flow; search and filter with URL state; seed script.
**Deliverables:** Resumes, Resume Detail, Interviews, Interview Detail pages; secure file upload/download; seeded demo dataset.
**Dependencies:** Sprint 1 auth and applications.
**Risks:** File upload security is easy to get subtly wrong. *Mitigation:* Write the malicious-upload test before the feature. Debrief UX may fail the 3-minute target. *Mitigation:* Time it with 3 users mid-sprint and cut fields if needed.
**DoD:** Upload rejects a renamed executable; cross-user file access returns 404; debrief timed under 3 minutes with 3 users.

### Sprint 3 — Preparation & Analytics
**Goal:** The system turns recorded history into measured preparation progress and a legible campaign picture, with a dashboard that recommends action rather than displaying numbers.
**Stories:** US-18, US-25, US-26, US-33, US-34, US-35 → US-41, US-43 → US-46, US-49, US-50.
**Tasks:** Topic taxonomy seed with cycle test; question bank seed (≥150); practice attempt model; mastery scoring module with golden-file tests; analytics service with documented formulas; Recharts components; dashboard action queue.
**Deliverables:** Question Bank, Interview Prep, Analytics pages; redesigned Dashboard; mastery engine.
**Dependencies:** Sprint 2 interview questions; seed data.
**Risks:** Analytics correctness is hard to eyeball and easy to get wrong. *Mitigation:* Golden-file tests on a fixed fixture dataset, written first. Sprint 3 is the heaviest at 80 points. *Mitigation:* US-25, US-26, US-33 are the designated drop candidates.
**DoD:** Every metric has a documented formula and a passing golden-file test; every chart has empty, loading, and error states.

### Sprint 4 — Recommendations & Federated Learning
**Goal:** Explainable, evidence-tiered recommendations, and a genuinely federated learning loop with a user-visible Privacy Ledger.
**Stories:** US-47, US-48, US-51 → US-65.
**Tasks:** Recommendation rule engine with template registry and the phrasing linter; prerequisite routing; feature extractor with schema allow-list test; Flower client and server; clipping and noise; FedAvg with min-cohort enforcement; synthetic cohort client generator; ledger model and UI; lift viewer.
**Deliverables:** Recommendations page; Privacy & FL page with ledger; running Flower server and ≥5 client containers; documented FL evaluation.
**Dependencies:** Sprint 3 mastery scores and topic analytics (features depend on them).
**Risks:** **Highest-risk sprint.** Flower integration friction; too little real data to train on; convergence may look poor with non-IID synthetic clients. *Mitigations:* FL spike completed during Sprint 3 (see §13.11); synthetic cohort generator built in Sprint 3 as part of US-72's extension; success is defined as a *correct and private* pipeline, not a high accuracy number, and that definition is agreed with the evaluator in advance.
**DoD:** A round with fewer than k clients is refused and logged; the payload schema test passes; the FL server container demonstrably holds no user-schema credentials; the ledger shows a real transmitted vector.

### Sprint 5 — Hardening, Privacy Controls & Delivery
**Goal:** A secure, accessible, documented, deployable product with a rehearsed demonstration.
**Stories:** US-06, US-07, US-08, US-19, US-32, US-42, US-66, US-73, US-74, US-75.
**Tasks:** Full security test suite; accessibility remediation; account deletion with orphan checks; data export; production Compose profile; backup/restore rehearsal; final documentation, README, ADR consolidation, user guide; demo script.
**Deliverables:** Final report, complete test suite meeting coverage gates, deployed instance, rehearsed demo.
**Dependencies:** Everything.
**Risks:** Bug backlog from Sprints 3–4 crowds out hardening. *Mitigation:* 20% of Sprint 5 capacity is reserved for defects from the start and is not planned against stories.
**DoD:** Coverage gates met; zero critical axe violations; deletion leaves no orphans; restore rehearsed; demo run end to end twice without intervention.

---

## 13. Sprint 1 — Detailed Plan

This is the sprint that starts on approval, so it is specified to task level.

### 13.1 Sprint goal

> *A user can register, log in, and manage their job applications with a full status history, running in a reproducible Docker environment with CI, migrations, and a concept poster — the vertical slice that proves the architecture works end to end.*

### 13.2 Stories included

US-67, US-68, US-69, US-70, US-71 (platform); US-01 → US-05 (auth); US-09 → US-15 (applications). **66 points.**

Note the deliberate shape: this is a *vertical* slice through every layer (React → API → service → repository → Postgres → migration → test → CI → container), not a horizontal one. It de-risks the architecture in week two rather than week ten.

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

**In:** registration, login, logout, refresh rotation, Argon2id hashing, password policy with blocklist, JWT issuing and verification, `get_current_user` dependency, protected routes, auth rate limiting, per-user query scoping.
**Out (later sprints):** password reset by email, change password (Sprint 5), account deletion (Sprint 5), OAuth (never), MFA (future), admin role (Sprint 5 if capacity allows).

### 13.8 Testing scope for Sprint 1

- **Unit:** password hashing and verification; token creation, expiry, and rotation; status state-machine validity; application service create/update/delete; days-in-stage helper.
- **API:** all 16 endpoints — happy path, validation failure, unauthenticated (401), and cross-user access (404).
- **Cross-user isolation:** a fixture creating users A and B; A's application must return 404 to B on GET, PATCH, DELETE, and status change. **This test is written in Sprint 1 and is the template for US-73.**
- **Frontend (RTL):** login form validation and submission; application form validation; DataTable renders rows, empty state, and loading state; ProtectedRoute redirects when unauthenticated.
- **Integration:** register → login → create application → change status → verify history, run against a real Postgres test container.
- **Coverage gate:** ≥70% backend line coverage enforced in CI from Sprint 1, not retrofitted later.

### 13.9 Documentation scope for Sprint 1

`README.md` (setup, run, test, structure); `docs/requirements.md` (FR/NFR/DR from §6–8); `docs/architecture.md` with the C4-style diagrams from §16; `docs/database.md` with the ERD; `docs/api.md` (or published OpenAPI); `docs/agile/` containing product backlog, sprint 1 plan, daily standup log, sprint review and retrospective notes; `docs/adr/` with ADRs for the first decisions (Argon2id over bcrypt; refresh-token rotation; per-user query scoping at the repository layer; Alembic from day one); `CONTRIBUTING.md` with branch and commit conventions.

### 13.10 Concept poster content (Sprint 1 deliverable)

See §29 for full layout. Sprint 1 task: produce the finished A1 poster as PDF plus source, reviewed by the whole team.

### 13.11 One thing to schedule now for later: the FL spike

Add a **timeboxed 1-day Flower spike** to Sprint 3 (not Sprint 4): two hard-coded clients, a trivial 3-feature logistic regression, one FedAvg round, nothing else. Its only purpose is to surface Flower version and API friction a full sprint before Sprint 4 depends on it. This single scheduling decision is the highest-leverage risk reduction in the whole plan.

### 13.12 Sprint 1 Definition of Done

Project-wide DoD (§30), plus:
- `docker compose up` starts Postgres, backend, and frontend on **every** team member's machine.
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

### C1 — Privacy vs Analytics richness

**Conflicting:** FR-58…FR-69 (rich analytics), FR-71 (weak-topic identification) vs NFR-10, NFR-16, DR-18, DR-21.
**Why they conflict:** Better analytics and better models want more data, more granular data, and data pooled across users. Every one of those increases exposure and re-identification risk, and job-search data is career-sensitive.
**Decision:** **Locality wins over richness.** Personal analytics are computed only from the requesting user's own data. Cross-user learning happens only through federated model parameters, never through pooled records.
**Resolution:** Two-tier design — Tier 1 (personal analytics, full fidelity, never leaves the user's partition) and Tier 2 (federated learning, fixed numeric schema, clipped and noised). Feature-schema minimisation is enforced by an allow-list test (NFR-10). Data minimisation reviewed at each sprint review (NFR-16).
**Impact:** Personal analytics have low statistical power for new users; the system compensates with honest sample-size labelling (FR-67) and heuristic Tier-D recommendations rather than fabricated precision. Federated features are coarser than centralised ones, so the global model will be weaker than a centralised equivalent — an accepted, documented trade.

### C2 — Centralised ML vs Federated privacy

**Conflicting:** Ease and accuracy of centralised training vs FR-79…FR-92, DR-21, and the brief's hard constraint.
**Why they conflict:** Centralised training over a single database is dramatically simpler, converges faster, and produces better metrics. FL adds a server, N client processes, orchestration, DP noise, and non-IID convergence problems, for a strictly worse accuracy number.
**Decision:** **FL, unconditionally.** It is the project's stated innovation and a mandatory requirement; simulating it over pooled data would be dishonest.
**Resolution:** MVP FL is a *real* Flower deployment with genuinely isolated client data stores (§22), sized down rather than faked: small model, few clients, few rounds, synthetic cohort peers. Success is redefined as **architectural correctness and demonstrable privacy**, not accuracy. This redefinition is agreed with the evaluator in advance so the criterion is not contested at the end.
**Impact:** Higher engineering cost, concentrated in Sprint 4, mitigated by the Sprint 3 spike. Model quality will be modest and must be reported honestly, including the FL-vs-centralised gap — which is itself a good result to discuss in the report.

### C3 — Feature richness vs deliverability

**Conflicting:** The full FR set vs a fixed academic deadline and student capacity.
**Why they conflict:** The requirement list describes a product a funded team would build over months. Attempting all of it produces a broad, broken system, which scores worse than a narrow, working one.
**Decision:** **Depth over breadth.** Ship a smaller set of features that are complete, tested, and polished.
**Resolution:** MoSCoW prioritisation (§10); an MVP defined as Must + six identity-carrying Shoulds (§10.3); a pre-agreed cut list in priority order (§10.2); an explicit capacity check before approval; a strict Definition of Done that makes "nearly finished" impossible to claim.
**Impact:** Several attractive features (bulk operations, spaced review, privacy budget meter, external feedback) land in Future Scope. Documented deliberately in the report as scope management, which reads as competence rather than omission.

### C4 — Personalisation vs standardisation

**Conflicting:** FR-70…FR-77 (individually tailored recommendations) vs FR-79 (fixed feature schema) and FR-59 (comparable, standard metrics).
**Why they conflict:** Personalisation wants each user's idiosyncratic signal; federated learning requires every client to share an identical, fixed-length schema; and comparable metrics require standard definitions applied uniformly.
**Decision:** **Standard schema, personal weighting.** The vocabulary is shared; the emphasis is individual.
**Resolution:** A shared, versioned topic taxonomy and feature schema provide the common ground. Personalisation comes from the user's own mastery scores, decay state, and history determining *which* topics surface and in what order. The Federated Lift viewer (US-65) makes the boundary between the shared model and the personal ranking visible to the user.
**Impact:** Users cannot invent arbitrary topics that participate in federated learning; free-form topics are supported locally but excluded from the feature vector. This limitation is disclosed on the opt-in screen.

### C5 — Security vs usability

**Conflicting:** NFR-01…NFR-09 (short tokens, strong passwords, rate limits, confirmations) vs NFR-30…NFR-35 (fast, low-friction use).
**Why they conflict:** Every security control adds a step. A 15-minute token naively implemented means being logged out mid-form.
**Decision:** **Move friction to the boundary, remove it from the interior.** Be strict at registration, login, and destructive actions; be invisible everywhere else.
**Resolution:** Silent refresh-token rotation (US-04) makes short access tokens imperceptible. The password policy is enforced with live, constructive feedback rather than a post-submit rejection. Confirmation dialogs appear only for destructive and irreversible actions, and they name the object. Rate limiting applies to auth endpoints only. FL opt-in is a single explicit decision, not a per-round prompt.
**Impact:** More frontend work (token refresh interceptor, request queueing during refresh) in exchange for a materially better experience. Documented as an ADR.

### C6 — Honest uncertainty vs actionable confidence

**Conflicting:** FR-67, FR-74, DR-06, DR-08, DR-14 (statistical honesty) vs G3 and NFR-30 (users want a clear answer).
**Why they conflict:** Hedged output is useless output. "Your graph performance may possibly be weak (n=3, not statistically significant)" helps nobody. But confident output from n=3 is a lie.
**Decision:** **Be decisive about the action, honest about the evidence.** Recommend one concrete next step; label the confidence behind it plainly.
**Resolution:** The evidence-tier system (D3/FR-73): the headline is an imperative ("Practise BFS and DFS this week"), and the tier badge plus expandable "why am I seeing this?" carries the uncertainty. Below the minimum sample threshold the system says "not enough data yet" rather than guessing. Prohibited causal phrasing is enforced by a failing test (FR-74).
**Impact:** Every recommendation needs both an action template and an evidence explanation, roughly doubling the copy work in EPIC 7. Worth it — this is a defining characteristic of the product.

### C7 — FL model utility vs differential privacy noise

**Conflicting:** FR-83/FR-86 (a useful global model) vs FR-81/FR-82/FR-90 (clipping, noise, budget) and DR-21.
**Why they conflict:** Noise sufficient for a meaningful epsilon can overwhelm the signal, especially with few clients and small local datasets. This is the central tension of practical DP and it does not have a clean resolution.
**Decision:** **Privacy parameters are fixed first; utility is measured against them.** We do not tune epsilon upward to make the accuracy chart look better.
**Resolution:** Defaults chosen from literature (clipping norm S=1.0, noise multiplier σ=1.0, k=3) and held constant. A utility-vs-privacy sweep is run as an *experiment* and reported as a finding, including the honest result that at small client counts the privacy cost is high relative to the utility gained. The three user-facing postures expose the trade-off rather than hiding it.
**Impact:** Reported model performance will be modest. Framed correctly this is a strength: measuring and reporting a real privacy–utility trade-off is a more sophisticated result than an unexplained high accuracy number, and it is defensible under questioning.

---

## 15. Technology Stack

### 15.1 Stack as specified, with justification

| Layer | Technology | Why it is the right choice here |
|---|---|---|
| UI library | **React 18** | The interface is a stateful dashboard with tables, filters, modals, and charts — exactly the component-tree problem React solves. Largest ecosystem, so the team can find help fast, which matters on a deadline. |
| Language (FE) | **TypeScript** | The domain has many similar-shaped entities (application, interview, resume) and a compiler that catches a field mismatch at build time is worth more than a debugging session. Types generated from the OpenAPI schema keep the frontend and backend contract honest automatically. |
| Styling | **Tailwind CSS** | Utility classes keep styling colocated with markup, which suits a small team with no dedicated designer, and enforce a design-token scale (spacing, colour, type) so the UI stays consistent without a style guide nobody reads. |
| Components | **shadcn/ui** | Accessible Radix primitives delivered as owned source rather than an opaque dependency. This matters twice: NFR-44…NFR-47 come largely for free, and the components can be restyled to avoid the default look flagged in §2.3. |
| Charts | **Recharts** | Declarative React charts covering funnel, bar, line, and area needs. Composable enough to add the sample-size annotations FR-67 requires, without the learning cost of D3. |
| Backend framework | **FastAPI** | Pydantic validation satisfies FR-96 by construction; automatic OpenAPI generation feeds both the frontend types and `docs/api.md`; dependency injection makes `get_current_user` and per-user scoping clean; async support handles the FL-server calls without blocking. Being Python, it shares a runtime with scikit-learn and Flower — no cross-language bridge anywhere in the system. |
| Language (BE) | **Python 3.11+** | Mandatory for the ML and FL layer; unifying the backend on it removes an entire class of integration problem. |
| Database | **PostgreSQL 16** | The domain is genuinely relational — users own applications own interviews own questions — and the isolation requirement is best enforced with real foreign keys and constraints. Also gives JSONB for semi-structured resume content, window functions for dwell-time and funnel queries, and per-schema privilege separation, which is what makes the FL client isolation in §22 architecturally real rather than cosmetic. |
| ORM | **SQLAlchemy 2.0** | Parameterised queries by default (NFR-03), typed models, and a repository layer where per-user scoping is enforced once rather than remembered at every call site (FR-94). |
| Auth | **JWT + Argon2id** | JWT keeps the API stateless (NFR-23) while refresh tokens stored server-side keep logout genuinely revocable (FR-06). **Argon2id over bcrypt**: memory-hard, resistant to GPU cracking, and the current OWASP recommendation. Recorded as ADR-001. |
| ML | **scikit-learn** | Right-sized for the task. Its linear models expose `coef_` and `intercept_` as plain arrays, which is precisely what a FedAvg implementation needs to average — a deep-learning framework would be heavier and no more capable here. |
| FL | **Flower (flwr)** | Framework-agnostic, works cleanly with scikit-learn, has a real client/server separation over gRPC (so clients are genuinely separate processes, not a loop), supports custom strategies for the min-cohort and norm-filtering rules in FR-83/FR-84, and its simulation mode gives a fallback if the containerised deployment misbehaves near the deadline. |
| Testing (BE) | **Pytest** | Fixtures model the multi-user setups the isolation tests need; `pytest-cov` provides the CI gate; parametrisation covers the state machine cheaply. |
| Testing (FE) | **React Testing Library + Vitest** | Tests behaviour rather than implementation, which survives refactoring. Vitest shares the Vite config, so there is no second toolchain. |
| Containers | **Docker + Docker Compose** | Removes environment drift across a mixed-OS team (NFR-41) and — critically — is what makes the FL client isolation demonstrable: separate containers, separate credentials, inspectable. |
| VCS | **Git + GitHub** | Required. Also provides Actions for CI, Projects for the sprint board, and a per-member contribution record. |

### 15.2 Minimal additions, each justified

These are not new technology choices so much as unavoidable companions to the ones above. Nothing here expands the stack conceptually.

| Addition | Layer | Why it is necessary |
|---|---|---|
| **Vite** | FE build | React needs a bundler; Vite is the current default and shares config with Vitest. |
| **React Router** | FE routing | Multi-page SPA requires routing. |
| **TanStack Query** | FE data | Server-state caching, request deduplication, and — the deciding reason — a clean place to implement the single-flight token-refresh interceptor from US-04. Hand-rolling this is more code and worse. |
| **React Hook Form + Zod** | FE forms | The application, resume, interview, and debrief forms are large; Zod schemas mirror the Pydantic models so validation rules are stated once per side. |
| **Alembic** | BE migrations | Part of the SQLAlchemy ecosystem. NFR-27 requires reversible versioned migrations; `create_all` cannot provide them. |
| **Argon2-cffi** | BE auth | The Argon2id implementation. |
| **python-jose** or **PyJWT** | BE auth | JWT signing and verification. |
| **pytest-asyncio, httpx, testcontainers** | BE test | Async test support, API test client, and a real Postgres instance for integration tests. |
| **Ruff, Black, mypy, ESLint, Prettier** | Quality | Enforce NFR-38 and NFR-39 in CI. |
| **NumPy** | ML | Transitive under scikit-learn; used directly for clipping and noise arithmetic. |

### 15.3 Explicitly rejected

| Rejected | Why |
|---|---|
| Redis | Nothing needs a cache or queue at this scale; rate limiting can use an in-process store or a Postgres table. Adding it costs a container and a failure mode for no benefit. |
| Celery / background workers | FL rounds are triggered manually or on a schedule; FastAPI background tasks suffice. |
| MinIO / S3 | Local volume storage is adequate for the MVP; the storage layer is abstracted behind an interface so S3 is a future swap, not a rewrite. |
| Next.js | Server-side rendering brings no benefit to an authenticated dashboard and complicates the Compose setup. |
| PyTorch / TensorFlow | Over-powered for a logistic-regression-scale problem and would triple image size. |
| An LLM API | Would make the "AI" feature a network call to someone else's model, undermining both the FL contribution and the privacy story. Deliberately out. |
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
   No external integrations. No third-party data sharing. No outbound
   user data of any kind.
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
│  FastAPI Backend  (container: api)                                   │
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
│  ── Cross-cutting: JWT auth · Pydantic validation · rate limiting    │
│     structured logging · error handling · file storage adapter       │
└───────────┬───────────────────────────────────┬──────────────────────┘
            │ SQLAlchemy                        │ HTTP (control plane
            ▼                                   │  only — no user data)
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

### 16.3 Component responsibilities

| Component | Responsibility | Explicitly not responsible for |
|---|---|---|
| **SPA** | Rendering, client-side validation, routing, server-state caching, token refresh | Authorisation decisions, business rules, any computation the API can be trusted to do |
| **API layer** | HTTP concerns: routing, deserialisation, status codes, auth extraction | Business logic, direct database access |
| **Service layer** | Business rules, orchestration, state machine, scoring, metric computation | HTTP concerns, SQL construction |
| **Repository layer** | Data access, always parameterised, **always scoped to `user_id`** | Business decisions |
| **MasteryEngine** | Topic mastery scoring with decay; prerequisite traversal | Persistence, presentation |
| **AnalyticsService** | Funnel, conversions, dwell times, distributions, sample-size annotation | Cross-user computation of any kind |
| **RecommendationEngine** | Candidate generation, evidence tiering, ranking, template rendering | Model training, causal claims |
| **FLCoordinator** | Round scheduling, model version registry, ledger writes, serving the global model to the app | Training; touching client raw data |
| **FL Client** | Local feature extraction, local training, clipping, noise, transmission | Sending anything but parameters |
| **Flower Server** | FedAvg aggregation, min-cohort enforcement, norm filtering, round metadata | Reading user data — it has no credentials to do so |

### 16.4 Data flow — the two loops

**Loop A: normal request.** Browser sends a request with a bearer token → API validates the token, resolves `current_user` → router calls a service → the service calls repositories, which inject `user_id` into every query → Postgres returns only that user's rows → the service computes → Pydantic serialises → the browser renders. *User data never leaves the app boundary.*

**Loop B: federated round.** FLCoordinator opens round *r* and publishes global model *M(r-1)* → each opted-in client fetches *M(r-1)* → each client reads **only its own schema**, extracts a fixed 24-float feature matrix locally, and fits a local model → computes the update Δ, clips it to ‖Δ‖₂ ≤ S, adds Gaussian noise N(0, σ²S²I) → sends the noised vector over gRPC → the Flower server counts participants, refuses if < k, filters over-norm updates, computes the FedAvg mean → *M(r)* is versioned and stored → FLCoordinator writes round metadata and a per-user ledger entry → clients fetch *M(r)*. *No record, string, or identifier crosses the boundary at any point.*

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

### 16.6 Privacy boundaries — what crosses what

| Data | Stays on client | Processed locally | In model update | Aggregated | Ever shown globally |
|---|---|---|---|---|---|
| Email, password hash | ✅ | ✅ | ❌ | ❌ | ❌ |
| Company names | ✅ | ✅ | ❌ | ❌ | ❌ |
| Job descriptions, notes | ✅ | ✅ | ❌ | ❌ | ❌ |
| Resume files and content | ✅ | ✅ | ❌ | ❌ | ❌ |
| Interview question text | ✅ | ✅ | ❌ | ❌ | ❌ |
| Interviewer names | ✅ | ✅ | ❌ | ❌ | ❌ |
| Individual outcomes | ✅ | ✅ | ❌ | ❌ | ❌ |
| Bucketed topic mastery (derived) | ✅ | ✅ | ✅ (as a feature) | ✅ | ❌ |
| Normalised practice counts (derived) | ✅ | ✅ | ✅ | ✅ | ❌ |
| Model parameters, clipped + noised | — | ✅ | ✅ | ✅ | Only as the global model |
| Round metadata (count, timestamp) | — | — | — | ✅ | ✅ (aggregate only) |

The single sentence for the poster: **only clipped, noised model parameters ever cross the boundary, and every user can read their own outgoing vector in the Privacy Ledger.**

---

## 17. Database Design

### 17.1 Entity–relationship overview

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

**users** — `email CITEXT UNIQUE NOT NULL`, `password_hash TEXT NOT NULL`, `display_name`, `target_role`, `graduation_year SMALLINT`, `timezone`, `role` (`user`|`admin`), `is_active BOOLEAN DEFAULT true`, `fl_opt_in BOOLEAN NOT NULL DEFAULT false`, `fl_privacy_posture` (`strict`|`balanced`|`contributor`, default `balanced`), `epsilon_spent NUMERIC DEFAULT 0`.

**refresh_tokens** — `user_id FK→users ON DELETE CASCADE`, `token_hash UNIQUE`, `expires_at`, `revoked_at NULL`. Index on `token_hash`.

**companies** — `name`, `normalized_name`, `website`, `industry`, `created_by_user_id FK→users`. Unique (`created_by_user_id`, `normalized_name`). *Design note:* companies are user-scoped rather than global. A global company table would create a subtle cross-user inference channel (company creation timestamps leak who is applying where) and would need deduplication logic nobody has time to write. Scoping per user costs some duplication and buys clean isolation. Recorded as ADR-004.

**applications** — `user_id FK→users NOT NULL ON DELETE CASCADE`, `company_id FK→companies NOT NULL`, `resume_id FK→resumes ON DELETE SET NULL`, `job_title NOT NULL`, `job_description`, `job_url`, `location`, `work_mode` (`onsite`|`hybrid`|`remote`), `job_type` (`internship`|`full_time`|`part_time`|`contract`), `salary_min NUMERIC`, `salary_max NUMERIC`, `salary_currency CHAR(3)`, `date_applied DATE NOT NULL`, `status` (enum, §17.5), `current_stage`, `priority` (`low`|`medium`|`high`), `source`, `is_referral BOOLEAN`, `deadline DATE`, `recruiter_name`, `recruiter_email`, `notes TEXT`, `cover_letter TEXT`, `archived_at`, `stage_entered_at TIMESTAMPTZ NOT NULL`.
Constraints: `CHECK (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)`, `CHECK (date_applied <= CURRENT_DATE)`.
Indexes: `(user_id, status)`, `(user_id, date_applied DESC)`, `(user_id, company_id)`, `(user_id, archived_at)`.

**application_status_history** — `application_id FK ON DELETE CASCADE`, `from_status`, `to_status NOT NULL`, `note`, `changed_at NOT NULL`. **No `updated_at`, no update endpoint — append-only by design.** Index `(application_id, changed_at DESC)`. This table is the source of truth for the funnel (§20.7): current status alone cannot tell you an application *reached* the interview stage before being rejected.

**tags** / **application_tags** — `tags(user_id FK, name)` unique per user; join table with composite PK.

**resumes** — `user_id FK NOT NULL`, `name NOT NULL`, `version_label`, `target_role`, `summary`, `is_default BOOLEAN`, `parent_resume_id FK→resumes NULL` (lineage), `file_path`, `file_original_name`, `file_size_bytes`, `file_mime`, `file_uploaded_at`. Partial unique index enforcing one default per user.

**resume_sections** — `resume_id FK ON DELETE CASCADE`, `section_type` (`experience`|`education`|`project`|`achievement`|`skill`), `title`, `organisation`, `start_date`, `end_date`, `description`, `sort_order`, `metadata JSONB`. *Design note:* one flexible table rather than five near-identical ones, because the sections share a shape and JSONB absorbs the differences.

**interviews** — `application_id FK ON DELETE CASCADE NOT NULL`, `user_id FK NOT NULL` (denormalised deliberately, so every isolation query is a single-table filter and can never be forgotten in a join), `scheduled_at`, `completed_at`, `round_number SMALLINT NOT NULL`, `round_type` (enum), `duration_minutes`, `mode` (`onsite`|`video`|`phone`), `interviewer_name`, `interviewer_role`, `outcome` (`passed`|`failed`|`pending`|`unknown`), `perceived_difficulty SMALLINT 1–5`, `notes`. Unique (`application_id`, `round_number`).

**interview_questions** — `interview_id FK ON DELETE CASCADE`, `user_id FK`, `question_text NOT NULL`, `topic_id FK→topics NULL`, `category` (enum), `difficulty` (`easy`|`medium`|`hard`), `self_rating SMALLINT 1–5`, `time_taken_minutes`, `linked_question_id FK→questions NULL`, `notes`.

**interview_feedback** — `interview_id FK UNIQUE ON DELETE CASCADE`, `self_rating SMALLINT 1–5 NOT NULL`, `what_went_well`, `what_went_poorly`, `improvement_areas`, `external_feedback`, `submitted_at`, `is_draft BOOLEAN`. One-to-one with interview.

**topics** — `name UNIQUE`, `slug`, `category` (enum), `description`, `default_difficulty`, `taxonomy_version SMALLINT`. Seeded, system-owned.

**topic_prerequisites** — `topic_id FK`, `prerequisite_topic_id FK`, composite PK. **Must remain acyclic** — enforced by a seed-time cycle-detection test, since Postgres cannot express this as a constraint.

**questions** (bank) — `user_id FK NULL` (NULL = system-seeded), `topic_id FK`, `category`, `difficulty`, `question_text NOT NULL`, `expected_answer`, `source`, `is_system BOOLEAN`, `forked_from_id FK→questions NULL`. Index `(user_id, topic_id)`, `(category, difficulty)`.

**practice_attempts** — `user_id FK NOT NULL`, `question_id FK NOT NULL`, `status` (`not_started`|`in_progress`|`confident`|`needs_review`), `confidence SMALLINT 1–5`, `time_taken_minutes`, `notes`, `attempted_at NOT NULL`. **Append-only** — repeated attempts create rows, giving a confidence trajectory rather than a single overwritten value. Index `(user_id, question_id, attempted_at DESC)`.

**user_topic_mastery** — `user_id FK`, `topic_id FK`, `mastery_score NUMERIC(4,3)` in [0,1], `raw_score`, `last_practiced_at`, `last_interviewed_at`, `observation_count`, `computed_at`. Unique (`user_id`, `topic_id`). A materialised cache recomputed on write to practice or interview questions; the formula is in §21.4.

**preparation_tasks** — `user_id FK`, `topic_id FK NULL`, `title NOT NULL`, `description`, `due_date`, `estimated_minutes`, `is_completed`, `completed_at`, `source_recommendation_id FK→recommendations NULL`, `priority`.

**recommendations** — `user_id FK`, `type` (`weak_topic`|`prerequisite_gap`|`stalled_application`|`resume_pattern`|`prep_cadence`), `title`, `body`, `evidence_tier CHAR(1)` in (A,B,C,D), `observation_count`, `score NUMERIC`, `target_topic_id FK NULL`, `target_application_id FK NULL`, `model_version` NULL, `state` (`active`|`accepted`|`snoozed`|`dismissed`), `snoozed_until`, `dismissed_reason`, `generated_at`.

**recommendation_evidence** — `recommendation_id FK ON DELETE CASCADE`, `evidence_type`, `description`, `source_table`, `source_id`, `weight`. This is what makes "why am I seeing this?" a database-backed answer rather than a generated sentence.

**fl_models** — `version INT UNIQUE`, `model_type`, `feature_schema_version`, `parameters BYTEA` (serialised coefficients), `metrics JSONB`, `client_count`, `is_active BOOLEAN`, `created_at`.

**fl_rounds** — `round_number INT UNIQUE`, `model_version_in FK→fl_models`, `model_version_out FK→fl_models NULL`, `participant_count`, `min_cohort_required`, `clipping_norm NUMERIC`, `noise_multiplier NUMERIC`, `status` (`pending`|`running`|`aggregated`|`refused`|`failed`), `refusal_reason`, `started_at`, `completed_at`.

**fl_client_state** — `user_id FK UNIQUE`, `client_id`, `last_round_participated`, `local_model_version`, `last_trained_at`, `local_sample_count`, `is_enabled`.

**privacy_ledger_entries** — `user_id FK`, `fl_round_id FK`, `feature_schema_version`, `raw_update_norm NUMERIC`, `clipped_norm NUMERIC`, `noise_multiplier NUMERIC`, `epsilon_consumed NUMERIC`, `cohort_size INT`, `transmitted_vector JSONB`, `created_at`. **Append-only, never updated or deleted except by account deletion.** This table is the product feature described in D2.

### 17.3 Key relationships explained

- **User → Applications (1:∞).** The central ownership edge. `user_id` is NOT NULL with no default and is set server-side from the token; the client can never supply it. Every repository method for applications takes `user_id` as a required parameter — it is impossible to write a query that forgets it, because the function signature will not compile without it.
- **Application → Company (∞:1).** Many applications may target the same company (DR-03), so uniqueness is never enforced on (user, company).
- **Application → Resume (∞:1, nullable).** One resume per application (FR-34); one resume serves many applications, which is what makes §20.7's resume outcome analysis possible. `ON DELETE SET NULL` so deleting a resume does not destroy application history.
- **Application → Interviews (1:∞, cascade).** Interviews have no meaning without their application, so cascade is correct. `round_number` unique within the application enforces DR-04's variable-length rounds without imposing a fixed count.
- **Interview → Questions (1:∞) and → Feedback (1:1).** Questions are the atomic unit of topic attribution (DR-09), which is why topic linkage lives here rather than on the interview.
- **User → Recommendations → Evidence.** Every recommendation carries its provenance rows, satisfying FR-73 structurally rather than by convention.
- **User → PrivacyLedger → Round.** Each entry is scoped to one user and one round; a user sees only their own entries, and even an administrator sees only aggregate round metadata.

### 17.4 How isolation is actually enforced (three independent layers)

1. **Schema:** `user_id NOT NULL` on every owned table, foreign-keyed with cascade.
2. **Repository:** a `UserScopedRepository` base class whose every method requires `user_id`; route handlers cannot call the raw session. An import-boundary lint rule prevents routers from importing models directly.
3. **Test:** the US-73 suite enumerates every endpoint from the OpenAPI schema and asserts a 404 for cross-user access, so a newly added endpoint that forgets scoping fails CI rather than shipping.

Belt, braces, and an alarm. Any one of these can fail without a breach.

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
 genuinely skip stages (DR-04).
 Backward transitions are rejected with 409, except one documented
 exception: any status may move to a *previous* status if a correction
 note is supplied, because users do mis-click. The correction is written
 to history like any other change, so the audit trail stays truthful.
 Terminal ──▶ non-terminal is rejected outright.
```

Encoding this as an explicit transition table (rather than free-text statuses) is what makes FR-21, the funnel, and the dwell-time analytics all computable. It is a small piece of design that pays for itself three times.

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
| `GET /applications/{id}` | Detail | — | 200 with company, resume, history, interviews | 401, **404 for foreign or missing** |
| `PATCH /applications/{id}` | Update | Partial payload | 200 | 400, 401, 404, 422 |
| `DELETE /applications/{id}` | Delete | — | 204, cascades | 401, 404 |
| `POST /applications/{id}/status` | Change status | `{to_status, note?}` | 200 with new history entry | 401, 404, **409 invalid transition** |
| `GET /applications/{id}/history` | Timeline | — | 200 list, newest first | 401, 404 |
| `POST /applications/bulk-status` | Bulk change | `{application_ids[], to_status}` | 200 `{updated, failed[]}` | 401, 422 |

**Filtering is server-side.** Returning everything and filtering in the browser would violate NFR-20 and leak the full dataset into memory.

### 18.4 Resumes

`GET /resumes` · `POST /resumes` · `GET /resumes/{id}` · `PATCH /resumes/{id}` · `DELETE /resumes/{id}` · `POST /resumes/{id}/file` (multipart, magic-byte validated, 413 over 5 MB, 415 wrong type) · `GET /resumes/{id}/file` (authenticated, expiring, `Content-Disposition: attachment`) · `POST /resumes/{id}/clone` · `GET /resumes/{id}/performance` (returns counts, rates, `sample_size`, and `sufficient_data: bool`; when false, rates are omitted entirely rather than shown with a warning — you cannot un-see a number).

### 18.5 Interviews & questions

`GET /interviews` (filter `application_id`, `upcoming`, `round_type`) · `POST /interviews` · `GET|PATCH|DELETE /interviews/{id}` · `GET|POST /interviews/{id}/questions` · `PATCH|DELETE /interview-questions/{id}` · `GET|PUT /interviews/{id}/feedback` (PUT is upsert, supports draft) · `POST /interview-questions/{id}/promote` (adds to personal bank) · `GET /interviews/{id}/topics` (derived).

### 18.6 Question bank & preparation

`GET /questions` (filter `category`, `topic_id`, `difficulty`, `is_system`, `q`) · `POST /questions` · `PATCH|DELETE /questions/{id}` (own only) · `POST /questions/{id}/fork` · `POST /questions/{id}/attempts` · `GET /questions/{id}/attempts` · `GET /topics` · `GET /topics/graph` (nodes + prerequisite edges) · `GET /me/mastery` (per-topic scores with decay applied and `observation_count`) · `GET|POST /preparation/tasks` · `PATCH|DELETE /preparation/tasks/{id}` · `GET /preparation/progress`.

### 18.7 Analytics

Rejecting the single `GET /analytics` suggested in the brief in favour of resource-scoped endpoints: one endpoint returning everything is slow, uncacheable, hard to test, and forces the client to over-fetch for a single chart.

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

All accept `date_from`, `date_to`, `tag[]`. **Every response includes a `sample_size` and a `sufficient_data` boolean per metric** (FR-67) — the honesty requirement is enforced in the response schema, not left to the UI.

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
| `POST /fl/opt-in` | `{enabled, posture}` — informed consent | User | Default off |
| `POST /fl/opt-out` | Withdraw | User | Effective next round |
| `GET /fl/ledger` | The user's ledger entries, paginated | User | Includes transmitted vectors |
| `GET /fl/ledger/export` | JSON export | User | |
| `GET /fl/model` | Current global model metadata and parameters | Client/admin | Never contains user data |
| `POST /fl/rounds` | Start a round | Admin | Returns 409 if a round is running |
| `GET /fl/rounds` | Round history with participant counts | User (metadata only) | |
| `GET /fl/schema` | The current feature schema, human-readable | Public | Shown on the opt-in screen — the schema is documentation |

**`POST /fl/update` is deliberately absent.** Client updates travel over Flower's gRPC channel to the Flower server, not through the application API. Routing updates through the app backend would put user-linked model updates in the same process that holds database credentials, weakening exactly the boundary the design exists to create. This is a considered deviation from the endpoint list in the brief, recorded as ADR-007.

---

## 19. Frontend Architecture

### 19.1 Structure

Feature-first, not type-first. `features/applications/` holds that feature's pages, components, hooks, and API client together; shared primitives live in `components/ui/`. This keeps related code adjacent and makes parallel work on different features nearly conflict-free — which matters directly for the GitHub contribution requirement.

### 19.2 State strategy

| Kind of state | Handled by |
|---|---|
| Server data | TanStack Query (caching, invalidation, background refetch) |
| Auth session | React Context + secure token storage |
| Form state | React Hook Form + Zod |
| Filter/sort/pagination | URL search params (bookmarkable, shareable, survives refresh) |
| Ephemeral UI (modals, toasts) | Local `useState` / a small context |

No Redux. There is no state complex enough to justify it, and adding it would be a self-inflicted wound.

### 19.3 Pages

| Page | Route | Purpose | Main components | Data required | User actions |
|---|---|---|---|---|---|
| **Login** | `/login` | Authenticate | `AuthCard`, `LoginForm` | — | Submit, navigate to register |
| **Register** | `/register` | Create account | `AuthCard`, `RegisterForm`, `PasswordStrength` | — | Submit |
| **Dashboard** | `/` | Answer "what should I do today?" | `PipelineStrip`, `ActionQueue`, `UpcomingInterviews`, `RecentActivity` | `/analytics/summary`, stalled apps, pending debriefs, due tasks, upcoming interviews | Act on a queue item, quick-add application |
| **Applications** | `/applications` | Manage the campaign | `DataTable`, `FilterBar`, `StatusBadge`, `BulkActionBar`, `ApplicationFormModal` | Paginated applications, tags, companies | Create, filter, search, sort, bulk update, open detail |
| **Application Detail** | `/applications/:id` | Everything about one application | `DetailHeader`, `FieldPanel`, `StatusChanger`, `Timeline`, `InterviewList`, `ResumeChip`, `NotesPanel` | Application with relations | Edit, change status, add interview, attach resume, delete |
| **Resumes** | `/resumes` | Version library | `ResumeCard` grid, `UploadDropzone`, `VersionBadge` | Resumes with usage counts | Create, upload, clone, set default, delete |
| **Resume Detail** | `/resumes/:id` | One version | `SectionEditor`, `FilePreview`, `UsageList`, `PerformancePanel` | Resume, sections, linked applications, performance | Edit sections, replace file, download, clone |
| **Interviews** | `/interviews` | Upcoming and past | `InterviewTable`, `UpcomingPanel`, `DebriefPrompt` | Interviews with application context | Create, open, start debrief |
| **Interview Detail** | `/interviews/:id` | One round | `InterviewHeader`, `QuestionList`, `QuestionForm`, `FeedbackPanel`, `TopicChips` | Interview, questions, feedback, derived topics | Add questions, complete debrief, promote question to bank |
| **Interview Prep** | `/prep` | Practice hub | `TopicGrid`, `MasteryLegend`, `TaskList`, `ProgressBars`, `ReviewQueue` | Mastery scores, tasks, progress | Start practice, complete task, open topic |
| **Question Bank** | `/questions` | Browse and practise | `QuestionTable`, `CategoryFilter`, `PracticeModal`, `ConfidenceRating` | Questions, attempts | Filter, practise, record attempt, add/fork |
| **Analytics** | `/analytics` | Campaign performance | `FunnelChart`, `ConversionCards`, `TimelineChart`, `RejectionChart`, `TopicPerformance`, `SampleSizeBadge`, `DateRangePicker` | All analytics endpoints | Change range, filter by tag, drill into a stage |
| **Recommendations** | `/recommendations` | What to do next | `RecommendationCard`, `EvidenceTierBadge`, `EvidenceDrawer`, `FederatedLiftToggle` | Recommendations with evidence | Accept, snooze, dismiss, toggle federated, expand evidence |
| **Privacy & FL** | `/privacy` | Control and verify | `OptInPanel`, `PostureSelector`, `PrivacyLedgerTable`, `VectorInspector`, `BudgetMeter`, `RoundHistory`, `WhatWasNotSent` | FL status, ledger, rounds, schema | Opt in/out, choose posture, inspect a vector, export ledger |
| **Profile** | `/profile` | Account | `ProfileForm`, `PasswordChange`, `DataExport`, `DangerZone` | Current user | Update, change password, export, delete account |
| **404** | `*` | Not found | `NotFound` | — | Return home |

### 19.4 Cross-cutting frontend concerns

- **Route protection:** `ProtectedRoute` wrapper redirects unauthenticated users, preserving the intended destination.
- **Token refresh:** a single-flight interceptor in the TanStack Query client; concurrent 401s queue behind one refresh.
- **Error boundaries:** one global boundary plus per-route boundaries so a broken chart does not blank the page.
- **Loading:** skeletons matching final layout, not spinners — layout shift is the main perceived-slowness culprit.
- **Optimistic updates:** on status change and task completion, with rollback on failure.
- **Type safety:** API types generated from the backend OpenAPI schema into `types/api.ts`, regenerated in CI so drift breaks the build rather than production.

---

## 20. UI/UX Plan

### 20.1 Design direction

The brief for the visual language: *an analyst's tool, not a marketing site*. Dense, calm, information-first — closer to a well-built internal dashboard than to a landing page. Concretely, and in deliberate contrast to the defaults flagged in §2.3:

- **Neutral base with a single accent.** Slate/zinc greys carrying the interface; one accent colour used only for primary actions and the active nav item. Status colour is confined to badges.
- **A real typographic scale.** A distinctive text face (e.g. Söhne, Geist, or IBM Plex Sans) paired with a monospace face for numbers, dates, and IDs — tabular figures throughout so columns of numbers align. This one choice does more to make the product look considered than any amount of colour work.
- **Sharp, not soft.** Small radii (4–6 px), hairline borders, near-flat surfaces. No large shadows, no gradients, no glassmorphism.
- **Density by default** with a comfortable-mode toggle. The primary user has 100+ applications; a card grid with 3 items per row is the wrong shape for that.
- **Charts styled down.** Muted palette, no drop shadows, gridlines at 10% opacity, direct labelling in preference to legends.

### 20.2 Navigation

Persistent left sidebar, collapsible to icons, collapsing to a bottom bar below 768 px:

```
  Cohort
  ──────────────────
  ◈  Dashboard
  ▤  Applications        12
  ▢  Resumes
  ◉  Interviews           3
  ✎  Interview Prep
  ▲  Analytics
  ✦  Recommendations      5
  ⚿  Privacy / FL
  ──────────────────
  ○  Profile
```

Counts are live and meaningful: open applications, upcoming interviews, active recommendations. A badge that never changes is decoration; a badge that changes is information.

### 20.3 Component patterns

| Pattern | Specification |
|---|---|
| **Data table** | Sticky header, zebra-free with hairline row borders, sortable headers with direction indicator, row hover, click opens a detail drawer (list context preserved) with a "full page" affordance, 25 rows per page |
| **Status badge** | Text + subtle background + a leading dot; never colour alone (NFR-47). Saved (grey), Applied (blue), OA (indigo), Interview (violet), Final (amber), Offer (green), Rejected (red), Withdrawn (grey outline) |
| **Filter bar** | Inline chips above the table showing active filters, each removable; "Clear all"; state in the URL |
| **Dashboard pipeline strip** | Single horizontal band of stage counts with connecting arrows and a subtle width-proportional fill — the funnel as a header, not a chart |
| **Action queue** | Ranked list, each row an icon, one-line description, and a single button. Max 7 items. Empty state: "Nothing needs your attention today." |
| **Progress bar** | Track + fill + numeric label; on the topic grid, mastery shown as a filled ring with an accessible numeric value |
| **Forms** | Single column, logical grouping, inline validation on blur, disabled submit while pending, field-level server errors, unsaved-changes guard |
| **Modals** | Focus trapped and restored, Esc closes, backdrop click closes non-destructive dialogs only, max 560 px wide |
| **Confirmation dialogs** | Name the object ("Delete your application to Stripe — Backend Engineer?"), state the consequence, destructive button on the right and styled as destructive |
| **Empty states** | Illustration or icon, one sentence of explanation, one primary action. Different copy for "no data yet" vs "no results for this filter" |
| **Loading** | Skeletons matching final layout; charts show axes with a shimmering plot area |
| **Error states** | What happened, what to do, a retry button. Never a raw status code |
| **Toasts** | Bottom-right, auto-dismiss at 4 s, with an undo action where the operation is reversible |

### 20.4 Signature screens (the ones the demo lives on)

1. **Dashboard action queue** — proves the product is a tool, not a report.
2. **Application detail timeline** — proves history is captured, not overwritten.
3. **Topic mastery grid** — a colour-and-value grid of ~50 topics that reads at a glance, with prerequisite edges revealed on hover.
4. **Recommendation card with the evidence drawer open** — proves explainability.
5. **Privacy Ledger with a vector expanded** — the single most distinctive screen in the project.

### 20.5 Responsive behaviour

| Breakpoint | Behaviour |
|---|---|
| ≥1280 px | Full sidebar, full table columns, side-by-side detail drawer |
| 768–1279 px | Icon sidebar, secondary table columns dropped, drawer becomes full-width sheet |
| <768 px | Bottom nav, table becomes a card list, charts stack, filters move into a sheet |

### 20.6 Accessibility commitments

WCAG 2.1 AA contrast; full keyboard operability with visible focus; labelled inputs with `aria-describedby` error linkage; charts accompanied by a data table view; status conveyed by text as well as colour; `prefers-reduced-motion` respected; axe-core in CI with zero critical violations on the main pages.

### 20.7 Analytics: metric definitions

Every displayed number needs an unambiguous formula, or the analytics are not verifiable. These definitions are authoritative and are pinned by golden-file tests.

**Stage attainment.** An application has *reached* stage S if `application_status_history` contains any row with `to_status = S` **or** a status ordered after S. Computed from history, never from current status — an application currently `Rejected` that previously reached `Interview` must count in the interview stage, or the funnel is meaningless (DR-01).

**Funnel counts.**
- `applied = COUNT(applications WHERE status != 'Saved')`
- `reached_oa = COUNT(applications that ever reached OA or beyond)`
- `reached_interview`, `reached_final`, `reached_offer` — same construction.

**Conversion rates.** Each rate is *reached next stage ÷ reached this stage*, over **resolved applications only** — an application still awaiting a response is neither a success nor a failure and is reported separately as `still_open` (DR-02, DR-05).
- `application_to_oa = reached_oa / applied_resolved`
- `oa_to_interview = reached_interview / reached_oa_resolved`
- `interview_to_final = reached_final / reached_interview_resolved`
- `final_to_offer = reached_offer / reached_final_resolved`
- `overall_success = reached_offer / applied_resolved`

Every rate returns `{value, numerator, denominator, sufficient_data}`, where `sufficient_data = denominator >= 5`. When false, the UI shows the counts and suppresses the percentage (FR-67).

**Days in stage.** `now() - stage_entered_at`, where `stage_entered_at` is the `changed_at` of the most recent history row.

**Median dwell time per stage.** Median of `(next_transition_time - stage_entry_time)` over **completed** transitions only. Applications currently sitting in a stage are right-censored and excluded from the median, reported separately as `censored_count`. Reported with IQR; suppressed below 5 completed observations.

**Stall flag.** An application is stalled when `days_in_stage > max(user_median_dwell[stage] × 1.5, system_default[stage])`, requiring ≥5 observations for the personal median; otherwise the system default applies and the UI says which basis was used.

**Rejection stage distribution.** For each rejected application, the last stage reached before the rejection row. Applications with no terminal status and no activity for 60 days are reported as `no_response`, **never merged into rejections** (DR-02).

**Topic performance.** For topic *t*: recency-weighted mean of `self_rating` across `interview_questions` with `topic_id = t`, weight `exp(-λ · days_ago)` with λ chosen for a 90-day half-life. Requires ≥3 questions; otherwise reported as insufficient data.

**Resume outcome rate.** For resume *r*: `applications using r that reached ≥ Interview ÷ resolved applications using r`. Suppressed below 5 (DR-14, FR-37), and always displayed with the standing note that differences may reflect role, timing, or company mix rather than the document.

**Preparation progress.** Per category: `questions with ≥1 attempt at confidence ≥4 ÷ total questions attempted in that category`. Deliberately measured against attempted rather than available questions, so the denominator is the user's own scope of work rather than the size of the seed bank.

---

## 21. ML Recommendation Engine

### 21.1 Design philosophy

The engine is **rules-first with a learned re-ranker**, not a black box. Three reasons, all of which matter for a university project:

1. **Explainability is a requirement**, not a nice-to-have (FR-73). A rule that fires on identifiable evidence rows can explain itself exactly; a learned model can only be rationalised after the fact.
2. **Cold start is the normal case.** A new user has zero interviews. A pure ML system produces nothing useful for weeks; rules produce something sensible on day one and improve as evidence accumulates.
3. **The FL contribution stays visible.** Because the learned model only *re-ranks* candidates that the rules generated, the federated contribution can be isolated, measured, and toggled (US-65). If ML generated the candidates, the lift viewer would be impossible to build.

### 21.2 Pipeline

```
 [1] Candidate generation   rules over the user's own data
             │              → 10–30 candidates, each with evidence rows
             ▼
 [2] Evidence tiering       A (n≥5 personal) · B (2–4 personal)
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
| `BaselineGenerator` (Tier D) | The user has fewer than 3 interviews logged | None — heuristic, and says so |

### 21.4 Topic mastery formula

For user *u* and topic *t*:

**Raw evidence score.** Combine two sources, each normalised to [0,1]:

```
practice_component(u,t) = Σ_a w_a · (confidence_a - 1)/4      /   Σ_a w_a
interview_component(u,t) = Σ_q w_q · (self_rating_q - 1)/4     /   Σ_q w_q

where w = exp(-ln(2) · days_ago / H)      (exponential recency weight)
      H = 90 days for practice, 120 days for interview evidence
```

**Blend**, favouring real interviews over self-directed practice, because interview evidence is scarcer and more diagnostic:

```
raw(u,t) = 0.4 · practice_component + 0.6 · interview_component
           (falls back to whichever component has evidence when only one does)
```

**Time decay on the blended score**, so a topic untouched for months is not reported as mastered (DR-16):

```
Δ = days since last evidence of any kind for (u,t)
mastery(u,t) = raw(u,t) · exp(-ln(2) · Δ / H_diff)

H_diff = 60 days (hard topics) · 90 (medium) · 120 (easy)
```

**Confidence gating.** With `observation_count < 2`, the score is stored but reported as *insufficient evidence* rather than as a number — this prevents a single bad question from branding a topic as a weakness.

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

The 0.15 margin prevents routing on noise. The rendered recommendation shows the chain, which is what makes it feel intelligent: *"Shortest-path questions have gone poorly (3 rounds). Your graph-traversal mastery is lower still (0.31 vs 0.46), so start there — BFS and DFS before Dijkstra."*

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

The federated adjustment is bounded in [0.75, 1.25] — deliberately constrained so the shared model can *reorder* recommendations but never invent or suppress one. A user's own evidence always dominates. This is both a privacy-conscious and a product-quality decision, and it is what makes the lift viewer show interesting but not alarming movement.

### 21.7 Template registry and phrasing enforcement

All user-facing recommendation copy lives in one registry module. Each template declares its type, required evidence fields, and both the headline (imperative) and explanation (associative) strings.

The test `test_no_causal_language` iterates every template and fails on any match against the prohibited list: *guarantees, will get you, ensures, because of this you were rejected, caused, proven to, always leads to, you failed because*. A second test asserts every template renders with its declared evidence fields present. A third asserts no template renders without an explanation.

Encoding an epistemic standard as a build gate is unusual, cheap, and exactly the kind of detail that reads well in a viva.

### 21.8 What the machine-learning model actually is

| Property | Choice |
|---|---|
| Task | Binary classification: given a user's preparation state before an interview round, predict whether that round was self-rated ≥4 |
| Honest framing | The model predicts **self-reported confidence**, not hiring success. DR-08 makes this distinction mandatory, and it is stated in the UI and the report. |
| Model | `sklearn.linear_model.SGDClassifier(loss='log_loss')` — logistic regression |
| Why linear | Parameters are a flat, fixed-length float array, which is what FedAvg averages; coefficients are inspectable, so the global model can be discussed rather than merely reported; it is robust with tiny per-client datasets, where anything deeper would simply memorise |
| Training example | One completed interview round |
| Features | The 24 features in §22.2, computed **as of that round's date** |
| Point-in-time correctness | Feature extraction takes an `as_of` timestamp and excludes all later records. Without this, mastery computed today would leak the outcome into a round from three months ago — the classic silent leakage bug, and one an examiner may well probe |
| Parameters exchanged | `coef_` (24 floats) + `intercept_` (1 float) = **25 floats per round, per client** |

### 21.9 Evaluation

- **Offline:** stratified k-fold on the synthetic cohort, reporting AUC, accuracy, and Brier score against a majority-class baseline. If the model does not beat the baseline, that is reported as the finding, not hidden.
- **Ablation:** rules-only vs rules + local model vs rules + federated model, which is the quantitative backing for the Federated Lift feature.
- **Ranking quality:** overlap between the top-5 recommendations and the topics that actually appeared in the user's subsequent interviews, measured on held-out synthetic timelines.
- **Honest reporting:** the report states sample sizes, states that synthetic data validates the *mechanism* rather than the *finding*, and does not present synthetic-data metrics as evidence about real job seekers.

---

## 22. Federated Learning Architecture

### 22.1 The learning task

**What is learned:** the association between a user's *preparation state* (a 24-dimensional derived feature vector) and *self-reported interview round performance*.

**Why this task and not another.** It satisfies DR-19 — it is learnable from a fixed numeric vector, needs no raw text, and requires no cross-user joins. It is genuinely useful: knowing which preparation patterns correlate with better-rated rounds is exactly the cross-user insight no single user can obtain alone, since each has only their own handful of interviews. And it is honestly bounded: the label is self-reported, which is disclosed everywhere the model is surfaced.

**Rejected alternatives, and why:** learning over question *text* (would require raw text transmission — violates the core constraint); predicting hiring outcomes (labels too sparse, too confounded, and would invite exactly the causal claims §14/C6 forbids); learning company-specific patterns (would identify companies and, transitively, users).

### 22.2 Feature schema v1 — the complete list of what leaves a client

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
| 11 | `share_dsa` | Category mix — DSA | [0,1] |
| 12 | `share_technical` | Category mix — technical | [0,1] |
| 13 | `share_behavioural` | Category mix — behavioural | [0,1] |
| 14 | `share_system_design` | Category mix — system design | [0,1] |
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

**What is provably absent:** no strings of any kind, no user or record identifiers, no company names, no job titles, no dates or timestamps, no free text, no interviewer names, no resume content, no question text, no salary, no location. The serialisation test asserts the payload is exactly 24 floats with these names, in this order, and fails on any addition.

**Why bucketing and log-scaling matter.** A raw count of 137 applications is close to a fingerprint; `log1p(137)/log1p(300)` is not. Every count-like feature is compressed for this reason, not for model performance.

### 22.3 Client architecture and isolation

Each FL client is a separate container running its own process, with:

- **Its own Postgres schema** (`client_1`, `client_2`, …) and **its own database role** granted `USAGE` on that schema alone. This is enforced by Postgres, not by application code.
- Its own feature extractor, reading only from its own schema.
- Its own model instance and training loop.
- Its own clipping and noise step.
- A gRPC connection to the Flower server carrying parameters only.

The **Flower server container receives no `DATABASE_URL`, no credentials, and no volume mount of user data.** A CI job asserts this by inspecting the container's environment and failing if any database variable is present (NFR-11). This turns the privacy claim into a testable property rather than a design intention — which is the whole point of D1.

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

**Why uniform weighting rather than sample-count weighting.** Standard FedAvg weights each client by its number of local examples. That would require every client to transmit its exact sample count — a real leak, since sample count is job-search volume, which is personal. Uniform weighting costs some statistical efficiency and buys the removal of an entire side channel. Trade-off recorded as ADR-009.

**Parameters and their justification:** S = 1.0 (bounds any single client's influence, which is simultaneously the DP sensitivity bound and the poisoning bound); σ = 1.0 (a conservative default from the DP-SGD literature, held fixed per §14/C7 rather than tuned to flatter the accuracy chart); k = 3 (the minimum at which averaging provides any anonymity at all; production would use 50+, and the report says so); τ = 3 (rejects updates more than 3× the clipping norm, which after clipping should be impossible for an honest client).

### 22.5 MVP FL vs Future Production FL

| Dimension | **MVP (this project)** | **Future production** |
|---|---|---|
| Clients | 5–8 containers: 1 bound to the demo user, 4–7 synthetic-cohort clients | Thousands of real user devices or per-tenant enclaves |
| Client location | Docker containers on one host, with genuinely separate schemas and roles | Browser (WASM) or user-controlled agent |
| Data isolation | Postgres schema + role separation, verified in CI | Physical device isolation |
| Selection | All eligible clients each round | Random sampling with availability and fairness constraints |
| Rounds | 10–30, manually or schedule triggered | Continuous, automated |
| Privacy | Local DP (clip + Gaussian noise), min cohort k=3 | Secure aggregation (SecAgg) + central DP + formal accounting with a privacy engine |
| Aggregation trust | Server sees individual noised updates | Server sees only the encrypted sum; individual updates are cryptographically hidden |
| Poisoning defence | Norm filtering | Krum / trimmed mean / Byzantine-robust aggregation + client reputation |
| Model | Logistic regression, 25 parameters | Larger models, personalisation layers, per-cohort specialisation |
| Accounting | Approximate per-round ε, displayed honestly as approximate | Rigorous RDP composition with a formal ε–δ guarantee |

**The MVP is a scale reduction, not a fidelity reduction.** Every architectural property that matters — data never moves, updates are clipped and noised, small cohorts are refused, the server has no data access — is genuinely present. What is reduced is the number of clients, the size of the model, and the cryptographic sophistication. That is exactly the right kind of compromise for a student project, and it is defensible under questioning in a way that a simulated-on-pooled-data version would not be.

### 22.6 How the prototype demonstrates the concept without distributed infrastructure

The obstacle is real: a student project has one real user and no cohort. Three mechanisms handle it honestly.

1. **Synthetic cohort clients.** A seeded generator produces 4–7 plausible job-search histories with *deliberately different* distributions — a DSA-heavy candidate, a behavioural-heavy candidate, a high-volume applicant, a low-volume one — which makes the data non-IID, matching DR-20 and giving a realistic (i.e. imperfect) convergence curve. Each history is loaded into its own client schema. Everything is clearly labelled synthetic in the UI, the report, and the poster.
2. **Real containerised separation.** The clients are real processes with real credential isolation, so the privacy property is demonstrated rather than asserted. The demo includes opening a shell in the FL server container and showing there are no database credentials in it.
3. **Flower simulation mode as a fallback.** If containerised deployment fights back near the deadline, `flwr.simulation` runs the same client code in-process. This is a documented degradation, not a substitute: the report would state plainly that the isolation property was demonstrated in the containerised configuration and the convergence results in simulation.

**The five-minute demo sequence:** show the Privacy page and a user's feature vector → start round *r* → watch clients train locally → show the server refusing a 2-client round → show a 5-client round aggregating → show the new model version live → open the Privacy Ledger and expand the exact 25 floats that were sent → toggle Federated Lift and watch the recommendations re-rank.

### 22.7 Threat model — stated honestly, including what this does *not* defend against

**Defended:**
- A compromised FL server cannot read user data; it has no credentials and no data path (D1, NFR-11).
- Network interception yields clipped, noised parameters, not records.
- A single malicious client's influence is bounded by the clipping norm and filtered by τ.
- Small-cohort deanonymisation is prevented by the k-refusal rule.
- Gradient inversion is materially impeded by clipping plus noise (though not eliminated — see below).

**Not defended, and documented as such:**
- **The server sees individual noised updates.** Without secure aggregation, a curious server could attempt inference on a single client's update; the noise makes this hard, not impossible. SecAgg is Future Scope.
- **Sybil clients.** Many colluding fake clients could still bias the model. Real deployments need identity and reputation systems.
- **Epsilon accounting is approximate.** Per-round ε is reported under stated assumptions, without a rigorous composition engine. The UI and the report both say "approximate".
- **A compromised application backend** would expose user data directly. FL protects the cross-user learning channel, not the application itself; that is what §23's controls are for.
- **Membership inference over many rounds** remains theoretically possible with a fixed cohort. Mitigated by the budget cap, not eliminated.

Stating the limits explicitly is what separates a credible privacy design from a marketing claim, and it will earn more credit than an overclaim would.

### 22.8 Evaluation plan

Report: global model AUC per round; per-client AUC (to show non-IID divergence, per DR-20); a convergence curve over 20+ rounds; an ablation of noise levels σ ∈ {0, 0.5, 1.0, 2.0} against utility, which is the privacy–utility trade-off from §14/C7 made concrete; a centralised-training upper bound for comparison; and refused-round counts. Every number is labelled as derived from synthetic cohorts.

---

## 23. Privacy & Security

### 23.1 Data classification

See §16.6 for the full crossing matrix. Summarised by sensitivity:

| Class | Contents | Handling |
|---|---|---|
| **Critical** | Password hashes, resume files, interview question text, notes, interviewer names | Never leaves the app boundary; never in any model feature; never logged; encrypted at rest in deployment |
| **Sensitive** | Applications, companies, statuses, outcomes, feedback | User-scoped only; never aggregated across users; excluded from FL |
| **Derived–shareable** | Bucketed, normalised numeric features (§22.2) | Only after clipping and noise; only with opt-in; user-inspectable in the ledger |
| **Operational** | Round metadata, participant counts, model versions | Visible as aggregate only; contains no user reference |

### 23.2 Security controls by layer

| Layer | Controls |
|---|---|
| **Identity** | Argon2id (m=64 MiB, t=3, p=4); ≥10-char policy with a blocklist; no enumeration on register or login; auth rate limiting |
| **Session** | Access token ≤15 min; refresh token ≤7 days, hashed at rest, rotated on use, revocable; logout revokes server-side; password change revokes all other sessions |
| **Authorisation** | Repository-layer `user_id` scoping; 404 on foreign resources; admin role cannot read user content, only aggregates |
| **Input** | Pydantic models rejecting unknown fields; length caps on every text field; enum validation; server-side authority over all client validation |
| **Files** | 5 MB cap; extension + MIME + magic-byte validation; UUID filenames; stored outside served paths; authenticated expiring download; no server-side rendering or parsing of uploads |
| **Database** | Parameterised queries only (a CI grep gate rejects f-string SQL); least-privilege app role; FL clients on separate schemas with separate roles |
| **Transport** | HTTPS in deployment; HSTS; CORS restricted to the known origin |
| **Headers** | CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy |
| **Logging** | Structured logs with an explicit redaction list (password, token, email, file path); no request bodies logged on auth routes |
| **Secrets** | Environment only; `.env` git-ignored; `.env.example` committed with placeholders; CI secret scanning |
| **FL** | Opt-in default off; clipping; noise; min cohort; norm filtering; no server credentials; ledger |

### 23.3 Security risk register

| Risk | Likelihood | Impact | Mitigation | Verified by |
|---|---|---|---|---|
| Unauthorised access to another user's data | Medium | Critical | Repository-layer scoping; 404 responses; three-layer enforcement (§17.4) | US-73 suite enumerated from OpenAPI |
| Broken object-level authorisation on a new endpoint | **Medium–high** (the most likely real breach) | Critical | Test suite generated from the schema, so a new endpoint without scoping fails CI | Automated |
| Weak user passwords | High | High | Policy + blocklist + strength meter; Argon2id limits offline cracking | Unit test |
| JWT theft or replay | Low | High | Short access TTL; refresh rotation with reuse detection; server-side revocation; HTTPS | Integration test |
| Malicious resume upload | Medium | High | Magic-byte validation; size cap; UUID names; stored outside web root; never parsed or rendered server-side | Test uploading a renamed executable |
| SQL injection | Low | Critical | ORM parameterisation; CI gate against raw SQL interpolation | CI gate + review |
| XSS | Low | High | React escaping by default; no `dangerouslySetInnerHTML`; CSP; server-side length and content validation | Review + CSP header test |
| CSRF | Low | Medium | Bearer tokens in headers rather than cookies for the API; SameSite on any cookie used | Review |
| API abuse / brute force | Medium | Medium | Rate limiting on auth and generation endpoints; pagination caps | Integration test |
| Account enumeration | Medium | Medium | Identical responses and comparable timing for existing and non-existing emails | Test |
| Data leakage through analytics | Low | High | All analytics user-scoped; no cross-user endpoint exists to leak through | Review + test |
| Gradient inversion of FL updates | Low–medium | High | Clipping + Gaussian noise; small model; bucketed features; disclosed as impeded-not-eliminated | Ablation experiment |
| Deanonymisation via small cohort | Medium | High | k-refusal rule (k=3) enforced server-side | Unit test: a 2-client round is refused |
| Model poisoning | Low (closed cohort) | Medium | Norm filtering at τ=3; clipping bounds any single client; disclosed as insufficient against Sybils | Unit test with a crafted large update |
| Sample-count leakage via FedAvg weighting | Medium | Medium | Uniform weighting instead of count weighting (ADR-009) | Review + payload test |
| Secret committed to the repository | Medium | High | `.gitignore`, `.env.example` only, CI secret scan, pre-commit hook | CI |
| Dependency vulnerability | Medium | Medium | Dependabot; `pip-audit` and `npm audit` in CI; pinned versions | CI |
| Orphaned data after account deletion | Medium | High | Cascade constraints + a post-deletion orphan-check test across every table | Integration test |

### 23.4 Explicitly out of scope for the MVP

Multi-factor authentication; email verification and password reset by email (no mail infrastructure); secure aggregation; formal DP accounting; penetration testing by a third party; SOC2/GDPR certification (though the design follows GDPR principles: minimisation, purpose limitation, right to export, right to erasure). Each is listed in Future Scope rather than quietly omitted.

---

## 24. Testing Strategy

### 24.1 Shape of the suite

A pragmatic pyramid: many fast unit tests over pure logic (mastery, scoring, state machine, metrics, FL arithmetic), a solid layer of API tests over the real contract, a thin layer of integration tests over critical journeys, and no browser-automation layer at all — E2E tooling is a time sink that a student team should trade for better API and component coverage. Manual scripted walkthroughs cover what E2E would.

### 24.2 By category

**Unit (backend).** Password hashing and verification; token creation, expiry, rotation, reuse detection; status state machine (parametrised over every transition pair, valid and invalid); dwell-time and days-in-stage helpers; funnel stage attainment from synthetic history; every conversion rate against hand-computed fixtures; mastery scoring including decay and the insufficient-evidence gate; prerequisite routing including the depth cap and the margin; recommendation scoring and diversity filtering; template rendering and the prohibited-phrasing linter; feature extraction including point-in-time correctness (a test that fails if any post-`as_of` record influences the vector); clipping arithmetic; noise application; FedAvg averaging against a hand-computed example; the min-cohort refusal; the norm filter.

**API.** Every endpoint × {happy path, validation failure, unauthenticated, cross-user}. Pagination boundaries. Filter combinations. Upload rejection cases. Invalid status transitions returning 409. Response schemas validated against the OpenAPI spec so drift is caught automatically.

**Frontend (RTL + Vitest).** Form validation and submission for login, register, application, interview, and debrief; `DataTable` rendering, sorting, pagination, and empty state; `StatusBadge` rendering every status with text as well as colour; `ProtectedRoute` redirect; `ConfirmDialog` requiring explicit confirmation; error and loading states rendering distinctly; the recommendation card rendering its tier badge and evidence drawer; the ledger vector inspector rendering values.

**Integration.** Register → login → create application → change status → verify history. Create application → attach resume → add interview → add questions → complete debrief → verify derived topics. Practice attempts → mastery recomputation → recommendation generation → accept → task created. A full FL round: seed clients → extract → train → clip → noise → aggregate → version → ledger entries written. Account deletion → orphan check across every table. All run against a real Postgres via testcontainers, never SQLite — the constraints being tested only exist in Postgres.

**Security.** The US-73 suite, enumerated from the OpenAPI schema so coverage cannot silently lapse: unauthenticated access to every non-public endpoint returns 401; cross-user access to every owned resource returns 404; the client cannot set `user_id` on create; the malicious-upload test; the SQL-injection-shaped input test; the auth rate-limit test; the no-enumeration test; the FL server credential-absence check.

**FL-specific.** Payload schema conformance (exactly 24 named floats, no strings — the single most important test in the project); opted-out clients produce no update; a client with fewer than 5 examples abstains; clipped norms respect S; noised vectors differ from raw; a 2-client round is refused and logged; a crafted over-norm update is discarded; a failed round leaves the previous model active and servable; deterministic results under a fixed seed.

### 24.3 What is tested in each sprint

| Sprint | Focus | Gate |
|---|---|---|
| 1 | Auth unit + API tests; application CRUD; **the cross-user isolation pattern established here**; RTL basics; register→create→status integration | ≥70% backend coverage; CI green |
| 2 | Upload validation (write the malicious-upload test first); interview and question CRUD; debrief flow; resume-application association | Malicious upload rejected; cross-user file access 404 |
| 3 | Mastery golden-file tests; every analytics formula against hand-computed fixtures; question bank and practice; chart component tests | Analytics golden files pinned; ≥75% coverage |
| 4 | Recommendation logic; phrasing linter; feature extraction and point-in-time correctness; the full FL round; refusal and filtering | Payload schema test passing; refusal test passing; ≥80% on services |
| 5 | Full security suite; accessibility scan; deletion orphan check; export completeness; performance under seeded load | All gates; zero critical axe violations |

### 24.4 Standing rules

Every bug fix ships with a regression test that fails before the fix. Every PR touching a service adds or updates tests. Coverage never decreases. The suite runs offline and deterministically (NFR-50) with all randomness seeded. The full backend suite stays under 3 minutes (NFR-52) — a slow suite is a suite people stop running.

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
        └── chore/upgrade-alembic
```

Branch names carry the story ID, which makes the board, the branches, the commits, and the PRs mutually traceable — precisely what an evaluator checking individual contribution needs.

**Protection on `main`:** no direct pushes; PR required; at least one approving review from someone other than the author; all CI checks passing; branch up to date before merge.

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

**Commit hygiene rules that matter for grading:** commit at a working-increment granularity (several per day, not one per week); never commit under another member's account; never let one person merge everyone's work as their own; write messages that describe intent rather than mechanics. A history of 400 commits from one account and 6 from another is the single most common way student teams lose marks on this criterion, and it is entirely avoidable.

### 25.4 Pull requests

Template requires: linked story ID, what changed and why, how it was tested, screenshots for UI changes, a self-checklist against the DoD, and any follow-up work noted. Small PRs (target under 400 changed lines). Review within 24 hours — a PR that sits for three days blocks a teammate and destroys sprint flow. Reviewers check correctness, scoping (`user_id` present on every query), tests, and DoD compliance. **Authors never approve their own PR**, no matter how small the change or how close the deadline.

### 25.5 Issues and board

Every user story becomes a GitHub Issue with the story ID in the title, acceptance criteria as a task list, and labels for epic, priority, points, and sprint. Tasks within a story become sub-issues or checklist items. GitHub Projects provides the board: `Backlog → Sprint Backlog → In Progress → In Review → Testing → Done`. WIP limit of 2 per person in "In Progress" — the discipline that prevents five half-finished features at sprint end. Sprint milestones group issues and give a burndown for free.

### 25.6 Ceremonies

| Ceremony | Cadence | Duration | Output |
|---|---|---|---|
| Sprint planning | Start of sprint | 90 min | Sprint backlog, assignments, sprint goal |
| Daily standup | Daily (async in a Slack/Discord thread is fine and often better for students) | 10 min | Blockers surfaced within 24 h |
| Backlog refinement | Mid-sprint | 60 min | Next sprint's stories estimated, ACs expanded, assumptions from §5.3 validated |
| Sprint review | End of sprint | 60 min | Working demo, stakeholder feedback, updated backlog |
| Retrospective | End of sprint | 45 min | 3 keeps, 3 changes, 1 action item with an owner |

Every ceremony produces a dated markdown file in `docs/agile/sprint-N/`. These files are the evidence that the process happened, and they cost ten minutes each to write.

### 25.7 Dividing work without assuming team size

Assign by **vertical slice**, not by layer. "You do frontend, I do backend" is the most common student split and the worst one: it creates constant blocking, makes individual contribution hard to assess, and leaves half the team unable to discuss half the system in the viva. Instead, each person owns a feature end to end — schema, migration, service, endpoint, page, tests.

**Role rotation across sprints** (a member takes a different hat each sprint):

| Hat | Responsibility |
|---|---|
| **Integrator** | CI health, merge conflicts, release tagging, keeping `develop` green |
| **Quality lead** | Coverage gates, review depth, test debt |
| **Documentation lead** | Ceremony notes, ADRs, README, requirement traceability |
| **Domain owner** | The sprint's headline feature area |

**Suggested allocations:**

*Two people:* both full-stack; one leans data/ML, the other UI/UX; both review everything; cut scope per §10.2.
*Three:* A = auth + applications + platform; B = resumes + interviews + UI system; C = prep + analytics + ML/FL; everyone reviews outside their area.
*Four:* as above with D = analytics + recommendations, freeing C to focus on FL from Sprint 3.
*Five:* as four, plus E = QA, security suite, accessibility, documentation, deployment, and the poster.

Regardless of size: **everyone writes tests for their own code, everyone reviews someone else's, everyone is able to explain the FL design in the viva.** Concentrated FL knowledge in one head is a project risk (§28, R6), not a division of labour.

---

## 26. Project Folder Structure

Proposed only — nothing is created until approval.

```
smart-job-tracker/
├── README.md
├── LICENSE
├── CONTRIBUTING.md
├── .gitignore
├── .env.example
├── docker-compose.yml               # dev: db + api + web
├── docker-compose.fl.yml            # FL profile: fl-server + fl-client-1..N
├── docker-compose.prod.yml          # production profile
│
├── .github/
│   ├── workflows/
│   │   ├── ci.yml                   # lint · types · tests · coverage gate
│   │   ├── security.yml             # secret scan · pip-audit · npm audit
│   │   └── fl-isolation-check.yml   # asserts fl-server has no DB credentials
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── ISSUE_TEMPLATE/user_story.md
│
├── backend/
│   ├── pyproject.toml
│   ├── Dockerfile
│   ├── alembic.ini
│   ├── alembic/versions/
│   └── app/
│       ├── main.py                  # FastAPI app factory
│       ├── config.py                # pydantic-settings, env only
│       ├── database.py              # engine, session, base
│       ├── dependencies.py          # get_current_user, get_db, pagination
│       ├── api/v1/
│       │   ├── auth.py  users.py  applications.py  companies.py
│       │   ├── resumes.py  interviews.py  questions.py  preparation.py
│       │   ├── analytics.py  recommendations.py  fl.py  health.py
│       ├── models/                  # SQLAlchemy ORM, one module per aggregate
│       ├── schemas/                 # Pydantic request/response models
│       ├── repositories/
│       │   ├── base.py              # UserScopedRepository — user_id required
│       │   └── …
│       ├── services/
│       │   ├── auth_service.py  application_service.py  resume_service.py
│       │   ├── interview_service.py  preparation_service.py
│       │   ├── mastery_engine.py  analytics_service.py
│       │   ├── recommendation/
│       │   │   ├── generators.py  scoring.py  templates.py  tiering.py
│       │   └── fl_coordinator.py
│       ├── core/                    # security, hashing, jwt, rate limit, errors
│       ├── storage/                 # file storage adapter (local now, S3 later)
│       └── seeds/                   # topics, taxonomy edges, question bank
│
├── fl/                              # deliberately OUTSIDE backend/ —
│   ├── Dockerfile.server            # different trust zone, different image,
│   ├── Dockerfile.client            # no shared credentials
│   ├── server/
│   │   ├── server.py                # Flower server entrypoint
│   │   ├── strategy.py              # FedAvg + min-cohort + norm filter
│   │   └── model_registry.py
│   ├── client/
│   │   ├── client.py                # Flower NumPyClient
│   │   ├── feature_extractor.py     # the 24-feature schema, point-in-time
│   │   ├── local_trainer.py
│   │   └── privacy.py               # clipping + Gaussian noise
│   ├── common/
│   │   ├── schema.py                # FEATURE_SCHEMA_V1 — single source of truth
│   │   └── serialization.py
│   └── synthetic/
│       └── cohort_generator.py      # seeded non-IID synthetic clients
│
├── frontend/
│   ├── package.json  vite.config.ts  tailwind.config.ts  Dockerfile
│   └── src/
│       ├── main.tsx  App.tsx  router.tsx
│       ├── features/                # feature-first, not type-first
│       │   ├── auth/  applications/  resumes/  interviews/
│       │   ├── preparation/  questions/  analytics/
│       │   ├── recommendations/  privacy/  profile/
│       │   │   └── (each: components/ · hooks/ · api.ts · types.ts · pages/)
│       ├── components/
│       │   ├── ui/                  # shadcn primitives
│       │   └── shared/              # DataTable · StatusBadge · EmptyState
│       │                            # LoadingSkeleton · ErrorState · Timeline
│       ├── lib/                     # api client · query client · utils
│       ├── hooks/  contexts/  types/api.ts (generated from OpenAPI)
│       └── styles/
│
├── tests/
│   ├── backend/{unit,api,integration,security}/
│   ├── fl/
│   └── fixtures/                    # golden files for analytics & mastery
│
└── docs/
    ├── requirements.md  architecture.md  database.md  api.md
    ├── fl-design.md  privacy.md  testing.md  deployment.md  user-guide.md
    ├── adr/                         # ADR-001 … one file per decision
    ├── agile/
    │   ├── product-backlog.md
    │   └── sprint-1..5/{plan,standups,review,retrospective}.md
    └── poster/                      # source + exported PDF
```

Two structural decisions worth noting. **`fl/` sits outside `backend/`** because it is a different trust zone; nesting it would invite shared imports and shared configuration, which is exactly how the credential separation would erode. **`fl/common/schema.py` is the single source of truth for the feature schema**, imported by the client, the serialisation test, and the `/fl/schema` endpoint — so the opt-in screen cannot drift out of sync with what is actually sent.

---

## 27. Deployment Plan

### 27.1 Principle

Local first, containerised from day one, cloud only if it adds marks. The failure mode to avoid is a team spending Sprint 5 fighting a hosting provider instead of hardening the product.

### 27.2 Local development (Sprint 1 onward)

```
docker compose up
  ├── db      postgres:16-alpine · volume · healthcheck · port 5432
  ├── api     backend/Dockerfile · uvicorn --reload · mounts ./backend
  └── web     frontend/Dockerfile · vite dev · mounts ./frontend
```

`docker compose up` is the only command a new team member needs. Migrations run automatically on API start in dev via an entrypoint script that waits on the db healthcheck. `make seed` loads demo data. Verified independently on every member's OS in Sprint 1 (NFR-41), because "works on my machine" discovered in Sprint 4 is a lost week.

### 27.3 FL profile (Sprint 4 onward)

```
docker compose -f docker-compose.yml -f docker-compose.fl.yml up
  ├── fl-server     NO database environment variables at all
  ├── fl-client-1   DATABASE_URL scoped to schema client_1, role client_1
  ├── fl-client-2 … each with its own schema and role
```

Kept in a separate compose file so the FL stack starts only when needed, and so the credential asymmetry between server and clients is visible in one readable file — which makes it a demo artefact rather than a configuration detail.

### 27.4 Production profile

Multi-stage builds: the frontend compiles to static assets served by nginx; the backend runs uvicorn behind gunicorn workers with `--reload` off; Postgres uses a named volume with a nightly `pg_dump` to a mounted backup directory. All secrets from environment. HTTPS terminated at nginx or the platform edge.

### 27.5 Hosting options

| Option | Effort | Cost | When to choose |
|---|---|---|---|
| **Local only, demo on a laptop** | None | Free | Default. Fully acceptable for a university MVP and the lowest-risk choice for a live demo. |
| **Single VPS** (Hetzner, DigitalOcean, campus VM) | Low | ~$5/mo | If the evaluator wants a URL. One host, docker compose, Caddy or nginx for TLS. This is the recommended upgrade. |
| **Split PaaS** (Railway/Render API + Neon/Supabase Postgres + Vercel/Netlify frontend) | Medium | Free tiers | If a permanent public link matters more than the FL demo — note that free tiers make running FL client containers awkward. |
| **Kubernetes** | High | Real money | **No.** Nothing here justifies it. |

**Recommendation:** local Compose as the primary demo path, with an optional VPS deployment in Sprint 5 if capacity allows. The FL demonstration should always be run locally, because the credential-isolation walkthrough is the strongest part of it.

### 27.6 Pre-demo checklist

Fresh clone → `docker compose up` → migrations apply → seed loads → register a new user → complete the scripted path → run the FL round → restore from a backup once. Rehearsed twice, end to end, by two different people, at least three days before the presentation.

---

## 28. Risks & Mitigation

### 28.1 Project risks

| ID | Risk | Prob. | Impact | Mitigation | Early warning sign |
|---|---|---|---|---|---|
| R1 | **Scope creep** — the backlog grows faster than velocity | High | High | MoSCoW with a pre-agreed cut list (§10.2); no story enters a sprint mid-sprint; new ideas go to the backlog, not the branch | Sprint 2 finishes under 80% of committed points |
| R2 | **FL complexity overruns Sprint 4** | High | High | The 1-day Flower spike moved into Sprint 3 (§13.11); simulation-mode fallback; success defined as correctness not accuracy, agreed with the evaluator in advance | The spike takes more than one day |
| R3 | **Insufficient data to train anything** | **Certain** | Medium | Planned for, not discovered: synthetic non-IID cohort generator built in Sprint 3; results reported as mechanism validation | — (accepted and designed around) |
| R4 | **Analytics silently wrong** | Medium | High | Formulas defined in §20.7 before implementation; golden-file tests written before the code; hand-computed fixtures | A metric changes after an unrelated refactor |
| R5 | **Time pressure from other coursework** | High | High | 20% of Sprint 5 reserved for defects; the cut list; async standups; small PRs so work is never stuck in one head | Two consecutive missed standups |
| R6 | **Knowledge silos, especially FL** | Medium | High | Role rotation; cross-area review; a mandatory FL walkthrough session for the whole team in Sprint 4 | Only one person can answer a question about a subsystem |
| R7 | **Integration problems late** | Medium | High | Vertical slice in Sprint 1 exercises every layer; `develop` kept green; generated API types break the build on drift | A PR sits unmergeable for over 48 h |
| R8 | **Testing deferred to the end** | Medium | High | Coverage gate live in CI from Sprint 1; DoD blocks merge without tests | Coverage trends down two sprints running |
| R9 | **Uneven contribution damages grades** | Medium | High | Vertical-slice ownership; per-member PR requirement in Sprint 1 DoD; contribution reviewed at every retrospective while there is still time to correct | One member's PR count is an outlier by Sprint 2 |
| R10 | **Environment differences across the team** | Medium | Medium | Compose parity verified by every member in Sprint 1; no host-installed dependencies | Anyone says "it only works if I run it outside Docker" |
| R11 | **Team member unavailable (illness, withdrawal)** | Low–medium | High | No single-owner subsystem; documentation in-repo; work sliced so it can be handed over | — |
| R12 | **Database schema churn late in the project** | Medium | Medium | Full schema designed up front (§17); Alembic from day one; only additive migrations after Sprint 3 | A Sprint 4 story requires altering a Sprint 1 table |
| R13 | **Demo fails live** | Medium | High | Rehearsed twice from a fresh clone; seeded deterministic data; a recorded backup video | Any rehearsal needs a code change to complete |

### 28.2 Security risks

Covered in full in §23.3 with likelihood, impact, mitigation, and verification method for each.

### 28.3 The three risks that actually decide this project

If attention is limited, spend it here: **R2** (FL overrun — mitigated by moving the spike earlier, which costs one day and buys a sprint), **R1** (scope — mitigated by having the cut list agreed before it is needed, not during a panic), and **R9** (contribution — mitigated by noticing in Sprint 2, when it is fixable, rather than in Sprint 5, when it is not).

---

## 29. Concept Poster

### 29.1 Purpose and constraints

A1 portrait, readable from two metres, viewed for roughly ninety seconds. That constraint governs everything: roughly 60% visual, 40% text, no paragraph longer than three lines, no code, no ER diagram, no API table. Delivered as PDF plus editable source in `docs/poster/`.

### 29.2 Layout

```
┌──────────────────────────────────────────────────────────────┐
│  COHORT — Smart Job Application Tracker with                 │
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
│  [Simplified §16.2 diagram — 6 boxes maximum,                │
│   with the PRIVACY BOUNDARY drawn as a heavy line]           │
├──────────────────────────────────────────────────────────────┤
│  FEDERATED LEARNING — THE CORE IDEA                          │
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
│   ⚠ RAW USER DATA NEVER LEAVES THE USER'S CLIENT             │
├──────────────────────┬───────────────────────────────────────┤
│  KEY FEATURES        │  PRIVACY BY DESIGN                    │
│  ▤ Application       │  ✓ Opt-in, default off                │
│    tracking with     │  ✓ Only 24 numeric features leave     │
│    full history      │  ✓ Clipped + noised (local DP)        │
│  ◉ Structured        │  ✓ Never fewer than 3 clients         │
│    interview debrief │  ✓ Server holds no data credentials   │
│  ✎ Topic mastery     │  ✓ Privacy Ledger: every user sees    │
│    graph with decay  │    every value they ever sent         │
│  ▲ Funnel analytics  │                                       │
│    with honest       │  [Screenshot: Privacy Ledger with a   │
│    sample sizes      │   vector expanded]                    │
│  ✦ Explainable       │                                       │
│    recommendations   │                                       │
├──────────────────────┴───────────────────────────────────────┤
│  TECH STACK                                                  │
│  React · TypeScript · Tailwind · shadcn/ui · Recharts        │
│  FastAPI · Python · PostgreSQL · SQLAlchemy · JWT/Argon2id   │
│  scikit-learn · Flower · Pytest · Docker · GitHub Actions    │
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

One accent colour, used only for the privacy boundary and the FL arrows, so the eye goes to the innovation. Two screenshots maximum — the dashboard action queue and the Privacy Ledger. Every claim on the poster must be one the team can defend if asked; nothing aspirational stated in the present tense. The FL panel occupies the visual centre because it is what distinguishes the project, and the sentence *"raw user data never leaves the user's client"* should be the single most legible line on the sheet.

---

## 30. Definition of Done

### 30.1 Story-level DoD (every story, no exceptions)

- Acceptance criteria met and demonstrated.
- Code follows project conventions; lint, format, and type checks pass.
- Unit tests written for new logic; API tests for new endpoints; component tests for new UI.
- All tests pass locally and in CI; coverage has not decreased.
- **Cross-user isolation verified** for any endpoint touching user-owned data.
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
- **Every team member has authored and merged at least one reviewed PR.**

### 30.3 Sprint-specific additions

**Sprint 1:** Compose verified on every member's machine; migrations apply and roll back cleanly; the cross-user isolation suite passes for every endpoint; ≥70% backend coverage; concept poster complete and reviewed.
**Sprint 2:** A renamed executable is rejected by upload validation; cross-user file access returns 404; the debrief flow is timed under 3 minutes with 3 users; seed data loads deterministically.
**Sprint 3:** Every analytics metric has a documented formula in §20.7 and a passing golden-file test; the topic graph passes cycle detection; the Flower spike is complete and written up.
**Sprint 4:** The FL payload schema test passes (24 named floats, no strings); a 2-client round is refused and logged; the FL server container is verified to hold no database credentials; the phrasing linter passes; a Privacy Ledger entry displays a real transmitted vector.
**Sprint 5:** Zero critical axe violations; account deletion leaves no orphan rows; export completeness verified; backup and restore rehearsed; the demo runs end to end twice without intervention.

### 30.4 Project-level DoD

All MVP stories done; all NFR measures verified or explicitly waived with a reason; full documentation set complete; poster delivered; deployment working; demo rehearsed; every team member able to explain the architecture, the analytics formulas, and the FL privacy design.

---

## 31. Final MVP Scope

### 31.1 In the MVP

**Authentication:** registration, login, logout, refresh rotation, profile management, Argon2id, per-user isolation.
**Applications:** full CRUD, search, filter, sort, status changes with immutable history, timeline, days-in-stage, stall detection, company records, tags.
**Resumes:** versioned records, structured content, validated file upload and secure download, application association, cloning, sample-size-gated outcome comparison.
**Interviews:** creation against applications, multiple rounds, question capture with topic and difficulty, structured debrief under three minutes, derived topics.
**Preparation:** seeded question bank (≥150), personal questions, practice attempts with confidence history, topic taxonomy with prerequisites, mastery scoring with time decay, preparation tasks, progress views.
**Analytics:** funnel from history-based stage attainment, five conversion rates with denominators, activity over time, rejection distribution separated from no-response, dwell times, topic performance, sample-size honesty throughout.
**Recommendations:** rule-based generation, prerequisite root-cause routing, evidence tiers A–D with observation counts, plain-language explanations, enforced non-causal phrasing, accept/snooze/dismiss, stall follow-ups.
**Federated learning:** opt-in default off, local 24-feature extraction with point-in-time correctness, local scikit-learn training, L2 clipping, Gaussian noise, Flower FedAvg with a minimum cohort of 3 and norm filtering, versioned global models, Privacy Ledger with inspectable vectors, withdrawal, Federated Lift viewer, credential-free FL server verified in CI.
**Platform:** Docker Compose, Alembic migrations, CI with lint/type/test/coverage gates, security test suite, accessibility pass, seed data, full documentation, concept poster.

### 31.2 Deliberately not in the MVP

Email verification and password reset; MFA; OAuth; admin dashboards beyond account disable; job-board scraping or integrations; LLM features of any kind; calendar or email sync; mobile applications; collaboration or sharing of any kind; secure aggregation; formal DP accounting; real-user federation at scale; browser-based FL clients.

### 31.3 The one-sentence version

*A privacy-first job-search tracker that turns each interview into targeted, evidence-tiered preparation, and learns which preparation patterns work across users through a real federated learning deployment in which raw data never leaves the user's own client — and every user can inspect exactly what did.*

---

## 32. Future Scope

| Area | Enhancement | Value | Effort |
|---|---|---|---|
| **FL** | Secure aggregation (SecAgg) so the server sees only the encrypted sum | Removes the largest acknowledged gap in the threat model (§22.7) | High |
| **FL** | Browser-native WASM clients | True on-device training; no server-side client containers | High |
| **FL** | Formal RDP composition and a real ε–δ guarantee | Turns "approximate" epsilon into a defensible number | Medium |
| **FL** | Byzantine-robust aggregation (Krum, trimmed mean) + client reputation | Sybil and poisoning resistance at scale | Medium |
| **FL** | Personalisation layers over the global model | Better fit under non-IID data (DR-20) | Medium |
| **ML** | Survival analysis for stage transitions | Proper handling of right-censored applications; better stall prediction | Medium |
| **ML** | Sequence models over interview history | Captures trajectory rather than a snapshot | High |
| **Product** | Calendar integration for interview scheduling | Removes duplicate entry | Medium |
| **Product** | Email parsing to auto-detect application updates | Largest possible reduction in data-entry burden — and the largest new privacy surface, which is precisely why it is not in the MVP | High |
| **Product** | Browser extension to capture a listing in one click | Removes the biggest friction point in adoption | Medium |
| **Product** | Mock interview mode with timed practice sessions | Deepens the preparation loop | Medium |
| **Product** | Mobile application | Debrief immediately after leaving the room, when recall is best | High |
| **Product** | Spaced-repetition scheduling built on the decay model | The mastery formula already supports it | Low |
| **Analytics** | Cohort benchmarking for institutions, opt-in and differentially private | Brings career-services staff in as a genuine stakeholder (deferred from §4) | High |
| **Platform** | S3-compatible object storage | Already abstracted behind the storage interface — a swap, not a rewrite | Low |
| **Platform** | Observability: metrics, tracing, alerting | Operational maturity | Medium |
| **Security** | MFA, email verification, third-party penetration test | Closes the §23.4 gaps | Medium |
| **Accessibility** | Screen-reader-optimised chart alternatives beyond data tables | Beyond AA | Medium |

---

## 33. Recommended Changes to the Brief, and Why

You asked what I would do differently. Seven deviations from the instructions as written, each with a reason. Reject any of them and I will follow the original.

**1. `POST /fl/update` removed from the API.**
Routing model updates through the application backend would put user-linked updates in the same process that holds database credentials, which weakens the exact boundary the FL design exists to create. Updates travel over Flower's gRPC channel instead. The app API keeps only the control-plane endpoints (`/fl/status`, `/fl/opt-in`, `/fl/ledger`, `/fl/model`, `/fl/rounds`). This is the single most important change.

**2. `GET /analytics` split into nine resource-scoped endpoints.**
One endpoint returning everything is slow, uncacheable, hard to test, and forces over-fetching for a single chart. Splitting also lets each metric carry its own `sample_size` and `sufficient_data` in the response schema, which is how FR-67 gets enforced by the contract rather than by UI discipline.

**3. A ninth EPIC for platform, quality, and DevOps.**
Infrastructure work that is not in the backlog is invisible, unestimated, and first to be skipped. Making it first-class also gives every member reviewable Sprint 1 work.

**4. Argon2id specified over bcrypt.**
The brief allows either. Argon2id is memory-hard and the current OWASP recommendation; bcrypt has no advantage here.

**5. Five sprints rather than four plus an undefined "final phase".**
The final phase in the brief has no goal, no stories, and no DoD, which is how hardening gets squeezed to nothing. Sprint 5 is a real sprint with real stories and 20% capacity reserved for defects from the start.

**6. The Flower spike moved into Sprint 3.**
One day in Sprint 3 to surface Flower's API friction before Sprint 4 depends on it. This is the highest-leverage scheduling decision in the plan and costs almost nothing.

**7. Companies scoped per user rather than shared globally.**
A global company table creates a subtle cross-user inference channel and needs deduplication logic nobody has time to write. Per-user scoping costs some duplication and buys clean isolation.

### 33.1 If you want an even leaner plan

If the team is two or three people, or the timeline is under ten weeks, here is the version I would actually recommend:

- **Drop EPIC 3 to the minimum:** resume records, upload, download, and application association. Cut structured sections (US-23), cloning (US-25), and outcome comparison (US-26). Saves 13 points and removes the weakest statistical claim in the product.
- **Cut analytics to four metrics:** funnel, conversion rates, activity over time, and stall detection. Drop rejection distribution, dwell-time IQR, topic performance, and preparation trends. Saves ~14 points.
- **Keep every FL story.** It is the differentiator and the graded innovation; cutting it to save time inverts the priority.
- **Keep the Privacy Ledger and the mastery decay.** They are cheap and they carry the project's identity.
- **Merge Sprints 3 and 4 into one six-week block** with FL work starting in week one, so FL is never the thing that runs out of runway.

That version is roughly 230 points, fits three people comfortably, and loses nothing that distinguishes the project.

---

## 34. Status: Awaiting Approval

Nothing has been created, installed, configured, or modified. The repository is untouched and empty.

**What I need from you before implementation starts:**

1. **Team size** — this determines the sprint rebalance (§10.2). It is the one input I cannot infer.
2. **Timeline** — number of weeks and sprint length.
3. **Confirm or correct assumptions A1–A7** (§1.4).
4. **Accept, modify, or reject the seven deviations** in §33.
5. **Accept or reject the lean variant** in §33.1.
6. **Confirm the project name** — *Cohort*, or your own.
7. **Explicit approval to begin Sprint 1.**

On approval, Sprint 1 begins with US-67 (repository bootstrap) and US-68 (Compose environment), because every other story depends on them.
