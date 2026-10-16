# Cohort: implementation overview

Cohort is a web application for running a job search in one place: applications, interviews, resume versions, contacts and offers, with interview debriefs feeding a personal preparation plan.

Status: Sprints 1 to 7 are built. Sprint 8 is built for P89, P94, P95, P98, P99 and P103, with the other ten phases deferred. Sprint 9 is built for P105 to P110. The phase-by-phase plan is in `docs/plan/BUILD_PHASES_JAVA.md`.

## 1. Purpose

A job search usually means dozens of applications tracked in a spreadsheet. Applications go quiet without anyone noticing, interview questions are forgotten, and preparation uses generic question lists.

Cohort addresses this with:

- an application tracker where every status change is kept as a timeline
- a short structured debrief after each interview
- topic mastery scores that decay when a topic is not practised
- analytics that show their sample size
- federated learning, so a shared model can learn from many users without their raw data leaving their own partition

It is aimed at students and early-career job seekers who apply to many roles.

## 2. Plan

Nine sprints, 110 phases. Each phase is one commit with its tests.

| Sprint | Theme | Phases | Status |
|---|---|---|---|
| 1 | Foundation | P01-P15 | Built |
| 2 | Resumes and interview capture | P16-P26 | Built |
| 3 | Preparation and analytics | P27-P38 | Built |
| 4 | Recommendations and federated learning | P39-P58 | Built |
| 5 | Notifications, reports and account | P59-P68 | Built |
| 6 | Experience and core gaps | P69-P81 | Built |
| 7 | Job-search workflow | P82-P88 | Built |
| 8 | AI and intelligence | P89-P104 | Built: P89, P94, P95, P98, P99, P103; 10 deferred |
| 9 | Hardening and delivery | P105-P110 | Built |

Working rules:

- one commit per phase
- tests are committed with the code
- every user-owned endpoint returns 404 for another user's data and has a test for it
- design patterns are used where the plan names them

## 3. What is built

- Accounts: register, login, refresh, logout, password change and reset, email verification, Google and GitHub sign-in, data export, account deletion.
- Applications: CRUD, status workflow with history, filters, list and board views, tags.
- Resumes: versions, file upload with type checks, cloning, linking to applications.
- Interviews: rounds, questions, feedback and a quick debrief.
- Preparation: topic graph, a question bank of six original questions per topic, practice attempts, mastery with decay, preparation tasks.
- Analytics: funnel, conversion rates, timeline, rejection distribution, dwell times, stall detection, dashboard action queue.
- Recommendations: generator chain, evidence tiers, accept, snooze and dismiss, federated lift view.
- Federated learning: feature schema, local training, clipping and noise, server with minimum cohort, poisoning filter, privacy ledger.
- Notifications and reports: in-app notifications from domain events, PDF and CSV exports.
- Job-search workflow: contacts and referrals with cold-contact nudges, company pages, offers with a deadline countdown, weekly goals with an activity heatmap, global search in the command palette, and a structured resume section editor with PDF export.
- Everyday interaction: keyboard shortcuts, undo on delete, unsaved-changes warning and kept drafts, remembered list filters, quick status menu, form defaults and company autocomplete, duplicate application, one-tap debrief, CSV import of applications.

## 4. Architecture

```
Browser (React + TypeScript)
   |  JSON over HTTP, bearer token
Spring Boot API (stateless)
   controller -> service -> repository -> entity
   |  JDBC                    |  HTTP, behind a circuit breaker
PostgreSQL 16 (Flyway)     fl-service (Flower + scikit-learn)
```

- Controllers handle HTTP and validation only. Services hold the rules. Repositories take the owner id on every finder.
- DTOs and mappers sit at the API edge, so entities are not exposed.
- The API keeps no session, so more than one instance can run.
- The fl-service has no credentials for user-content tables. CI checks this.

## 5. Technology

| Layer | Choice | Reason |
|---|---|---|
| API | Java 21, Spring Boot 3.5 | Typed, with security, validation and data access built in |
| Security | Spring Security, JWT, Argon2id | Argon2id is memory-hard, which makes cracking a stolen database slow |
| Database | PostgreSQL 16 | The data is relational; roles let the FL schema be isolated |
| Migrations | Flyway | Versioned schema changes; Hibernate only validates |
| Web | React 19, TypeScript, Vite | Typed components, fast builds |
| Styling | Tailwind CSS 4, shadcn/ui | Shared tokens and accessible components |
| Server state | TanStack Query | Caching and loading and error handling in one place |
| Charts | Recharts | Funnel and analytics charts |
| Federated learning | Python, Flower, scikit-learn, FastAPI | Flower has no Java equivalent |
| Resilience | Resilience4j | Circuit breaker around the FL service |
| Tests | JUnit 5, MockMvc, Testcontainers, Vitest, React Testing Library, pytest | Backend tests run against real PostgreSQL |
| CI | GitHub Actions | Build, tests, coverage gate, secret scan, check for string-built SQL |

Python is used only for federated learning.

## 6. Design patterns

| Pattern | Where |
|---|---|
| State | Application status transitions |
| Repository with a user-scoped base | All user-owned data access |
| DTO and mapper | API boundary |
| Specification | Application filters |
| Strategy | File storage, notification channels, recommendation tiers, mail sender |
| Decorator and Chain of Responsibility | Resume upload validation |
| Template Method | Analytics metrics with the sample-size gate |
| Chain of Responsibility | Recommendation generators |
| Observer | Domain events for stalled applications, interviews, debriefs and cold contacts |
| Facade and Builder | Recommendation pipeline, report assembly |
| Adapter and Circuit Breaker | Federated learning gateway |

## 7. Data model

| Table | Purpose |
|---|---|
| `users`, `refresh_tokens`, `auth_tokens`, `oauth_identities` | Accounts and sessions |
| `companies`, `applications`, `application_status_history`, `tags`, `application_tags` | Applications and their timeline |
| `resumes`, `resume_sections` | Resume versions and their sections |
| `interviews`, `interview_questions`, `interview_feedback` | Interview capture |
| `topics`, `topic_prerequisites`, `questions`, `practice_attempts`, `preparation_tasks` | Preparation |
| `recommendations`, `recommendation_evidence` | Recommendations |
| `notifications`, `report_exports` | Notifications and exports |
| `contacts`, `company_notes`, `offers`, `goals` | Job-search workflow |
| `fl.fl_rounds`, `fl.privacy_ledger_entries` | Federated learning |

An application moves through Saved, Applied, OA, Interview, Final round, then Offer or Rejected, then Accepted, Declined or Withdrawn. Terminal states have no further moves. Each move is validated by the State pattern and written to the history table.

## 8. Security

- Passwords are hashed with Argon2id.
- Access tokens last 15 minutes. Refresh tokens last 7 days, are stored as hashes and can be revoked.
- Login attempts are rate limited per email, and responses do not reveal whether an email exists.
- Another user's resource returns 404, never 403. Each endpoint has a test for it.
- Password reset and email verification tokens are single-use and expire.
- Responses carry CSP, nosniff, Referrer-Policy and HSTS headers.
- Uploads are checked by extension, MIME type and magic bytes.
- Hibernate only validates the schema; migrations change it.

## 9. Federated learning

Each opted-in user's client trains a model on that user's own features. Before anything is sent, the update is clipped and noise is added. The server refuses to aggregate below a minimum cohort size, filters updates with abnormal norms and averages the rest with FedAvg.

- Participation is opt-in and off by default.
- The privacy ledger lists every round a user took part in, including the transmitted vector.
- The model predicts how confident a user feels after an interview round. It does not predict hiring outcomes.
- If the FL service is down, recommendations fall back to local-only ranking.

## 10. Scaling

What works across several instances now: the stateless API, PostgreSQL as the single store, indexed lookups and pagination, and the separate FL service.

What would need work:

| Area | Now | At higher scale |
|---|---|---|
| Login rate limiter | In memory, per instance | Shared store such as Redis |
| Uploaded files | Local disk behind a storage interface | Object storage |
| Reports and FL rounds | Run inside the application | Background job queue |

## 11. Testing

- Backend tests run against PostgreSQL through Testcontainers, including migrations.
- Each user-owned endpoint has an isolation test that expects 404.
- Status transitions are tested pair by pair.
- Analytics metrics have golden-file tests.
- A linter test fails the build if a recommendation uses causal wording.
- The build fails when backend line coverage drops below 70 percent.
- Frontend pages are tested with React Testing Library; the FL service has pytest suites.

## 12. Running it

Requirements: Docker or Podman, Java 21, Node 20 or later.

```
docker compose up

cd api && mvn spring-boot:run
cd frontend && npm install && npm run dev

cd api && mvn test
cd frontend && npm test
cd fl-service && .venv/bin/pytest -q
```

Configuration comes from environment variables; see `.env.example`.

## 13. Limitations

- Ten Sprint 8 phases are deferred and not built.
- The axe-core check runs at the final test, not per commit.
- The login rate limiter and file storage are single-instance.
- No load testing has been done.

## 14. Where recent changes sit

| Change | Sprint | Phase |
|---|---|---|
| Typing text limited to the landing hero, auth headline and dashboard greeting | 6 | P79 |
| Colour token contrast test | 6 | P69, P80 |
| Keyboard shortcuts | 6 | P70 |
| Undo on delete, unsaved-changes warning, kept drafts | 6 | P71 |
| Remembered filters, quick status menu, relative dates, form defaults, duplicate | 6 | P74 |
| Empty states with a next step | 6 | P71, P80 |
| One-tap debrief | 2 | P25 |
| CSV import of applications | 6 | P76 |
| Contacts, company pages | 7 | P83, P84 |
| Offers and deadline countdown | 7 | P85 |
| Weekly goals and activity heatmap | 7 | P86 |
| Global search in the command palette | 7 | P87 |
| Resume section editor | 7 | P88 |
