# Cohort: Requirements

## 1. Introduction

### 1.1 Purpose

This document lists the requirements for Cohort: the problem it addresses, its stakeholders, how the requirements were gathered, the functional, non-functional and domain requirements, the epics and user stories with their acceptance criteria, the product backlog, the trade-offs between requirements, and the product scope. The architecture is described in `docs/architecture.md` and the sprint and phase breakdown in `docs/sprints.md`.

### 1.2 Product summary

Cohort is a web application for students and early-career job seekers. It tracks every job application from saved to offer, captures what happened in each interview, turns that into a targeted preparation plan, and measures whether the preparation is working. Across users, a federated learning deployment learns which preparation patterns go with better outcomes, without any user's raw data leaving that user's own data partition.

The product is built around six ideas:

1. Federated learning on separated data. Each FL client is a separate process with its own data store and credentials scoped to that store. The Flower server has no database credentials. Only a fixed-length vector of clipped, noised model parameters crosses the boundary.
2. Privacy Ledger. Each user has a page listing every federated round their client took part in: round ID, time, feature schema, update norm before and after clipping, noise multiplier, cohort size, the exact vector sent, and a list of what was not sent.
3. Evidence-tiered recommendations. Every recommendation carries an evidence tier, and the wording depends on the tier:

   | Tier | Source | Example phrasing |
   |---|---|---|
   | A, your data | 5 or more of the user's own observations | "In 6 of your last 8 technical rounds, graph questions were marked low-confidence." |
   | B, your data, thin | 2 to 4 observations | "Early signal only (2 rounds): dynamic programming may be a gap." |
   | C, federated | Global model, cohort of at least k | "Across the federated cohort, users who logged 10 or more practice reps on a weak topic before a repeat round reported higher confidence. This is an association, not a guarantee." |
   | D, heuristic | Rules, no data yet | "You have no interviews logged. Start with a broad baseline." |

   Causal phrasing is not allowed. A test (`test_no_causal_language`) fails the build if a recommendation template contains a banned phrase such as "will get you", "guarantees" or "because of this you were rejected".
4. Topic mastery with time decay. Preparation topics form a directed prerequisite graph (Arrays → Two Pointers → Sliding Window; Recursion → Trees → Graphs → Shortest Path). Each user has a mastery score per topic in [0,1] that rises with practice and good interview answers, decays with time since the topic was last touched, and routes weakness back to prerequisites.
5. Pipeline health with stall detection. From the user's own history, the app computes how long applications usually stay in each stage and flags those that have been stuck longer than usual, with a sample-size note when history is thin.
6. Federated Lift viewer. The recommendations page has a "Local only" / "Local + Federated" toggle that re-ranks the list and shows which items moved and by how much.

The name works on three levels: the peer cohort of job seekers, the cohort of FL clients, and the minimum cohort size that the privacy design enforces.

### 1.3 Definitions

| Term | Meaning |
|---|---|
| Application | One job application by one user to one role at one company |
| Stage / status | Where an application stands: Saved, Applied, OA, Interview, Final Round, Offer, Accepted, Rejected, Withdrawn, Declined |
| Dwell time | Days an application has spent in its current stage |
| Stalled | An application whose dwell time is longer than the user's own median for that stage (or a default when history is thin) |
| Debrief | The structured record a user makes after an interview: outcome, self-rating, what went well and badly, questions asked |
| Mastery | A per-topic score in [0,1] built from practice and interview answers, with exponential time decay |
| Evidence tier | The A to D label on a recommendation that says what data it rests on |
| Sample size (n) | The number of observations behind a rate, average or median; shown next to every figure |
| Federated learning (FL) | Training a shared model from model updates computed on each client, without moving the training data |
| FedAvg | Federated averaging: the server averages client updates into a new global model |
| Clipping | Scaling an update down so its L2 norm is at most a bound S |
| Differential privacy (DP) noise | Gaussian noise added to each clipped update before it leaves the client |
| Epsilon (ε) | The privacy cost recorded for a round |
| Minimum cohort (k) | The smallest number of clients a round needs before the server aggregates; k = 3 |
| Privacy Ledger | The per-user record of everything the user's FL client has sent |
| Federated Lift | The difference in recommendation ranking with and without the global model |
| Isolation | The rule that a user can only ever reach their own records; another user's record returns 404 |

---

## 2. Problem and goals

### 2.1 The problem

A student applying for internships or graduate roles typically runs 40 to 150 applications in a season. The state of that search lives in a spreadsheet, a notes app, a few email threads and memory. Three failures follow:

1. Pipeline amnesia. Applications stall silently. Nobody follows up because nobody notices.
2. Post-interview loss. Memory of what was asked fades within about 48 hours. It is rarely written down in a structured way, so it cannot shape the next round of preparation.
3. Blind preparation. Students work through a generic list instead of the topics that actually cost them interviews.

Typical tracking tools handle the first problem with a board and ignore the other two.

### 2.2 Product goals

| ID | Goal | Success measure |
|---|---|---|
| G1 | One place for the whole job search | A user can answer "where does every application stand?" from one screen |
| G2 | Capture interview experience while it is fresh | A debrief takes under 3 minutes |
| G3 | Turn experience into targeted preparation | Every logged interview leads to at least one concrete, evidence-backed prep item |
| G4 | Make search performance readable | Funnel, conversion rates and stall detection with defined formulas |
| G5 | Learn across users without collecting their data | No raw records at the FL server, checkable by inspection |
| G6 | Make privacy inspectable | Everything that leaves a client is visible to the user in the ledger |

### 2.3 Course goals

Cohort is a course project. It follows Agile and Scrum with sprint artifacts, has a full requirements set (FR, NFR, DR, elicitation and trade-off analysis), shows each member's contribution through the GitHub history, and is a working, tested system that uses federated learning.

### 2.4 Non-goals

- Not a job board or scraper. Cohort does not pull listings from third-party sites.
- Not an AI wrapper. AI helps with the user's own workflow, answers only from the user's own data, and never acts on the user's behalf. Drafts are never sent automatically.
- Not a social network. No user can see anything belonging to another user.
- Not a production multi-tenant service. It runs as a single Compose-based deployment.

---

## 3. Stakeholders

| ID | Stakeholder | Role | Interest | Requirements they bring | Influence | Elicitation technique |
|---|---|---|---|---|---|---|
| S1 | Students and job seekers (primary users) | Daily users; own all their data | Manage the search, prepare better, get offers | Fast data entry, works on a phone, analytics that state their limits, private notes stay private | High | Semi-structured interviews, survey, observation of current spreadsheets |
| S2 | Project team | Build, test, document, present | Achievable scope, clean architecture, visible individual contribution | Clear module boundaries, CI, seed data, Definition of Done | High | Sprint planning, retrospectives, brainstorming |
| S3 | Course staff | Set and review the project | Sound process, complete documentation, individual contribution | Scrum artifacts, traceability, poster, per-member commit history | Very high | Analysis of the course brief; clarification meetings |
| S4 | System administrator (role played by the team) | Deploy, monitor, recover | A reliable, recoverable system | Health endpoint, migrations, configuration through environment variables | Medium | Brainstorming |
| S5 | FL administrator (role played by the team) | Configure and run federated rounds | Correct privacy parameters, contained poisoning | Round configuration, minimum cohort, clipping and noise settings, round log, model versioning | Medium to high | FL literature; prototype experiments |
| S6 | Database maintainer (role played by the team) | Schema, migrations, integrity | Normalised schema, safe migrations, indexed queries | Flyway migrations, foreign keys, per-user isolation in the data layer | Medium | Design walkthroughs |
| S7 | University | Provides infrastructure and rules of conduct | Academic integrity, no misuse of student data, licence compliance | Data handling within institutional norms; open-source licence compliance | Low to medium, but a hard constraint | Analysis of institutional policy |

Recruiters and employers never use the system and have no interface to it. Career services staff would only matter for an institution-level dashboard, which is outside the product scope.

---

## 4. Requirement elicitation

### 4.1 Sources

Requirements come from three sources: the course brief (fixed), the end users (sampled), and the federated learning and differential privacy literature (technical constraints).

### 4.2 Techniques

| Technique | Used with | Gathers | Why it fits | Output |
|---|---|---|---|---|
| Document analysis | S3, S7, FL and DP literature, existing job trackers | Mandatory deliverables; FedAvg, DP mechanisms and their limits; what current tools offer and miss | The brief is fixed and must be traced; FL correctness cannot be guessed | Traceability matrix; FL parameter defaults; feature gap list |
| Semi-structured interviews (6 to 8, 30 minutes each) | S1 students who are applying | Real workflow, pain points, what they record after interviews, what they will not type into an app, privacy attitudes | Depth on motivation and privacy that a survey cannot reach | Pain-point list; a sensitive-data list that limits the FL feature schema |
| Survey (30 to 50 responses) | S1 wider student population | Applications per season, tools used, drop-off stages, willingness to share anonymised model updates, feature demand | Checks whether interview findings hold more widely | Ranked feature demand; share of users willing to federate, which supports opt-in by default off |
| Observation of existing trackers | 3 or 4 S1 volunteers | How their spreadsheet or Notion board is laid out and which columns they added themselves | People under-report their own workarounds; the columns they invent are requirements | Field list for the application data model |
| Brainstorming workshops | S2, with the S5 role | Architecture options, FL task choice, requirement conflicts, risks | Design needs wide generation and then selection | Design choices; trade-off list (section 11); risk list |
| Prototype walkthroughs | S1 (5 users), S3 | Reactions to clickable screens and to the Privacy Ledger | Privacy UX cannot be specified without showing people something | Backlog refinements; ledger content |

### 4.3 Known requirements and design assumptions

From the course brief:

- The stack: React, TypeScript, Tailwind, shadcn/ui and Recharts; Java and Spring Boot; PostgreSQL with JPA and Flyway; JWT and Argon2id; scikit-learn; Flower; JUnit, pytest and React Testing Library; Docker, Compose, Git and GitHub.
- Federated learning is mandatory, and raw data must not reach the central server.
- The documentation set and the Agile/Scrum process.
- Development on GitHub with visible per-member contribution.
- A concept poster.

From the domain:

- Application stages are not linear; rejection can happen at any stage.
- Users keep several resume versions.
- Interviews belong to applications and come in rounds.
- Users must never see each other's data.

Assumptions the design relies on, and how the product handles each:

| ID | Assumption | How the product handles it |
|---|---|---|
| V1 | Users complete a structured debrief if it takes under 3 minutes | The debrief is one screen with outcome and topic chips preselected from the round type, and is timed in usability testing |
| V2 | Users opt in to federated learning when they can see what is sent | Opt-in is off by default and the consent screen shows the real feature schema; the ledger shows every vector sent |
| V3 | Self-rated confidence per topic is a usable training label | The model is described as predicting self-reported confidence, not interview success (DR-08) |
| V4 | Users track enough applications for personal analytics to mean something | Every figure shows its sample size, and figures from fewer than 5 observations are marked low confidence |
| V5 | Tagging notes is enough; users do not need full-text search over notes | Applications are searchable by company and title, and global search covers applications, companies, contacts, questions and notes |

---

## 5. Functional requirements

Priority: M = Must, S = Should, C = Could, Out = outside the product scope (section 12). Out rows keep their IDs so that the numbering stays stable. Gaps in the numbering are requirements that are not part of the product.

### 5.1 Authentication and user management

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | The system shall allow a visitor to register with email, password and display name. | M |
| FR-02 | The system shall reject registration for an email that already exists, without revealing account existence in a timing-observable way. | M |
| FR-03 | The system shall enforce a password policy (at least 10 characters, checked against a common-password blocklist). | M |
| FR-04 | The system shall hash passwords with Argon2id and a per-user salt; plaintext passwords are never stored or logged. | M |
| FR-05 | The system shall authenticate users and issue a short-lived access token and a longer-lived refresh token. | M |
| FR-06 | The system shall allow a user to log out, invalidating the refresh token on the server. | M |
| FR-07 | The system shall allow a user to view and update their profile (display name, target role, graduation year, timezone). | M |
| FR-08 | The system shall allow a user to change their password after re-authenticating. | S |
| FR-09 | The system shall allow a user to permanently delete their account and all associated data, including stored FL client state. | S |
| FR-10 | The system shall allow a user to export all of their own data as JSON. | S |
| FR-11 | An administrator role able to disable accounts and view system health. | Out |

### 5.2 Job application tracking

| ID | Requirement | Priority |
|---|---|---|
| FR-12 | The system shall allow a user to create an application with company, job title, status and date applied as required fields. | M |
| FR-13 | The system shall support optional application fields: job description, job URL, location, work mode, job type, salary range and currency, priority, source, referral flag, deadline, recruiter contact, notes and tags. | M |
| FR-14 | The system shall show a paginated, sortable list of the user's own applications. | M |
| FR-15 | The system shall allow a user to search their applications by company and title. | M |
| FR-16 | The system shall allow a user to filter applications by status, stage, priority, tag, date range and work mode, with filters combinable. | M |
| FR-17 | The system shall show a single application's full detail, including related interviews, resume and status history. | M |
| FR-18 | The system shall allow a user to edit any field of their own application. | M |
| FR-19 | The system shall allow a user to delete an application after confirmation, cascading to its interviews and history. | M |
| FR-20 | The system shall allow a user to change an application's status, and shall record every change as an immutable history entry with time, previous status, new status and optional note. | M |
| FR-21 | The system shall check status transitions against a defined state machine and reject invalid ones (for example `Rejected` → `Applied`). | S |
| FR-22 | The system shall show an application's status history as a timeline. | M |
| FR-23 | The system shall keep a company record per user, reused across applications and created on demand. | M |
| FR-24 | Bulk status update for several selected applications. | Out |
| FR-25 | The system shall compute, per application, the number of days spent in the current stage. | M |
| FR-26 | The system shall flag applications whose current-stage dwell time exceeds the user's own median dwell time for that stage (minimum 5 observations), or a system default when history is insufficient. | S |
| FR-27 | The system shall allow archiving an application without deleting it. | C |

### 5.3 Resume management

| ID | Requirement | Priority |
|---|---|---|
| FR-28 | The system shall allow a user to create a resume record with name, version label, target role and summary. | M |
| FR-29 | The system shall support structured resume content: skills, experience, education, projects and achievements. | S |
| FR-30 | The system shall allow uploading a PDF or DOCX file of up to 5 MB to a resume record. | M |
| FR-31 | The system shall validate uploaded files by extension, declared MIME type and magic-byte signature, and store them outside the web root under a generated name. | M |
| FR-32 | The system shall serve resume files only to their owner, through an authenticated, expiring download endpoint. | M |
| FR-33 | The system shall allow a user to keep several resume versions and mark one as the default. | M |
| FR-34 | The system shall allow associating exactly one resume with an application, and changing that association. | M |
| FR-35 | The system shall allow a user to clone a resume into a new version, keeping its lineage (`parent_resume_id`). | S |

### 5.4 Interview tracking

| ID | Requirement | Priority |
|---|---|---|
| FR-38 | The system shall allow a user to create an interview linked to one of their applications. | M |
| FR-39 | The system shall record interview date and time, round number, round type (OA, Phone Screen, Technical, System Design, Behavioural, HR, Final), duration, mode, and interviewer name and role. | M |
| FR-40 | The system shall support several rounds per application, ordered by round number. | M |
| FR-41 | The system shall allow recording the questions asked in an interview, each with text, category, topic, difficulty and how well the user felt they answered. | M |
| FR-42 | The system shall allow linking an interview question to a question-bank entry, or saving a recorded question into the user's question bank. | S |
| FR-43 | The system shall allow recording structured post-interview feedback: overall self-rating (1 to 5), what went well, what went poorly, improvement areas, perceived difficulty and outcome. | M |
| FR-44 | Capture of feedback relayed by the recruiter, separate from self-evaluation. | Out |
| FR-45 | The system shall provide a guided debrief that takes under 3 minutes. | S |
| FR-47 | The system shall derive the topics tested in an interview from its recorded questions. | M |

### 5.5 Interview preparation

| ID | Requirement | Priority |
|---|---|---|
| FR-48 | The system shall provide a question bank with seeded system questions and user-created questions. | M |
| FR-49 | The system shall categorise questions by category (DSA, Technical, Behavioural, HR, System Design, Company-specific), topic and difficulty (Easy, Medium, Hard). | M |
| FR-50 | The system shall allow a user to create, edit and delete their own questions; seeded questions are read-only but can be copied. | M |
| FR-51 | The system shall allow a user to record a practice attempt on a question, with practice status, self-confidence (1 to 5), time taken and notes. | M |
| FR-52 | The system shall keep a topic taxonomy with prerequisite relationships between topics. | S |
| FR-53 | The system shall compute a per-topic mastery score in [0,1] from practice attempts and interview answers, with exponential time decay. | S |
| FR-54 | The system shall allow a user to create preparation tasks with title, linked topic, due date, estimated effort and completion state. | M |
| FR-55 | The system shall suggest a preparation plan from weak topics, which the user may accept, edit or dismiss. | S |
| FR-56 | The system shall show preparation progress per topic and per category. | M |
| FR-57 | The system shall provide a spaced-review queue of topics whose decayed mastery has fallen below a threshold. | C |

### 5.6 Analytics

| ID | Requirement | Priority |
|---|---|---|
| FR-58 | The system shall show an application funnel (Applied → OA → Interview → Final Round → Offer) with counts at each stage. | M |
| FR-59 | The system shall compute stage-to-stage conversion rates from the stages each application has ever reached, not its current status, using the metric definitions in `docs/architecture.md`. | M |
| FR-60 | The system shall show applications created over time in weekly buckets. | M |
| FR-61 | The system shall show interviews attended over time. | S |
| FR-62 | The system shall show the distribution of stages at which applications were rejected. | S |
| FR-64 | The system shall show per-topic interview performance from question-level self-ratings. | S |
| FR-65 | The system shall show preparation progress over time. | C |
| FR-66 | The system shall show the median dwell time per stage from the user's own history. | S |
| FR-67 | The system shall show a sample-size indicator next to every computed rate, and mark any rate from fewer than 5 observations as low confidence. | M |
| FR-68 | The system shall allow filtering analytics by date range and tag. | C |
| FR-69 | The system shall compute all analytics only from the requesting user's own records. | M |

### 5.7 Recommendations

| ID | Requirement | Priority |
|---|---|---|
| FR-70 | The system shall generate ranked, personal recommendations for the signed-in user. | M |
| FR-71 | The system shall identify weak topics from interview question ratings, practice confidence and decayed mastery. | M |
| FR-72 | The system shall route to root causes: when a weak topic has weak prerequisites, the recommendation targets the prerequisite. | S |
| FR-73 | The system shall attach to every recommendation an evidence tier (A, B, C or D), the number of supporting observations, and a plain explanation of why it was generated. | M |
| FR-74 | The system shall phrase all recommendations in associative, non-causal language, checked against a prohibited-phrase list by an automated test. | M |
| FR-75 | The system shall allow a user to accept a recommendation (creating a preparation task), snooze it, or dismiss it with a reason. | S |
| FR-76 | The system shall detect stalled applications and recommend a follow-up. | S |
| FR-77 | The system shall let the user switch between local-only and local plus federated ranking, and show the rank change between the two. | S |
| FR-78 | The system shall never generate recommendations that refer to or imply data from another identifiable user. | M |

### 5.8 Federated learning and privacy controls

| ID | Requirement | Priority |
|---|---|---|
| FR-79 | The system shall extract, on the client side, a fixed-schema numeric feature vector from a user's local data, with no free text, identifiers or company names. | M |
| FR-80 | The system shall train a model locally on a client's own data without sending training examples. | M |
| FR-81 | The system shall clip each local model update to a configured L2 norm before sending it. | M |
| FR-82 | The system shall add calibrated Gaussian noise to each local update before sending it. | M |
| FR-83 | The FL server shall aggregate client updates with FedAvg and refuse any round with fewer than k participating clients (k = 3). | M |
| FR-84 | The FL server shall reject updates whose norm exceeds the clipping bound (poisoning defence). | S |
| FR-85 | The system shall version global models and record per-round metadata: round number, participant count, aggregation time, noise multiplier, clipping norm and evaluation metric. | M |
| FR-86 | The system shall give clients the current global model on request. | M |
| FR-87 | The system shall require explicit, informed opt-in before a user's client takes part in any round, off by default. | M |
| FR-88 | The system shall allow a user to withdraw from federated participation at any time, effective from the next round. | M |
| FR-89 | The system shall keep a per-user Privacy Ledger recording, for each round the user took part in, the feature schema version, norms before and after clipping, noise multiplier, cohort size, epsilon and the vector sent. | S |
| FR-92 | The FL server shall have no credentials that can read any user's application, resume, interview or note data. | M |
| FR-93 | The system shall include the user's Privacy Ledger in their JSON data export. | C |

### 5.9 Notifications and report export

| ID | Requirement | Priority |
|---|---|---|
| FR-99 | The system shall publish a domain event when an application is flagged as stalled, when an interview date passes without a debrief, and when an interview is due within 24 hours. | S |
| FR-100 | The system shall create an in-app notification from each subscribed event, visible only to the affected user. | S |
| FR-101 | The system shall allow a user to mark a notification as read, and show unread notifications separately from read ones. | S |
| FR-102 | The system shall export a user's applications as PDF or CSV, optionally limited to a date range. | S |
| FR-103 | The system shall export a user's analytics summary (funnel, conversion rates, sample sizes) as PDF, using the same formulas and sample-size rules as the in-app analytics. | S |
| FR-104 | Exported files shall be downloadable only by their owner, through an authenticated, expiring endpoint, and never served publicly. | M |

### 5.10 Cross-cutting

| ID | Requirement | Priority |
|---|---|---|
| FR-94 | Every data-access operation shall be scoped to the signed-in user's ID in the query itself, not filtered afterwards in application code. | M |
| FR-95 | The system shall return HTTP 404 (not 403) when a user asks for a resource belonging to another user, so its existence is not confirmed. | M |
| FR-96 | The system shall validate all request payloads against typed schemas and reject unknown fields. | M |
| FR-97 | The system shall provide a health endpoint reporting API, database and FL service reachability. | S |
| FR-98 | The system shall provide demonstration seed data, clearly marked as synthetic. | S |

---

## 6. Non-functional requirements

### 6.1 Security

| ID | Requirement | Measure |
|---|---|---|
| NFR-01 | Passwords stored with Argon2id, memory cost at least 64 MiB, time cost at least 3, unique salt | Unit test asserting the hash prefix and parameters |
| NFR-02 | Access tokens expire within 15 minutes; refresh tokens within 7 days and can be revoked on the server | Token lifetime assertions in tests |
| NFR-03 | All database access uses parameterised queries; no string-built SQL | CI check plus code review |
| NFR-04 | Every endpoint except register, login and health requires a valid bearer token | Test that reads every endpoint from the OpenAPI document and asserts 401 without a token |
| NFR-05 | Cross-user access returns 404 | Security test suite covering every owned resource type |
| NFR-06 | Uploaded files checked by magic bytes and stored under generated names outside served paths | A renamed executable is rejected |
| NFR-07 | Rate limiting on authentication endpoints: at most 10 attempts per IP per 15 minutes | Integration test |
| NFR-08 | Security headers on every response (CSP, X-Content-Type-Options, Referrer-Policy, HSTS) | Response header test |
| NFR-09 | No secret is committed; all secrets come from the environment | CI secret scan |

### 6.2 Privacy

| ID | Requirement | Measure |
|---|---|---|
| NFR-10 | No raw record, free text, identifier or company name is sent to the FL server | Test asserting the client payload matches an allow-listed numeric schema exactly |
| NFR-11 | The FL server container holds no database credentials for user data | CI inspection of the container environment |
| NFR-12 | Federated participation is opt-in, off by default and revocable | Test asserting a new user's `fl_opt_in` is false and no update is produced |
| NFR-13 | Aggregation needs at least 3 participating clients | Unit test: a 2-client round is refused |
| NFR-14 | Account deletion removes all of the user's rows and files and leaves no orphan records | Deletion test with an orphan check across all tables |
| NFR-15 | Analytics, recommendations and AI output never refer to another identifiable user | Code review plus tests on recommendation output and AI prompt assembly |
| NFR-16 | Only the fields the product needs are collected; every field traces to a requirement | Data dictionary review |

### 6.3 Performance

| ID | Requirement | Measure |
|---|---|---|
| NFR-17 | 95th-percentile response within 300 ms for list and detail endpoints with 500 applications, excluding analytics | Load test with seeded data |
| NFR-18 | Analytics endpoints respond within 800 ms at p95 for 500 applications and 200 interviews | Load test |
| NFR-19 | Frontend first contentful paint within 2 s on a mid-range laptop over a local network | Lighthouse run |
| NFR-20 | The application list is paginated at 25 rows and never loads the full table | Code review and response size assertion |
| NFR-21 | A federated round of 5 clients completes within 60 s in the demo configuration | Timed FL run |

### 6.4 Scalability

| ID | Requirement | Measure |
|---|---|---|
| NFR-22 | Schema and queries support 10,000 applications per user without full-table scans; all foreign key and filter columns indexed | `EXPLAIN` review of the busiest queries |
| NFR-23 | The backend holds no state outside the database, so it can be replicated | Architecture review |
| NFR-24 | Clients can be added to the federation without server code changes | Client count set in configuration |

### 6.5 Availability and reliability

| ID | Requirement | Measure |
|---|---|---|
| NFR-25 | 99% availability during the demonstration period | Uptime log |
| NFR-26 | All write operations are transactional; a failed multi-step operation leaves no partial state | Integration test with an induced failure |
| NFR-27 | Database migrations are versioned and reversible | Flyway migration and rollback scripts tested |
| NFR-28 | A failed FL round never corrupts the current global model; the previous version stays servable | Test that fails a round and checks the served model |
| NFR-29 | A documented backup and restore procedure for the database | Restore procedure written and tried |

### 6.6 Usability

| ID | Requirement | Measure |
|---|---|---|
| NFR-30 | A new user can create their first application within 60 seconds of first login | Usability test with 5 participants |
| NFR-31 | A post-interview debrief takes 3 minutes or less | Timed usability test |
| NFR-32 | Every list view has a designed empty state with a primary action | UI review checklist |
| NFR-33 | Every asynchronous action shows loading, success and error states | UI review checklist |
| NFR-34 | Destructive actions need explicit confirmation naming the object | UI review checklist |
| NFR-35 | Error messages say what happened and what to do next; no stack traces or raw status codes reach the user | UI review |

### 6.7 Maintainability

| ID | Requirement | Measure |
|---|---|---|
| NFR-36 | Backend layered as controllers → services → repositories → entities, with no layer skipped | Code review |
| NFR-37 | Backend line coverage at least 70% | JaCoCo coverage gate in CI |
| NFR-38 | fl-service public functions are typed; frontend TypeScript runs in `strict` mode with no `any` in committed code | Type check |
| NFR-39 | Frontend code is linted | `npm run lint` |
| NFR-40 | Every non-obvious design choice is written down with its reason | Design choices section in `docs/architecture.md` |

### 6.8 Portability

| ID | Requirement | Measure |
|---|---|---|
| NFR-41 | The full stack starts with one `docker compose up` on Linux, macOS and Windows | Checked by each team member on their own machine |
| NFR-42 | No hard-coded absolute paths, hostnames or ports outside configuration | Code review |
| NFR-43 | The frontend supports the last two major versions of Chrome, Firefox, Edge and Safari | Manual check |

### 6.9 Accessibility

| ID | Requirement | Measure |
|---|---|---|
| NFR-44 | Colour contrast meets WCAG 2.1 AA (4.5:1 for text, 3:1 for UI elements) in both themes | Automated contrast test on the design tokens |
| NFR-45 | Every interactive element is keyboard reachable with a visible focus ring; dialogs trap and restore focus | Keyboard-only walkthrough |
| NFR-46 | Every form input has a label; errors are linked with `aria-describedby` | axe-core scan with no critical violations |
| NFR-47 | Status is never shown by colour alone; badges carry text | UI review |
| NFR-48 | The layout works from 360 px to 1920 px wide | Responsive check at 360, 768, 1280 and 1920 px |

### 6.10 Testability

| ID | Requirement | Measure |
|---|---|---|
| NFR-49 | All external dependencies (database, file system, FL server, clock, AI provider) are injectable and mockable | Architecture review |
| NFR-50 | The test suite runs offline and deterministically; all randomness is seeded | CI run |
| NFR-51 | A fixed fixture dataset produces identical analytics output on every run | Golden-file tests |
| NFR-52 | The full backend suite completes within a few minutes in CI | CI timing |
| NFR-53 | The notification channel is pluggable (Strategy pattern), so a new channel needs no change to event detection | Unit test swapping the channel |

---

## 7. Domain requirements

Domain requirements are facts about recruitment, interviewing and federated learning that the system has to respect. A functional requirement says what the system does; a domain requirement says what the world is like.

### 7.1 Job applications and recruitment

| ID | Domain requirement | Consequence |
|---|---|---|
| DR-01 | An application moves through discrete stages, but not in a straight line: rejection or withdrawal can happen at any stage, and stages can be skipped. | The status model is a state machine with terminal transitions from every open state, not a fixed pipeline. |
| DR-02 | No response is the most common outcome, and it is not an event: no employer sends an "we are ignoring you" signal. | "Ghosted" is inferred from elapsed time, and the funnel does not count missing outcomes as failures. |
| DR-03 | An application belongs to one company and one role, but a user can hold several applications with the same company. | Company is a separate entity, and (user, company) is not unique on applications. |
| DR-04 | Stages are not standard across employers; one company's "OA" is another's "screening test", and the number of rounds varies from 1 to 7. | The stage set is small and general, with a free-text round label alongside. |
| DR-05 | Timelines are long and irregular; weeks between stages is normal, and outcomes can arrive months later. | Time in stage is treated as a distribution, and open applications are not counted as failures. |
| DR-06 | Outcomes depend on many things the system cannot see: referrals, hiring freezes, timing, luck. | The system never attributes an outcome to a single tracked variable (FR-74). |

### 7.2 Interviews

| ID | Domain requirement | Consequence |
|---|---|---|
| DR-07 | Interview content is recalled from memory, fades quickly, and is often wrong on detail. | The debrief is fast, allows partial records and never requires fields the user cannot reliably give. |
| DR-08 | Self-assessment of interview performance is biased and only weakly linked to the real result. | Self-rating is an input, not ground truth; the model is described as predicting self-reported confidence, not success. |
| DR-09 | One round usually tests several topics at once. | Topics are attributed per question, not per round. |
| DR-10 | Interview questions are often under NDA or otherwise confidential. | Question text is private, never shown on any shared surface and never part of a federated payload. |
| DR-11 | Interviewer identity is personal data about a third party who has not agreed to be in the system. | Interviewer fields are optional free text, excluded from analytics and model features, and deleted with the account. |

### 7.3 Resumes

| ID | Domain requirement | Consequence |
|---|---|---|
| DR-12 | A resume is a versioned document tailored per role; a job seeker keeps several at once. | Versioning with lineage is part of the domain, not a convenience. |
| DR-13 | Resume files contain directly identifying data: name, phone, address, sometimes date of birth. | Resume files are the most sensitive artefact: never served publicly and never used for federated features. |
| DR-14 | Each resume version is used for few applications (typically 5 to 40), which gives very little statistical power. | The product does not compare resume versions by outcome; a "Resume B is 12% better" figure from 6 applications would mislead. |

### 7.4 Interview preparation

| ID | Domain requirement | Consequence |
|---|---|---|
| DR-15 | Technical topics have prerequisites: shortest-path algorithms cannot be learned before graph traversal. | The topic taxonomy is a directed acyclic graph, and recommendations follow it (FR-72). |
| DR-16 | Skill fades without practice; a topic practised four months ago is not ready now. | Mastery has a time-decay term (FR-53). |
| DR-17 | Preparation time competes with classes and personal commitments. | Recommendations are few and ranked, with realistic effort estimates. |

### 7.5 Privacy and federated learning

| ID | Domain requirement | Consequence |
|---|---|---|
| DR-18 | Job-search data is career-sensitive: disclosure that a user is interviewing elsewhere can cause real harm. | Privacy is a precondition for accurate data entry, and so for the analytics. |
| DR-19 | Federated learning needs a task that can be learned from a fixed-schema numeric vector. | Anything that needs raw text or cross-user joins is outside the FL task. |
| DR-20 | Federated data is non-IID: each user's data reflects their own field, seniority and location. | FedAvg converges unevenly, and evaluation reports per-client as well as global metrics. |
| DR-21 | Model updates can leak information about the training data. | Clipping and calibrated noise are mandatory. "We use FL, so it is private" is not claimed anywhere. |
| DR-22 | A cohort of only a few clients gives almost no anonymity. | The minimum cohort k is enforced on the server (FR-83). |
| DR-23 | Any client in a federation may be malicious and send crafted updates. | Norm-based filtering is in place (FR-84), and the threat model states what it does not defend against. |

---

## 8. Epics

| EPIC | Name | Stories | Points | Business value |
|---|---|---:|---:|---|
| 1 | Authentication and User Management | 11 | 42 | The gate for everything; the privacy foundation |
| 2 | Job Application Tracking | 16 | 64 | The daily-use loop and the wider job-search workflow |
| 3 | Resume Management | 6 | 22 | Links documents to applications |
| 4 | Interview Tracking | 8 | 29 | Captures the most valuable data in the system |
| 5 | Interview Preparation and Topic Mastery | 11 | 54 | Turns experience into practice |
| 6 | Analytics and Pipeline Health | 9 | 40 | Makes performance readable |
| 7 | Explainable Recommendations | 6 | 27 | Personal, evidence-backed next steps |
| 8 | Federated Learning and Privacy Ledger | 10 | 58 | Learning across users with inspectable privacy |
| 9 | Platform, Quality and DevOps | 8 | 41 | Makes the rest buildable and testable |
| 10 | Notifications | 4 | 11 | Surfaces what needs attention without checking the dashboard |
| 11 | Experience and First Run | 6 | 25 | A product people want to use every day |
| 12 | AI Assistance | 4 | 16 | Private, grounded help on a local model |

Total: 99 stories, 429 points. Every story belongs to exactly one epic. EPIC 8 combines federated learning and privacy because the privacy design is part of the FL design, not a separate policy. EPIC 9 makes platform work visible in the backlog.

---

## 9. User stories

Format: As a [role], I want [goal], so that [benefit]. Roles: job seeker (primary), developer, FL administrator, operator. Points use a modified Fibonacci scale (1, 2, 3, 5, 8, 13), where 1 point is about 2 to 3 focused hours for a student developer.

### EPIC 1: Authentication and User Management

US-01, Register an account · Must · 5 points
> As a job seeker, I want to register with my email and a password, so that I have a private account for my job-search data.
- Given a valid email, a password of at least 10 characters and a display name, the account is created and I land on onboarding.
- The password is stored as an Argon2id hash; the plaintext is in no database column, log line or API response.
- Registering with an email already in use returns a generic failure that does not confirm the address is registered.
- A password shorter than 10 characters or on the common-password blocklist is rejected with an inline field error.
- Email format is checked on client and server; the server decides.
- A new user has `fl_opt_in = false`.

US-02, Log in · Must · 3 points
> As a job seeker, I want to log in, so that I can reach my own data.
- Correct credentials return an access token (15 minutes or less) and a refresh token, and I land on the dashboard.
- Wrong credentials return 401 with a message that does not reveal whether the email exists.
- After 10 failed attempts from one IP within 15 minutes, further attempts are rate-limited.
- A valid session survives a page refresh.

US-03, Log out · Must · 2 points
> As a job seeker, I want to log out, so that my data cannot be reached from a shared machine.
- Logging out clears client-side credentials and revokes the refresh token on the server.
- The revoked refresh token cannot get a new access token.
- Protected routes redirect to login after logout.

US-04, Session persistence and expiry · Must · 3 points
> As a job seeker, I want my session refreshed quietly while I work and ended when it truly expires, so that I am neither logged out mid-task nor left logged in forever.
- An expired access token is refreshed once and the original request is retried.
- If the refresh token is expired or revoked, I am sent to login with a message.
- Concurrent requests during a refresh do not trigger several refresh calls.

US-05, View and edit my profile · Must · 3 points
> As a job seeker, I want to keep my profile up to date, so that defaults fit my situation.
- I can view and update display name, target role, graduation year and timezone.
- Changes persist and show immediately in the header.
- I cannot change my email to one already in use.
- Validation errors appear next to the field.

US-06, Change my password · Should · 3 points
> As a job seeker, I want to change my password, so that I can respond to a suspected compromise.
- Needs the current password; all other refresh tokens are revoked on success; the new password is checked against the policy.

US-07, Export all my data · Should · 3 points
> As a job seeker, I want to export everything I have entered as JSON, so that I am not locked in.
- The export includes applications, resume metadata, interviews, questions, tasks, stories, notifications and the Privacy Ledger; it contains no other user's data.

US-08, Delete my account · Should · 5 points
> As a job seeker, I want to permanently delete my account, so that I control my own footprint.
- Needs typed confirmation; removes every row referencing my ID plus uploaded files and FL client state; an orphan check afterwards returns zero rows.

US-91, Onboarding and starter plan · Should · 5 points
> As a new user, I want onboarding to ask my target role and date and give me a starter plan, so that I do not land on an empty dashboard.
- First login goes to onboarding, not the dashboard; target role and date are saved to the profile; starter preparation tasks are created and can be edited or removed.
- I can import an existing tracker from CSV, including LinkedIn's "Job Applications.csv" export and Google Sheets or Notion layouts, with a preview before anything is saved; unknown statuses map to Saved with a warning.

US-92, Password reset and email verification · Should · 5 points
> As a job seeker, I want to reset a forgotten password and verify my email, so that I can recover my account and prove the address is mine.
- Reset and verification tokens are single-use and expire; a reused or expired token is rejected; the reset request gives the same response whether or not the email exists.

US-93, Sign in with Google, GitHub or LinkedIn · Could · 5 points
> As a job seeker, I want to sign in with Google, GitHub or LinkedIn, so that I do not need another password.
- Provider sign-in issues the app's own tokens; an existing account is linked only when the provider has verified the email; email and password sign-in works unchanged; a provider button appears only when that provider is configured.

### EPIC 2: Job Application Tracking

US-09, Create an application · Must · 5 points
> As a job seeker, I want to record a job application, so that I stop losing track of where I applied.
- The form requires company, job title, status and date applied; everything else is optional.
- Typing a company name offers my existing companies and creates a new one if none matches.
- On save I return to the list with the new row visible and a confirmation toast.
- The server rejects a future date applied and an unknown status.
- The record is owned by me; the owner ID comes from the token and can never be supplied by the client.
- Pasting a job posting fills empty fields, and a "Save to Cohort" bookmarklet opens the form prefilled from the job page I am viewing.

US-10, List my applications · Must · 5 points
> As a job seeker, I want a table of all my applications, so that I can see my whole search at once.
- Columns: company, role, status, stage, days in stage, date applied, priority.
- Paginated at 25 rows with page-size options; total count shown.
- Sortable by date applied, company, status and days in stage.
- Only my applications are returned.
- An empty state with a "Create your first application" action appears when I have none.
- Loading and error states are distinct from the empty state.

US-11, View application detail · Must · 3 points
> As a job seeker, I want a detail view for an application, so that I can see everything about it in one place.
- Shows all stored fields, the linked resume, the status timeline and related interviews.
- Another user's application ID returns 404.
- A non-existent ID returns 404 with a not-found screen.

US-12, Edit an application · Must · 3 points
> As a job seeker, I want to edit an application, so that I can correct and add to it as I learn more.
- Every field is editable; `updated_at` changes; unchanged fields stay as they were; concurrent edits use last write wins.

US-13, Delete an application · Must · 3 points
> As a job seeker, I want to delete an application, so that mistakes and duplicates do not distort my analytics.
- A confirmation names the company and role; deletion cascades to interviews, questions and status history; the row disappears without a full page reload; deleting another user's record returns 404.

US-14, Change status and record history · Must · 5 points
> As a job seeker, I want to update an application's status with each change recorded, so that I keep an accurate history and not only the current state.
- Status can be changed from the list row and the detail view.
- Every change writes an immutable `application_status_history` row: previous status, new status, time and optional note.
- History rows cannot be edited or deleted on their own.
- The state machine rejects invalid transitions with a clear message.
- Moving to a terminal status asks for an optional reason.

US-15, See an application timeline · Must · 3 points
> As a job seeker, I want a timeline on the application detail page, so that I can see how it progressed.
- Newest-first timeline combining status changes and interviews; each entry shows relative and exact time; an empty state when only the creation event exists.

US-16, Search applications · Must · 3 points
> As a job seeker, I want to search by company or role, so that I can find one application among hundreds.
- Case-insensitive match on company name and job title; debounced at 300 ms; result count shown; a no-results state distinct from the empty state.

US-17, Filter and sort applications · Must · 5 points
> As a job seeker, I want to combine filters, so that I can narrow down to exactly the applications I care about.
- Filters on status, stage, priority, tag, work mode and date range, combined with AND; active filters shown as removable chips; filters and sort remembered per browser; "Clear all" resets.

US-18, Days in stage and stall flag · Should · 5 points
> As a job seeker, I want stalled applications flagged, so that I follow up before an opportunity dies quietly.
- Days in stage computed from the latest history entry; an application is flagged when it passes my own median dwell time for that stage (at least 5 observations) or a default; the flag explains its threshold and sample size.

US-89, Kanban board · Should · 5 points
> As a job seeker, I want to manage applications on a board, so that I can move them through stages at a glance.
- Dragging between columns calls the status endpoint; an illegal move is refused by the state machine and snaps back with the reason; a backward move asks for a note; every accepted move writes history.

US-95, Contacts and referrals · Should · 5 points
> As a job seeker, I want to track contacts and referrals per company, so that networking is part of my search.
- Contacts belong to me and link to companies and applications; referral status is recorded; a contact with no interaction for a set period raises a nudge; referral request and reconnect messages can be drafted; another user's contacts return 404.

US-96, Company pages · Should · 3 points
> As a job seeker, I want a page per company, so that everything I know about an employer is in one place.
- Shows my applications, interviews, recorded questions, contacts and notes for that company; private notes are editable; only my own records appear.
- A read-only company directory of general interview patterns is linked when the name matches; anonymous, opt-in experience contributions feed community figures that show only with 5 or more contributors.

US-99, Global search · Should · 5 points
> As a job seeker, I want one search box across everything, so that I can find any record quickly.
- One query searches applications, companies, contacts, questions and notes; results open from the command palette; results never include another user's data.

US-100, Offer comparison · Should · 3 points
> As a job seeker, I want to compare offers side by side with deadline countdowns, so that I decide well and on time.
- Offers record compensation and terms; two or more offers show side by side; each shows the days left to its deadline.

US-101, Weekly goals and activity heatmap · Should · 3 points
> As a job seeker, I want weekly goals and an activity heatmap, so that I keep a steady pace.
- Weekly targets for applications and practice with progress; a calendar heatmap of activity; the current streak shows on the dashboard.

### EPIC 3: Resume Management

US-20, Create a resume record · Must · 3 points
> As a job seeker, I want to register a resume version, so that I can tell apart the versions I send out.
- Name, version label, target role and summary; one resume can be the default; versions belong only to me.

US-21, Upload a resume file · Must · 5 points
> As a job seeker, I want to attach the actual PDF, so that I know exactly which document I sent.
- Accepts PDF and DOCX up to 5 MB.
- Checks extension, declared MIME type and magic bytes; a `.exe` renamed to `.pdf` is rejected.
- Files are stored outside any served directory under a generated name; the original name is kept only as metadata.
- Upload shows progress, and a failure shows a specific error.

US-22, Download my resume file · Must · 3 points
> As a job seeker, I want to download a stored resume, so that I can reuse it.
- Download needs authentication; the link expires; another user's file returns 404; `Content-Disposition` and content type are set.

US-23, Structured resume content · Should · 5 points
> As a job seeker, I want to record skills, experience, education and projects, so that the resume has structure and is not just a file.
- Repeatable sections with add, remove and reorder; skills are tags; content saves separately from the file; a clean PDF can be exported from the sections.

US-24, Attach a resume to an application · Must · 3 points
> As a job seeker, I want to record which resume I used for an application, so that I know what each employer received.
- Selectable on the application form; one resume per application; the link is visible from both sides; it can be changed.

US-25, Clone a resume into a new version · Should · 3 points
> As a job seeker, I want to copy a resume as a new version, so that I can tailor it without starting over.
- The clone copies structured content, not the file; `parent_resume_id` records lineage; the version label increments.

### EPIC 4: Interview Tracking

US-27, Schedule or record an interview · Must · 5 points
> As a job seeker, I want to record an interview against an application, so that my rounds are tracked in context.
- Date and time, round number, round type, duration, mode and interviewer; the interview must belong to one of my applications (checked on the server); round numbers are unique within an application; the interview can be downloaded as a calendar (.ics) event.

US-28, List and view interviews · Must · 3 points
> As a job seeker, I want to see all my interviews, upcoming and past, so that I can prepare and review.
- Split into upcoming and past; each row links to its application; empty and loading states present; only my interviews are returned.

US-29, Record questions asked · Must · 5 points
> As a job seeker, I want to log the questions I was asked, so that I keep the most valuable part of the interview before I forget it.
- Several questions per interview, each with text, category, topic, difficulty and a self-rating; questions are added inline; question text is private and never appears in any shared or federated output.

US-30, Post-interview debrief · Must · 5 points
> As a job seeker, I want a guided debrief, so that I record a structured reflection quickly instead of writing nothing.
- Captures self-rating (1 to 5), what went well, what went poorly, improvement areas, perceived difficulty and outcome; outcome and topic chips are preselected from the round type; partial input is kept as a draft; it takes under 3 minutes; the dashboard prompts for past interviews without a debrief.

US-31, Edit and delete an interview · Must · 3 points
> As a job seeker, I want to correct interview records, so that my history is accurate.
- All fields editable; deletion is confirmed and cascades to its questions and feedback; another user's interview returns 404.

US-34, Derive tested topics from an interview · Must · 3 points
> As a job seeker, I want the topics I was tested on taken from my recorded questions, so that preparation can target them without extra typing.
- The topic set is the distinct topics of the interview's questions; it feeds the mastery calculation and the interview prep pack.

US-110, STAR story bank · Should · 3 points
> As a job seeker, I want a bank of STAR stories with competency tags, so that behavioural answers are ready before the interview.
- Create, edit and delete stories with Situation, Task, Action and Result fields and competency tags; filter by competency; a story can be linked to an interview; stories are private to me.

US-118, Save an interview question to the question bank · Should · 2 points
> As a job seeker, I want to save a question I was asked into my question bank, so that I can practise it later.
- From the interview page, one action creates a personal bank question with the same text and topic and links the interview question to it; a question without a topic asks for one first; another user's question returns 404.

### EPIC 5: Interview Preparation and Topic Mastery

US-35, Browse the question bank · Must · 5 points
> As a job seeker, I want a searchable bank of practice questions, so that I have a starting point for preparation.
- Seeded with real, original questions for every topic; filter by category, topic and difficulty; my own questions and seeded ones are marked differently.

US-36, Add my own questions · Must · 3 points
> As a job seeker, I want to add questions I came across, so that my bank reflects my real interviews.
- Create, edit and delete my own; seeded questions are read-only but can be copied.

US-37, Record a practice attempt · Must · 5 points
> As a job seeker, I want to log practising a question, so that my progress is measured and not just remembered.
- Captures status (not started, in progress, confident, needs review), confidence 1 to 5, time taken and notes; attempts are added, never overwritten, so a confidence history exists.

US-38, Topic taxonomy with prerequisites · Should · 5 points
> As a developer, I want a topic graph with prerequisite edges, so that recommendations can find root causes rather than symptoms.
- 48 topics across categories, each with a short description; edges form a DAG checked by a cycle-detection test; every question links to exactly one topic.

US-39, Topic mastery with decay · Should · 8 points
> As a job seeker, I want a mastery score per topic that fades if I stop practising, so that my readiness reflects reality.
- Score in [0,1] combining practice confidence and interview answers, with exponential time decay; the formula is documented, tested against fixed fixtures and explained in the UI.

US-40, Create preparation tasks · Must · 3 points
> As a job seeker, I want a task list for preparation, so that intentions become scheduled work.
- Title, linked topic, due date, estimated effort and completion; overdue tasks are marked; completing a task updates progress.

US-41, Preparation progress view · Must · 5 points
> As a job seeker, I want to see progress by topic and category, so that I know where I stand.
- Progress bars per category; a topic grid with a readiness level that does not rely on colour alone; drill-down from category to topic to questions.

US-42, Spaced review queue · Could · 5 points
> As a job seeker, I want topics brought back as my mastery fades, so that I keep what I learned.
- A daily queue ordered by decayed mastery; scheduling is deterministic on fixed fixtures.

US-90, Question bank browser and practice logging · Must · 5 points
> As a job seeker, I want to browse the question bank and log practice from the UI, so that my mastery changes as I work.
- Every topic has real interview questions; I can filter and drill from topic to question; logging an attempt updates mastery visibly.

US-107, Job readiness score · Should · 5 points
> As a job seeker, I want each application to show how ready I am for that job, so that I prepare for the role in front of me.
- Topics named in the job posting are weighted by my mastery; the score shows its sample size and is hidden when evidence is thin.

US-109, Mock interview · Should · 5 points
> As a job seeker, I want a timed mock interview from my weakest topics, so that practice feels like the real thing.
- Questions come from my weakest topics, including topics with only a few attempts; I grade each answer myself; each answer is saved as a practice attempt on the right topic.

### EPIC 6: Analytics and Pipeline Health

US-43, Application funnel · Must · 5 points
> As a job seeker, I want a funnel from applications to offers, so that I can see where my search loses applications.
- Stages Applied → OA → Interview → Final → Offer, counted by stages ever reached (from history), not current status; open applications are shown as still in progress, not as failures.

US-44, Conversion rates · Must · 5 points
> As a job seeker, I want stage-to-stage conversion rates, so that I know which step to work on.
- Five rates using the defined formulas; every rate shows its denominator; rates from fewer than 5 observations are marked low confidence.

US-45, Activity over time · Must · 3 points
> As a job seeker, I want applications and interviews plotted over time, so that I can see my own consistency.
- Weekly buckets with applications and interviews as separate series; an empty state for users with no history.

US-46, Rejection stage distribution · Should · 3 points
> As a job seeker, I want to see at which stage I am rejected, so that I can target the right skill.
- Distribution of the last stage reached before rejection; "rejected" and "no response" are kept apart.

US-47, Topic performance · Should · 5 points
> As a job seeker, I want per-topic interview performance, so that I can see which topics hurt me in real rounds.
- Mean self-rating per topic with question counts; topics with fewer than 3 questions are grouped as "insufficient data".

US-48, Stage dwell times · Should · 3 points
> As a job seeker, I want the median time spent at each stage, so that I know what normal looks like for me.
- Median per stage from completed transitions only; applications still in a stage are reported separately.

US-49, Dashboard pipeline and action queue · Must · 8 points
> As a job seeker, I want the dashboard to tell me what to do today, so that the app is a tool rather than a report.
- A pipeline with live counts per stage; a ranked action queue mixing stalled applications, missing debriefs, upcoming interviews and due prep tasks; every item is one click from its action; the queue is capped at 7 items.

US-50, Sample sizes shown across analytics · Must · 3 points
> As a job seeker, I want to know how much data each number is based on, so that I do not over-read noise.
- Every rate, average and median shows n; one shared low-confidence badge is used everywhere; golden-file tests pin outputs for a fixed dataset.

US-80, Export my applications and analytics · Should · 5 points
> As a job seeker, I want to export my application list and analytics summary as PDF or CSV, so that I can share progress outside the app or keep an offline record.
- Exports follow FR-102 to FR-104; downloads are owner-only, authenticated and expiring.

### EPIC 7: Explainable Recommendations

US-51, Weak-topic recommendations · Must · 8 points
> As a job seeker, I want to be told which topics to work on, so that preparation is targeted.
- A ranked list of at most 5; each cites the observations behind it; topics with no supporting evidence are never recommended.

US-52, Root-cause prerequisite routing · Should · 5 points
> As a job seeker, I want to be pointed at the underlying gap rather than the symptom, so that I fix the cause.
- When a weak topic has a prerequisite with lower mastery, the recommendation targets the prerequisite and explains the chain; routing depth is capped at 2.

US-53, Evidence tiers and explanations · Must · 5 points
> As a job seeker, I want to know where each recommendation came from and how confident it is, so that I can judge it myself.
- Every recommendation carries a tier (A to D), an observation count and a plain "why am I seeing this?" explanation; a test asserts that none renders without an explanation.

US-54, Non-causal language · Must · 3 points
> As a job seeker, I want the app to be clear that correlation is not causation, so that I am not misled about my chances.
- All recommendation text lives in a template registry; a test fails the build if any template matches the prohibited-phrase list.

US-55, Act on a recommendation · Should · 3 points
> As a job seeker, I want to turn a recommendation into a task, snooze it or dismiss it, so that the list stays relevant.
- Accept creates a linked preparation task and shows the accepted state; snooze hides it for 7 days; dismiss records a reason, warns that it hides that recommendation type for 30 days, and snoozed or dismissed items can be managed.

US-56, Stalled-application follow-ups · Should · 3 points
> As a job seeker, I want to be nudged to follow up on stalled applications, so that opportunities do not lapse silently.
- Generated from the stall flag; names the application and its dwell time; at most 3 at a time.

### EPIC 8: Federated Learning and Privacy Ledger

US-57, Opt in to federated learning · Must · 5 points
> As a job seeker, I want to choose whether to contribute to the shared model, so that taking part is my decision.
- Off by default for every new user; the opt-in screen states what is sent and what is not, showing the real feature schema; a test asserts that an opted-out client produces no update.

US-58, Local feature extraction · Must · 8 points
> As an FL administrator, I want a fixed numeric feature schema computed entirely on the client, so that raw data is never a candidate for sending.
- A fixed-length, versioned, all-numeric vector of 24 features; a serialisation test asserts the payload matches the allow-listed schema exactly and contains no strings; extraction is point-in-time correct and reads only the client's own store.

US-59, Local model training · Must · 8 points
> As an FL administrator, I want each client to train on its own data, so that learning happens without moving data.
- A scikit-learn model trained in the client process; training examples never leave; deterministic under a fixed seed.

US-60, Clipping and noise before sending · Must · 5 points
> As a job seeker, I want my model update masked before it leaves my client, so that it cannot be turned back into my data.
- The update is L2-clipped to a set norm, then Gaussian noise is added; values before and after are recorded in the ledger; a test asserts the sent vector differs from the raw update and respects the norm bound.

US-61, Aggregation with a minimum cohort · Must · 8 points
> As an FL administrator, I want FedAvg aggregation that refuses small cohorts, so that averaging gives real anonymity.
- The Flower server aggregates 3 or more clients and refuses fewer with a logged reason; round metadata is stored; the server holds no database credentials for user data, checked in CI.

US-62, Global model distribution · Must · 5 points
> As a job seeker, I want my client to use the improved shared model, so that I benefit from what the cohort learned.
- Clients fetch the current global model version; the version in use is shown in the UI; a failed round leaves the previous model servable.

US-63, Privacy Ledger · Should · 8 points
> As a job seeker, I want a log of everything my client has sent, so that I can check the privacy claim instead of trusting it.
- One entry per round: round ID, time, schema version, norm before and after clipping, noise multiplier, cohort size, epsilon and the exact vector sent, expandable; a "what was not sent" list; entries cannot be changed.
- A demo command runs a round with synthetic clients, so a ledger entry can be shown with only a few real users.

US-64, Withdraw from federation · Must · 3 points
> As a job seeker, I want to stop contributing at any time, so that consent can be taken back.
- Takes effect from the next round; earlier contributions are explained as already aggregated; the ledger stays visible after withdrawal.

US-65, Federated Lift viewer · Should · 5 points
> As a job seeker, I want to compare my recommendations with and without the shared model, so that I can see what federation adds.
- A toggle re-ranks recommendations; changed items show their rank change; the adjustment from the federated model is bounded to [0.75, 1.25]; when the FL service is down the page falls back to local-only ranking.

US-84, Poisoning filter · Should · 3 points
> As an FL administrator, I want over-norm updates rejected before aggregation, so that one malicious client cannot steer the shared model.
- Updates whose norm exceeds the bound are discarded and logged; a crafted over-norm update in a test is left out of the average.

### EPIC 9: Platform, Quality and DevOps

Infrastructure, CI, seed data and security hardening take real effort, so they are stories in the backlog like everything else.

US-67, Repository and toolchain bootstrap · Must · 5 points
> As a developer, I want a configured repository, so that the team can work in parallel without conflicts.
- Monorepo layout, `.gitignore`, `.env.example`, licence, README, PR conventions and commit convention documented; every member can clone and run.

US-68, Docker Compose development environment · Must · 5 points
> As a developer, I want one-command startup, so that the app runs the same on every machine.
- `docker compose up` starts Postgres, the API and the frontend; checked by every team member on their own OS; documented in the README.

US-69, Database schema and migrations · Must · 8 points
> As a developer, I want versioned migrations, so that schema changes are reproducible and reversible.
- Flyway migrations create every table; each migration has a rollback script; migrations run cleanly on an empty database in the test suite.

US-70, CI pipeline · Must · 5 points
> As a developer, I want automated checks on every push, so that quality is enforced.
- GitHub Actions builds the API, runs backend tests with the coverage gate, runs frontend and fl-service tests, scans for secrets and string-built SQL, and checks that the FL service has no database credentials; a failing check blocks the merge.

US-71, Application shell and navigation · Must · 5 points
> As a job seeker, I want consistent navigation and layout, so that the product feels like one thing.
- Sidebar with grouped sections, collapse on small screens, active-route highlight, protected routes, a 404 page, a global error boundary and toasts.

US-72, Seed and demo data · Should · 3 points
> As a developer, I want realistic seed data, so that features can be shown and analytics tested without typing everything in.
- One seeded demo user with applications, interviews, resumes, practice history, contacts, offers, goals and stories; the data is deterministic, loads the same twice, and is marked synthetic.

US-73, Security test suite · Must · 5 points
> As a developer, I want automated cross-user access tests, so that isolation is proven, not assumed.
- For every owned resource type, user B gets 404 for user A's object; every non-public endpoint returns 401 without a token; endpoints are read from the OpenAPI document so a new one cannot be missed.

US-74, Accessibility and responsive pass · Should · 5 points
> As a job seeker with accessibility needs, I want the app to work with a keyboard and a screen reader, so that I can actually use it.
- axe-core reports no critical violations on the main pages; a full keyboard walkthrough passes; layouts work at 360, 768, 1280 and 1920 px.

### EPIC 10: Notifications

US-76, Notify me when an application stalls · Should · 3 points
> As a job seeker, I want to be notified when an application stalls, so that I do not have to keep checking the dashboard.
- A stall raises a domain event; an in-app notification is created for that user only.

US-77, Notify me before an interview · Should · 3 points
> As a job seeker, I want a reminder before a scheduled interview, so that I remember to prepare.
- An interview due within 24 hours raises a notification.

US-78, Notify me about a missing debrief · Should · 2 points
> As a job seeker, I want a nudge when I have not debriefed a past interview, so that I do not lose the details.
- An interview date passing without a debrief raises a notification that opens the debrief.

US-79, Notification bell and mark as read · Should · 3 points
> As a job seeker, I want a notification centre, so that I have one place to see what the app has flagged.
- A bell with an unread count; unread shown apart from read; notifications can be marked as read.

### EPIC 11: Experience and First Run

US-85, Consistent interface · Should · 8 points
> As a user, I want a consistent interface in light or dark mode on any screen size, so that the product is pleasant to use every day.
- Design tokens for colour, type and spacing; system-default theme with a toggle; every page follows one visual standard; both themes pass the contrast test.

US-86, App shell: user menu, logout, command palette · Must · 3 points
> As a user, I want to see who I am signed in as, log out, and jump anywhere with a command palette, so that navigation is fast.
- User menu with logout; page titles and breadcrumbs; the command palette opens any page; keyboard shortcuts that are ignored while typing.

US-87, Feedback and validation · Must · 3 points
> As a user, I want every action to give clear feedback and forms to tell me what is wrong before I submit, so that I never have to guess.
- Shared toasts; one formatter for every enum value; client-side validation blocks invalid submits; consistent empty, loading and error states; an undo toast for single deletes; unsaved-changes warning on forms.

US-88, Responsive tables, charts and privacy views · Must · 5 points
> As a user, I want tables, charts and the privacy views to be readable on my phone, so that I can use the app anywhere.
- Card rows on narrow screens; responsive charts with legends; the vector inspector works on a phone; checked at 390 px and 1440 px.

US-116, Sample data and guided tour · Could · 3 points
> As a new user, I want to load sample data and take a short guided tour, so that I understand the product before entering my own data.
- Sample data is marked, touches only my records and can be removed without orphans; the tour covers every area, can be skipped and replayed; per-page guides are available from a "How this page works" button.

US-117, Designed error and edge states · Should · 3 points
> As a user, I want errors, missing pages, offline state and expired sessions to show helpful screens, so that I always know what to do next.
- Full-page and inline states with a next action and retry; an offline banner; a clean session-expiry flow; both themes covered.

### EPIC 12: AI Assistance

AI runs on a local model by default, so user data does not leave the server. A hosted provider is optional and disclosed whenever it is active. Every prompt is built only from the requesting user's own data.

US-102, AI provider · Must · 5 points
> As an operator, I want AI to run on a local model by default, with an optional hosted provider behind one interface, so that privacy holds and providers can be swapped.
- One provider interface with local and hosted implementations; timeouts and a clear "AI unavailable" state; per-user rate limits; a usage log without prompt content.

US-108, Resume and job description match · Should · 5 points
> As a job seeker, I want to check my resume against a job description, so that I can tailor it.
- Gaps and tailoring suggestions per section, and a tailored cover letter draft; nothing changes until I accept it.

US-111, Interview prep pack · Should · 3 points
> As a job seeker, I want a one-page prep pack before each interview, so that I walk in prepared.
- One brief per upcoming interview, built only from my own data: company history, likely topics, recommended questions and stories.

US-112, Follow-up and negotiation drafts · Could · 3 points
> As a job seeker, I want follow-up and negotiation emails drafted for me, so that I reply promptly and well.
- Drafts only, never sent automatically; drafts use the application and offer details.

---

## 10. Product backlog

Priorities use MoSCoW. Phases refer to `docs/sprints.md`. US-69 (schema and migrations) is delivered by every database phase.

| ID | User story | Priority | Points | Epic | Phases |
|---|---|---|---:|---|---|
| US-01 | Register an account | Must | 5 | 1 | P04, P05, P08 |
| US-02 | Log in | Must | 3 | 1 | P05, P08 |
| US-03 | Log out | Must | 2 | 1 | P05, P08 |
| US-04 | Session persistence and expiry | Must | 3 | 1 | P05, P08 |
| US-05 | View and edit my profile | Must | 3 | 1 | P06 |
| US-06 | Change my password | Should | 3 | 1 | P67 |
| US-07 | Export all my data | Should | 3 | 1 | P67 |
| US-08 | Delete my account | Should | 5 | 1 | P67 |
| US-91 | Onboarding and starter plan | Should | 5 | 1 | P77 |
| US-92 | Password reset and email verification | Should | 5 | 1 | P69 |
| US-93 | Sign in with Google, GitHub or LinkedIn | Could | 5 | 1 | P79 |
| US-09 | Create an application | Must | 5 | 2 | P09, P11, P12, P75 |
| US-10 | List my applications | Must | 5 | 2 | P11, P12 |
| US-11 | View application detail | Must | 3 | 2 | P11, P12 |
| US-12 | Edit an application | Must | 3 | 2 | P11, P12 |
| US-13 | Delete an application | Must | 3 | 2 | P11, P12 |
| US-14 | Change status and record history | Must | 5 | 2 | P10, P11, P12 |
| US-15 | See an application timeline | Must | 3 | 2 | P11, P12 |
| US-16 | Search applications | Must | 3 | 2 | P16, P75 |
| US-17 | Filter and sort applications | Must | 5 | 2 | P16, P75 |
| US-18 | Days in stage and stall flag | Should | 5 | 2 | P37 |
| US-89 | Kanban board | Should | 5 | 2 | P74 |
| US-95 | Contacts and referrals | Should | 5 | 2 | P83, P84 |
| US-96 | Company pages | Should | 3 | 2 | P83, P85 |
| US-99 | Global search | Should | 5 | 2 | P88 |
| US-100 | Offer comparison | Should | 3 | 2 | P83, P86 |
| US-101 | Weekly goals and activity heatmap | Should | 3 | 2 | P83, P87 |
| US-20 | Create a resume record | Must | 3 | 3 | P17, P20, P22 |
| US-21 | Upload a resume file | Must | 5 | 3 | P18, P19, P20, P22 |
| US-22 | Download my resume file | Must | 3 | 3 | P20, P22 |
| US-23 | Structured resume content | Should | 5 | 3 | P89 |
| US-24 | Attach a resume to an application | Must | 3 | 3 | P21, P22 |
| US-25 | Clone a resume into a new version | Should | 3 | 3 | P21, P22 |
| US-27 | Schedule or record an interview | Must | 5 | 4 | P23, P24, P25 |
| US-28 | List and view interviews | Must | 3 | 4 | P24, P25 |
| US-29 | Record questions asked | Must | 5 | 4 | P24, P25 |
| US-30 | Post-interview debrief | Must | 5 | 4 | P24, P25 |
| US-31 | Edit and delete an interview | Must | 3 | 4 | P24 |
| US-34 | Derive tested topics from an interview | Must | 3 | 4 | P24, P31 |
| US-110 | STAR story bank | Should | 3 | 4 | P95 |
| US-118 | Save an interview question to the question bank | Should | 2 | 4 | P91 |
| US-35 | Browse the question bank | Must | 5 | 5 | P27, P28, P29 |
| US-36 | Add my own questions | Must | 3 | 5 | P28 |
| US-37 | Record a practice attempt | Must | 5 | 5 | P28 |
| US-38 | Topic taxonomy with prerequisites | Should | 5 | 5 | P27, P29 |
| US-39 | Topic mastery with decay | Should | 8 | 5 | P31 |
| US-40 | Create preparation tasks | Must | 3 | 5 | P32 |
| US-41 | Preparation progress view | Must | 5 | 5 | P32 |
| US-42 | Spaced review queue | Could | 5 | 5 | P98 |
| US-90 | Question bank browser and practice logging | Must | 5 | 5 | P29, P76 |
| US-107 | Job readiness score | Should | 5 | 5 | P92 |
| US-109 | Mock interview | Should | 5 | 5 | P94 |
| US-43 | Application funnel | Must | 5 | 6 | P34, P36 |
| US-44 | Conversion rates | Must | 5 | 6 | P34, P36 |
| US-45 | Activity over time | Must | 3 | 6 | P34, P36 |
| US-46 | Rejection stage distribution | Should | 3 | 6 | P35, P36 |
| US-47 | Topic performance | Should | 5 | 6 | P36 |
| US-48 | Stage dwell times | Should | 3 | 6 | P35, P36 |
| US-49 | Dashboard pipeline and action queue | Must | 8 | 6 | P38 |
| US-50 | Sample sizes shown across analytics | Must | 3 | 6 | P30, P35, P36 |
| US-80 | Export my applications and analytics | Should | 5 | 6 | P64, P65, P66 |
| US-51 | Weak-topic recommendations | Must | 8 | 7 | P39, P40 |
| US-52 | Root-cause prerequisite routing | Should | 5 | 7 | P41 |
| US-53 | Evidence tiers and explanations | Must | 5 | 7 | P42 |
| US-54 | Non-causal language | Must | 3 | 7 | P42 |
| US-55 | Act on a recommendation | Should | 3 | 7 | P43, P78 |
| US-56 | Stalled-application follow-ups | Should | 3 | 7 | P40 |
| US-57 | Opt in to federated learning | Must | 5 | 8 | P55 |
| US-58 | Local feature extraction | Must | 8 | 8 | P44, P45, P51, P52 |
| US-59 | Local model training | Must | 8 | 8 | P46 |
| US-60 | Clipping and noise before sending | Must | 5 | 8 | P47 |
| US-61 | Aggregation with a minimum cohort | Must | 8 | 8 | P48, P53 |
| US-62 | Global model distribution | Must | 5 | 8 | P50, P53 |
| US-63 | Privacy Ledger | Should | 8 | 8 | P56, P57, P59 |
| US-64 | Withdraw from federation | Must | 3 | 8 | P55 |
| US-65 | Federated Lift viewer | Should | 5 | 8 | P54, P58 |
| US-84 | Poisoning filter | Should | 3 | 8 | P49 |
| US-67 | Repository and toolchain bootstrap | Must | 5 | 9 | P01, P03, P15 |
| US-68 | Docker Compose development environment | Must | 5 | 9 | P02 |
| US-69 | Database schema and migrations | Must | 8 | 9 | P04, P09 and every database phase |
| US-70 | CI pipeline | Must | 5 | 9 | P13, P82 |
| US-71 | Application shell and navigation | Must | 5 | 9 | P07 |
| US-72 | Seed and demo data | Should | 3 | 9 | P26, P99 |
| US-73 | Security test suite | Must | 5 | 9 | P14, P103 |
| US-74 | Accessibility and responsive pass | Should | 5 | 9 | P104 |
| US-76 | Notify me when an application stalls | Should | 3 | 10 | P60, P61, P62, P63 |
| US-77 | Notify me before an interview | Should | 3 | 10 | P60, P61, P62, P63 |
| US-78 | Notify me about a missing debrief | Should | 2 | 10 | P60, P61, P62, P63 |
| US-79 | Notification bell and mark as read | Should | 3 | 10 | P60, P62, P63 |
| US-85 | Consistent interface | Should | 8 | 11 | P70, P80, P81 |
| US-86 | App shell: user menu, logout, command palette | Must | 3 | 11 | P71 |
| US-87 | Feedback and validation | Must | 3 | 11 | P72 |
| US-88 | Responsive tables, charts and privacy views | Must | 5 | 11 | P73 |
| US-116 | Sample data and guided tour | Could | 3 | 11 | P100 |
| US-117 | Designed error and edge states | Should | 3 | 11 | P101 |
| US-102 | AI provider | Must | 5 | 12 | P90 |
| US-108 | Resume and job description match | Should | 5 | 12 | P93 |
| US-111 | Interview prep pack | Should | 3 | 12 | P96 |
| US-112 | Follow-up and negotiation drafts | Could | 3 | 12 | P97 |

The core product is every Must story plus the Should stories that give Cohort its character: US-18 (stall detection), US-38 and US-39 (topic graph and mastery decay), US-52 (root-cause routing), US-63 (Privacy Ledger) and US-65 (Federated Lift).

Non-functional requirements trace to phases as follows:

| NFRs | Delivered or verified by |
|---|---|
| NFR-01, 02 | P05, P68 |
| NFR-03, 08, 09 | P82 |
| NFR-04, 05 | P14, P103 |
| NFR-06 | P19, P20 |
| NFR-07 | P05 |
| NFR-10 to 13 | P44 to P52, P55 |
| NFR-14 | P67 |
| NFR-15, 16 | P42, P90, P102 |
| NFR-26, 27 | Rollback script in every database phase; transactional tests in P103 |
| NFR-28 | P50 |
| NFR-30, 31 | P25, P77 |
| NFR-32 to 35 | P72, P73, P101 |
| NFR-37 | P13 |
| NFR-40 | P15 |
| NFR-41, 42 | P02 |
| NFR-44 to 48 | P70, P73, P104 |
| NFR-49 to 51 | Throughout (injected clock, storage, FL gateway and AI provider; seeded randomness; golden files) |
| NFR-53 | P62 |
| NFR-17 to 25, 29, 36, 38, 39, 43, 52 | Documented targets |

---

## 11. Requirement trade-offs

Some requirements pull against each other. This section lists each conflict and how the product settles it.

### T1: Privacy and rich analytics

Conflicting: FR-58 to FR-69 and FR-71 (rich analytics) against NFR-10, NFR-16, DR-18 and DR-21.
Why: Better analytics and better models want more data, finer data, and data pooled across users. Each of those raises exposure, and job-search data is career-sensitive.
Resolution: Locality comes first. Personal analytics use only the requesting user's own data, at full detail. Learning across users happens only through clipped, noised model parameters with a fixed numeric schema, never through pooled records. An allow-list test (NFR-10) keeps the feature schema minimal.
Effect: Personal analytics have little statistical power for new users, so every figure shows its sample size (FR-67) and early recommendations fall back to Tier D heuristics instead of false precision. The global model is weaker than a centralised one would be.

### T2: Centralised ML and federated privacy

Conflicting: The simplicity and accuracy of centralised training against FR-79 to FR-92, DR-21 and the course requirement to use FL.
Why: Training over one database is simpler, converges faster and scores better. FL adds a server, several client processes, orchestration, noise and non-IID convergence problems.
Resolution: Real FL with separate client data stores. Simulating FL over pooled data would not test the privacy property. The deployment is small (a small model, few clients, few rounds), and success is measured as architectural correctness and checkable privacy, not accuracy.
Effect: More engineering effort, concentrated in Sprint 4, reduced by the Flower spike in Sprint 3. Model quality is modest and reported as measured.

### T3: Feature breadth and deliverability

Conflicting: The full requirement set against a fixed academic deadline and student capacity.
Why: Building everything at once produces a broad, broken system, which is worse than a narrow, working one.
Resolution: Depth over breadth. MoSCoW priorities, a defined core product, an explicit out-of-scope list (section 12), sprints that each end with a working product, and a strict Definition of Done.
Effect: Bulk operations, recruiter feedback capture, an admin console and calendar or email sync are outside the product scope.

### T4: Personalisation and standardisation

Conflicting: FR-70 to FR-77 (personal recommendations) against FR-79 (fixed feature schema) and FR-59 (standard metrics).
Why: Personalisation wants each user's own signal; federated learning needs every client to share an identical schema; comparable metrics need fixed definitions.
Resolution: A shared schema with personal weighting. A versioned topic taxonomy and feature schema give the common ground. Personalisation comes from the user's own mastery, decay and history deciding which topics surface and in what order. The Federated Lift viewer (US-65) shows the boundary between the shared model and the personal ranking.
Effect: User-created questions are supported locally but only the fixed schema takes part in federated learning. The opt-in screen says so.

### T5: Security and usability

Conflicting: NFR-01 to NFR-09 (short tokens, strong passwords, rate limits, confirmations) against NFR-30 to NFR-35 (fast, low-friction use).
Why: Every security control adds a step. A 15-minute token done naively logs people out mid-form.
Resolution: Friction sits at the boundary and nowhere else. Silent refresh-token rotation (US-04) hides short access tokens. The password policy gives live feedback instead of a rejection after submit. Confirmation dialogs appear only for destructive actions and name the object. Rate limiting applies only to authentication endpoints. FL opt-in is one decision, not a prompt every round.
Effect: More frontend work (refresh interceptor, request queueing during refresh) for a much better experience.

### T6: Stated uncertainty and clear advice

Conflicting: FR-67, FR-74, DR-06, DR-08 and DR-14 (statistical caution) against G3 and NFR-30 (users want a clear answer).
Why: Heavily hedged output is useless, but confident output from three data points is misleading.
Resolution: Be decisive about the action and plain about the evidence. The headline is one concrete step ("Practise BFS and DFS this week"); the tier badge and "why am I seeing this?" carry the uncertainty. Below the minimum sample the app says "not enough data yet". Causal phrasing is blocked by a failing test (FR-74).
Effect: Every recommendation needs both an action template and an evidence explanation.

### T7: Model utility and privacy noise

Conflicting: FR-83 and FR-86 (a useful global model) against FR-81, FR-82 and DR-21 (clipping and noise).
Why: Noise strong enough for meaningful privacy can drown the signal, especially with few clients and small local datasets.
Resolution: Privacy parameters are fixed first and utility is measured against them. Defaults come from the literature (clipping norm S = 1.0, noise multiplier σ = 1.0, k = 3) and are not tuned to make accuracy look better.
Effect: Model performance is modest, and the privacy and utility trade-off is reported as measured.

---

## 12. Scope

### 12.1 In scope

- Authentication and account: registration, login, logout, refresh rotation, profile, onboarding, Argon2id, per-user isolation, password change and reset, email verification, Google, GitHub and LinkedIn sign-in, data export, account deletion.
- Applications and workflow: full CRUD, table and Kanban views, search, filters and sort, status changes with immutable history, timeline, days in stage, stall detection, company records and pages, a company directory with anonymous experience figures, contacts and referrals, offer comparison, weekly goals and activity heatmap, global search, CSV import (including LinkedIn's export), job posting paste and a save bookmarklet.
- Resumes: versioned records, structured section editor, PDF export, validated upload and secure download, application association, cloning.
- Interviews: interviews against applications, multiple rounds, question capture with topic and difficulty, a debrief under three minutes, derived topics, saving a question to the bank, calendar (.ics) export, STAR story bank, prep packs.
- Preparation: a question bank with real questions for all 48 topics, personal questions, practice attempts with confidence history, topic taxonomy with prerequisites, mastery with time decay, preparation tasks, progress and readiness views, spaced review, job readiness per application, a self-graded mock interview.
- Analytics: funnel from stages ever reached, five conversion rates with denominators, activity over time, rejection distribution separated from no response, dwell times, topic performance, sample sizes everywhere.
- Recommendations: rule-based generation, prerequisite root-cause routing, evidence tiers A to D, plain explanations, enforced non-causal phrasing, accept, snooze and dismiss, stall follow-ups.
- Federated learning: opt-in default off, 24-feature extraction with point-in-time correctness, local scikit-learn training, L2 clipping, Gaussian noise, Flower FedAvg with a minimum cohort of 3 and norm filtering, versioned global models, a circuit breaker with local-only fallback, the Privacy Ledger with inspectable vectors, withdrawal, the Federated Lift viewer, a demo round with synthetic clients, and an FL service with no user-data credentials, checked in CI.
- Notifications and reports: in-app notifications from domain events; PDF and CSV export with secured downloads.
- AI assistance: a local-model provider with an optional, disclosed hosted provider; resume and job description match with a cover letter draft; interview prep packs; follow-up, negotiation and referral drafts.
- Platform: Docker Compose, Flyway migrations, CI with build, tests, coverage gate, secret and SQL-interpolation scans and the FL credential check, security headers, the security test suite, an accessibility pass, seed and sample data, a guided tour and page guides, designed error states, privacy and AI disclosure pages, documentation and the concept poster.

### 12.2 Out of scope

The product does not include:

- Tag management and bulk status updates (FR-24).
- Capture of feedback relayed by recruiters (FR-44).
- An admin console or administrator role (FR-11).
- An email or push notification channel (the channel interface allows one; none is built).
- Two-way calendar or email sync.
- Job-board scraping, integrations or auto-apply, and a browser extension.
- An AI chat coach, AI parsing of job postings or debrief notes, embedding-based question picks, and AI-written weekly reviews.
- Resume performance comparison by outcome and an applications-needed simulator (see DR-14).
- Cohort benchmarks from the federated model, a privacy budget meter and user-selectable privacy postures.
- Multi-factor authentication.
- Sharing or collaboration between users of any kind.
- Native mobile apps (the web app is responsive instead).
- A production deployment profile with scheduled backups.
- Structured operations logging and observability.
- Load testing and a cross-browser test matrix.
- Secure aggregation, formal DP accounting, real-user federation at scale, and browser-based FL clients.

### 12.3 Summary

Cohort is a privacy-first job-search tracker that turns each interview into targeted, evidence-tiered preparation, helps with AI that runs on a local model and answers only from the user's own data, and learns which preparation patterns work across users through a federated learning deployment in which raw data never leaves the user's own partition, and every user can inspect exactly what did.
