# Cohort: Overview

A plain-language guide to what Cohort is, what it does and how it is built. The other documents go deeper: `requirements.md` (requirements and user stories), `architecture.md` (system design and design patterns), `recommendations-and-fl.md` (the recommendation engine and federated learning), `sprints.md` (team, sprints and phases), `testing.md` (how it is tested) and `design.md` (the interface).

## 1. What Cohort is

Cohort is a web application that helps students and job seekers run a job search in one place: every application, interview, contact, offer and resume version. It turns what they learn in each interview into targeted preparation. Its main technical feature is federated learning: the app learns from many users' preparation patterns without collecting anyone's data on a server. Each user's data is used for training privately, and only a small, noised summary is shared.

## 2. The problem

A typical job search has three problems:

- Applications reach 40, 80 or 150 places. A spreadsheet tracks some of it, then applications go quiet and the stalled ones are noticed too late to follow up.
- After an interview, the most useful information (what was asked, what went badly) is forgotten within a day or two because nobody writes it down in a structured way.
- Preparation follows a generic, popular question list instead of the specific topics that have caused problems.

Spreadsheets and generic trackers handle the first problem as a status board and ignore the other two. Cohort handles all three.

## 3. What the app does

### 3.1 Accounts
A user registers with an email and password, or signs in with Google, GitHub or LinkedIn. Passwords are hashed with Argon2id, a slow, memory-hard algorithm, so a stolen database cannot practically be cracked. Login issues a short-lived access token and a revocable refresh token. Users can reset a forgotten password, verify their email, change their password, export all their data and delete their account.

### 3.2 Application tracking
The everyday part of the app. The user records a company, a role and a status, and Cohort follows it from Saved through Applied, OA, Interview and Final round to Offer or Rejected. Every status change is logged, which gives a timeline. Applications show as a table or on a board where illegal moves are refused with a reason. They can be searched and filtered, imported from a CSV (including LinkedIn's own export), or saved from any job page with a bookmarklet. The app flags applications that have been quiet longer than the user's own usual wait for that stage.

### 3.3 Job-search workflow
Contacts and referrals per company, with a reminder when a contact goes cold. A page per company that gathers every application, interview, question, contact and note, plus a directory of general interview patterns for well-known companies. Side-by-side offer comparison with deadline countdowns. Weekly goals with an activity heatmap and streak. One search box that finds anything the user has entered.

### 3.4 Resumes
Users keep several resume versions, edit their sections (experience, projects, skills), attach a version to each application, clone a version to tailor it, and export a PDF.

### 3.5 Interviews and debriefs
Each interview round has a date, a type and the questions asked. After the round, a short guided debrief (under 3 minutes) captures the outcome, what was asked and what to do differently. Any question asked can be saved straight into the user's question bank. Upcoming interviews can be added to a calendar as .ics files.

### 3.6 Interview preparation
A question bank of original interview problems across 48 topics, plus the user's own questions. Practice is logged with a confidence rating, and each topic has a mastery score that fades when the user stops practising. Topics have prerequisites (shortest-path algorithms need graph traversal first), so a struggle with an advanced topic points to the root gap. A daily review queue brings topics back as they fade. A timed mock interview draws questions from the weakest topics and saves each self-graded answer as practice. A STAR story bank holds behavioural stories tagged by competency. Each application shows how ready the user is for that job.

### 3.7 Analytics
A funnel showing where applications go, conversion rates between stages, rejection points, time spent in each stage and activity over time. Every number shows its sample size and is flagged when it rests on too little data.

### 3.8 Recommendations
Ranked suggestions for what to work on next. Each one shows why it was generated and how strong the evidence is, using four evidence tiers (from "backed by your own solid history" down to "a starting point because you have no data yet"). The wording is never causal: the app reports patterns, not causes. An automated test fails the build if banned phrasing appears.

### 3.9 Federated learning and privacy
Explained in section 5. Participation is opt-in. A Privacy Ledger shows each user exactly what was sent on their behalf in every round.

### 3.10 Notifications and reports
In-app notifications when an application stalls, an interview is coming up, or a past interview has no debrief. Applications and an analytics summary export as PDF or CSV, using the same formulas as the app.

### 3.11 AI assistance
Resume and job description matching with a tailored cover letter draft, a one-page prep pack for each upcoming interview, and follow-up, referral and negotiation email drafts that are never sent automatically. AI runs on a local model by default, so data stays on the server. A hosted provider is optional and disclosed whenever it is active.

### 3.12 First run and help
Onboarding asks for the user's target role and date and builds a starter plan. Users can load clearly marked sample data and take a guided tour, and each main page has its own short guide. Error, missing-page, offline and expired-session states have their own screens. Privacy, AI disclosure and help pages explain how the app uses data.

## 4. Principles

- Federated learning runs on isolated data. The FL service has no database credentials for any user's content.
- Each user can inspect exactly what was sent on their behalf.
- Recommendations carry their uncertainty, and a test fails the build if wording presents a correlation as a cause.
- Every analytics number shows its sample size.
- AI answers only from the user's own data, on a local model by default.
- The dashboard suggests the next action as well as reporting what happened.

## 5. Federated learning without jargon

Take five friends who each want to get fitter. They do not want to share their diaries, but as a group they would like to know whether eating breakfast seems to help.

Instead of sharing diaries, each friend looks at their own data and writes down one small conclusion: "for me, it seemed to help a bit." Only that number is shared. Someone averages the five numbers, and the group has a shared result without any diary leaving its owner.

That is federated learning. In Cohort:

1. The server sends out the current shared model.
2. For every opted-in user, the model is improved slightly using only that user's data, which is never sent anywhere.
3. Before anything is sent back, the update is clipped so no one person can dominate, and noise is added so it cannot be reverse-engineered.
4. Only that capped, blurred summary is sent.
5. The server waits for a minimum group (fewer than 3 would not hide anyone), averages what it received, and the improved model goes back out to everyone.

This repeats in rounds, and nobody's job-search history leaves their own data partition.

What is learned: whether a user is likely to feel confident (self-rated 4 or 5 out of 5) after an interview round, based on their preparation beforehand. The model does not predict whether the user will get the job, because the data does not support that claim.

## 6. Technology

| Layer | Technology | Why |
|---|---|---|
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui, TanStack Query, Recharts | Component-based UI for a data-heavy app; TypeScript catches mismatches at build time |
| Main backend | Java 21, Spring Boot 3 | Owns everything user-facing. Spring Security ships Argon2id, JWT and OAuth2 sign-in support |
| Database | PostgreSQL 16, Spring Data JPA, Flyway | A relational domain; Postgres roles make the FL isolation real; versioned migrations |
| Federated learning | Python, Flower, scikit-learn | Flower has no Java equivalent, so this is the only Python service |
| AI | Ollama (local) by default, Google Gemini optional | User data stays on the server unless a hosted provider is enabled and disclosed |
| Resilience | Resilience4j circuit breaker | If the FL service is slow or down, recommendations fall back to local-only ranking |
| Testing | JUnit 5, MockMvc, Testcontainers, pytest, Vitest, React Testing Library | Unit, integration and UI tests in each part |
| DevOps | Docker Compose, GitHub Actions | One-command start-up; build, tests and coverage gate on every pull request |

## 7. Architecture in plain terms

```
   Browser (React app)
          |
          v
   Spring Boot API  ------->  Postgres: everything except
   (Java, owns almost         FL round data
   everything)
          |
          |  one narrow call ("start a round", "give me a status"),
          |  behind a circuit breaker so a slow or dead FL service
          |  never breaks the main app
          v
   fl-service (Python)  --->  Its own small schema in Postgres,
   Flower client + server     with credentials that cannot read
                              applications, resumes, interviews
                              or notes
```

The Java side uses the usual layers: controllers handle HTTP, services hold business logic, repositories talk to the database. A handful of design patterns are used where each solves a specific problem:

- State for application status: each status knows its legal next steps, so an invalid transition cannot happen. The board relies on it.
- Chain of Responsibility for upload validation (extension, then file type, then file signature) and for the recommendation generators.
- Strategy for swappable parts: file storage, notification channel, evidence-tier scoring, mail sender and AI provider.
- Decorator to add validation around file storage.
- Observer for notifications: the code that detects a stalled application announces it, and listeners react.
- Template Method for analytics: every metric follows the same compute, check sample size, flag skeleton, so no metric can skip the sample-size rule.
- Builder for report exports, Facade for the recommendation pipeline, Adapter for the Java to Python boundary, Circuit Breaker around it.

Plain CRUD code stays plain; no pattern is forced onto code that is already simple.

## 8. Privacy and security

- Every database query is scoped to the signed-in user in the repository layer, not left to each developer to remember.
- Asking for someone else's data returns "not found", not "forbidden", so nobody can confirm another user's record exists.
- Uploads are checked three ways (extension, declared type, binary signature), so a disguised file is rejected.
- Federated learning is opt-in and off by default, and the Privacy Ledger shows what was sent.
- AI prompts are built only from the requesting user's own data.
- Every response carries security headers, CI scans for committed secrets and string-built SQL, and an automated suite checks every endpoint for 401 and cross-user 404 behaviour.

## 9. Scope

Cohort includes everything in section 3. It does not include multi-factor authentication, native mobile apps (the web app is responsive), a browser extension, job-board scraping or auto-apply, two-way calendar or email sync, email notifications, an admin console, or sharing between users. `requirements.md` section 12 has the full list.

## 10. Sprints

| Sprint | Focus |
|---|---|
| 1 | Foundation: accounts, application tracking, the dev environment |
| 2 | Resumes and interview capture, including the debrief |
| 3 | Preparation and analytics: question bank, topic mastery, analytics, the dashboard |
| 4 | Recommendations and federated learning |
| 5 | Notifications, reports and account self-service |
| 6 | Experience: design system, board view, onboarding, social sign-in, security headers |
| 7 | Job-search workflow: contacts, company pages, offers, goals, search, resume editor |
| 8 | AI and interview preparation: AI provider, readiness, resume match, mock interview, story bank, prep packs, drafts, review queue |
| 9 | Hardening and delivery: sample data and tour, error screens, privacy pages, security and accessibility |

104 phases across a 10-person team in three departments (Backend, Frontend, AI/ML). `sprints.md` lists every phase.
