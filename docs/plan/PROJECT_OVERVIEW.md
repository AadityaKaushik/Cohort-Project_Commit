# Cohort: Project Overview

A plain-language guide to what the project is, why it exists and how it is built. The other documents (`PROJECT_PLAN.md`, `ARCHITECTURE_REVISION_JAVA.md`, `BUILD_PHASES_JAVA.md`) cover specific parts in more depth. This one describes the whole project and is intended for every team member.

---

## 1. What Cohort is

Cohort is a web application that helps students and job seekers manage a job search (every application, interview, contact, offer and resume version) in one place, and turns what they learn from each interview into targeted preparation. Its main technical feature is Federated Learning: the app learns from many users' preparation patterns without collecting anyone's data on a server. Each user's data is used for training privately, and only a small, noised summary is shared.

This summary is expanded in the sections below.

---

## 2. The problem

A typical job search has three problems:

- Applications reach 40, 80 or 150 places. A spreadsheet or notes app tracks some of it, then applications go quiet and the stalled ones are noticed too late to follow up.
- After an interview, the most useful information (what was asked, what went badly) is forgotten within a day or two because it is not written down in a structured way.
- Preparation uses a generic, popular question list instead of targeting the specific topics that have caused problems.

Spreadsheets and generic trackers handle the first problem as a status board and ignore the other two. Cohort addresses all three in one system.

---

## 3. What the app does

### 3.1 Accounts and authentication
A user registers with an email and password, or signs in with Google or GitHub. Passwords are hashed with Argon2id, a slow, memory-hard algorithm, so a stolen database cannot practically be cracked. Login issues a short-lived access token and a longer-lived refresh token that can be revoked, so logging out on a shared computer ends the session. Users can reset a forgotten password, verify their email, change their password, export all their data and delete their account.

### 3.2 Application tracking
The core, daily-use part of the app. The user records a company, a role and a status, and the app tracks it from `Applied` through `Offer` or `Rejected`. Every status change is logged, which gives a timeline. Applications can be shown as a table or on a Kanban board (illegal moves are refused with a reason), searched and filtered. The app flags applications that have been quiet longer than the user's own typical wait for that stage.

### 3.3 Job-search workflow
Beyond single applications: contacts and referrals per company (with a reminder when a contact goes cold), a page per company that gathers every application, interview, question, contact and note, side-by-side offer comparison with deadline countdowns, weekly goals with an activity heatmap and streak, and one search box that finds anything the user has entered.

### 3.4 Resume management
Users keep multiple resume versions, edit their sections (experience, projects, skills), attach a version to each application, export a PDF and, once there is enough data, see which version is leading to more interviews. Below a minimum sample size the app says so instead of showing a misleading percentage.

### 3.5 Interview tracking and debrief
After an interview, a short guided flow (designed to take under 3 minutes) captures what was asked, how it went and what the user would do differently. Everything in preparation builds on this data.

### 3.6 Interview preparation
A question bank of original interview problems across every topic, plus the user's own questions; practice logging with a confidence rating; and a topic mastery score that decays when the user stops practising. Topics have prerequisite relationships (shortest-path algorithms need graph traversal first), so a struggle with an advanced topic points to the root gap. A daily spaced-review queue resurfaces topics as they fade, and each application shows how ready the user is for that job.

### 3.7 Analytics
A funnel showing where applications go (applied → screening → interview → final round → offer), conversion rates between stages, activity over time and resume performance. Every number shows its sample size and is flagged as low-confidence when it rests on too little data.

### 3.8 Recommendations
Ranked, personalised suggestions for what to work on next. Every recommendation shows why it was generated and how confident the app is, using a four-tier evidence system (from "backed by your own solid history" down to "a starting-point guess because you have no data yet"). The wording is never causal: the app does not claim that something caused an outcome, only that a pattern was observed. An automated test fails the build if banned phrasing (like "this will get you the job") appears.

### 3.9 Federated learning and privacy
Covered in §5 below. Users also see cohort benchmarks learned from the shared model and a privacy budget meter showing how much of the privacy budget their participation has used.

### 3.10 Notifications
An in-app notification centre: a notice when an application stalls, when an interview is coming up, or when a past interview has no debrief. The detection logic is separate from how the user is notified, so further channels can be added without changing detection.

### 3.11 Report export
Applications and an analytics summary can be exported as PDF or CSV, for a mentor, a career advisor or an offline record. Exports reuse the in-app formulas, so the numbers match.

### 3.12 AI assistance
An AI coach that answers questions from the user's own data, a job-posting parser that fills in a new application, debrief notes turned into tagged interview questions, resume and job-description matching, mock interviews that record graded practice, a STAR story bank, one-page interview prep packs, follow-up and negotiation email drafts (never sent automatically), and a weekly review. AI runs on a local model by default, so data stays on the server; a hosted provider is optional and disclosed when active.

### 3.13 First run and help
Onboarding asks for the user's target role and date and builds a starter plan. Users can load sample data (clearly marked) and take a short guided tour. Error, missing-page, offline and expired-session states have their own screens. Privacy, AI disclosure and help pages explain how the app uses data.

---

## 4. Main design decisions

- Federated Learning runs on isolated data. The FL server has no database credentials for any user's content.
- Privacy Ledger. Each user can inspect exactly what was sent on their behalf.
- Recommendations carry their uncertainty. A test fails the build if the wording presents a correlation as a cause.
- Analytics show their sample size.
- AI is grounded and private. It answers from the user's own data on a local model.
- The dashboard suggests next actions as well as reporting past activity.

---

## 5. Federated learning without jargon

Take five friends who each want to improve their fitness. They do not want to share their diaries, but as a group they would like to know whether eating breakfast seems to help.

Instead of sharing diaries, each friend looks at their own data and writes down one small conclusion: "for me, it seemed to help a bit." Only that number is shared. Someone averages the five numbers, and the group has a shared result without any diary leaving its owner.

That is federated learning. In Cohort:

1. The server sends out the current shared model.
2. For every opted-in user, the model is improved slightly using *only that user's* data, which is never sent anywhere.
3. Before anything is sent back, two safety steps happen: the update is clipped so no one person can dominate, and statistical noise is added so it cannot be reverse-engineered.
4. Only that capped, blurred summary is sent.
5. The server waits for a minimum group (fewer than 3 would not hide anyone), averages what it received, and the improved shared model goes back out to everyone.

This repeats in rounds, each making the shared model slightly better, with nobody's job-search history leaving their own data partition.

What is learned: whether a user is likely to feel confident (self-rated 4-5 out of 5) after an interview round, based on their preparation beforehand. The model does not predict whether the user will get the job, because the data does not support that claim.

---

## 6. Technology stack

| Layer | Technology | Why |
|---|---|---|
| Frontend | React 19 + TypeScript + Vite + Tailwind CSS + shadcn/ui + TanStack Query + Recharts | Component-based UI for a data-heavy dashboard; TypeScript catches mismatches at build time; shadcn gives accessible components |
| Main backend | Java 21 + Spring Boot 3 | Owns everything user-facing: auth, applications, resumes, interviews, prep, analytics, recommendations, notifications, reports, AI features. Spring Security ships Argon2id, JWT and OAuth2 sign-in support |
| Database | PostgreSQL 16 + Spring Data JPA/Hibernate | A relational domain (users own applications own interviews own questions); Postgres roles make the FL data-isolation guarantee real |
| Migrations | Flyway | Versioned, reversible database changes |
| Federated learning | Python + Flower + scikit-learn, the *only* Python part of the backend | Flower has no Java equivalent; the Python service is limited to the FL client and server |
| AI | Ollama (local model) by default; Google Gemini optional | Keeps user data on the server unless a hosted provider is explicitly enabled and disclosed |
| Resilience | Resilience4j (circuit breaker) | If the FL service is slow or down, recommendations fall back to local-only instead of the page breaking |
| Testing | JUnit 5, MockMvc, Testcontainers (Java) · pytest (Python) · React Testing Library + Vitest (frontend) | |
| DevOps | Docker Compose, GitHub Actions CI | One-command local start-up; automated build, test and coverage gate on every pull request |

Why two languages: almost the entire application is plain Java. The only part that has to be Python is the Flower client and server, because that library has no Java equivalent. Java for everything else is a team choice (tooling comfort, Spring Security's built-in features), not a technical requirement.

---

## 7. Architecture in plain terms

```
   Browser (React app)
          │
          ▼
   Spring Boot API  ──────►  Talks to Postgres directly for
   (Java, owns almost         everything except FL round data
   everything)
          │
          │  (only this one call: "start a round,
          │   give me a status"), protected by a
          │   circuit breaker so a slow/dead FL
          │   service never breaks the main app
          ▼
   fl-service (Python)  ──►  Its own small slice of Postgres,
   Flower client + server     with credentials that cannot
                               read applications, resumes,
                               interviews, or notes, only
                               3 bookkeeping tables
```

The Java side uses the usual layers (controllers handle HTTP, services hold business logic, repositories talk to the database), and a handful of design patterns are used deliberately, each solving a specific problem:

- State for application status: each status knows its own legal next steps, so an invalid transition is structurally impossible; the Kanban board relies on it.
- Chain of Responsibility for file-upload validation (extension → file type → file signature) and the recommendation pipeline (generate candidates → tier by evidence → score → rank).
- Strategy for swappable pieces: file storage, notification channel, evidence-tier scoring, mail sender, and the AI provider (local model or hosted).
- Observer for notifications: the code that detects "this application stalled" announces it, and whoever is listening reacts.
- Template Method for analytics: every metric follows the same "compute → check sample size → flag if too little data" skeleton, so a new metric cannot forget the sample-size rule.
- Adapter for the Java↔Python boundary: one class knows how to talk to the FL service.

Plain CRUD controllers and services stay plain; no pattern is forced onto code that is already simple. That restraint is a design decision.

---

## 8. Privacy and security

- Every database query is scoped to the logged-in user at the code level; it is not something a developer has to remember each time.
- Requesting someone else's data returns "not found", not "forbidden", so nobody can confirm another user's record exists.
- Uploads are checked three ways (extension, declared type, binary signature) before being accepted, so a disguised malicious file is rejected.
- Federated learning participation is opt-in and off by default.
- The Privacy Ledger lets any user see exactly what was ever sent to the FL server on their behalf.
- AI prompts are assembled only from the requesting user's own data, and run on a local model unless a hosted provider is enabled and disclosed.
- Every response carries security headers, CI scans for committed secrets and string-built SQL, and an automated suite checks every endpoint for 401 and cross-user 404 behaviour.

---

## 9. Scope

In the build: everything in §3 above.

Out of scope: multi-factor authentication, native mobile apps (the web app is fully responsive), a browser extension, job-board scraping or auto-apply, two-way calendar or email sync, a calendar view or .ics export, CSV import, tag management and bulk updates, recruiter feedback capture, email notifications, an admin console, sharing between users, a production deployment profile, and secure aggregation (a more advanced cryptographic privacy technique). `PROJECT_PLAN.md` §31.2 has the full list.

---

## 10. The nine sprints

| Sprint | Focus |
|---|---|
| 1 | Foundation: accounts, application tracking, the dev environment everyone can run |
| 2 | Resumes and interview capture, including the debrief flow |
| 3 | Preparation and analytics: question bank and content, topic mastery, analytics, the dashboard |
| 4 | Recommendations and federated learning (the highest-risk sprint) |
| 5 | Notifications, reports and account self-service |
| 6 | Experience and core gaps: design system, board view, onboarding, social sign-in, security headers |
| 7 | Job-search workflow: contacts, company pages, offers, goals, global search, resume editor |
| 8 | AI and intelligence: AI assistance, prep intelligence, cohort benchmarks, privacy budget |
| 9 | Hardening and delivery: sample data and tour, error screens, privacy pages, security and accessibility verification |

110 phases across a 10-person team in three departments (Backend, Frontend, AI/ML); see `BUILD_PHASES_JAVA.md` for the team and every phase.

---

## 11. Where to look for more detail

| Question | Document |
|---|---|
| "What exactly does requirement FR-XX say?" | `PROJECT_PLAN.md` |
| "Which user stories are in which epic and sprint?" | `PROJECT_PLAN.md`, §9-§12 |
| "Why was this database table designed this way?" | `PROJECT_PLAN.md`, §17 |
| "What does the Java/Python split look like in code?" | `ARCHITECTURE_REVISION_JAVA.md` |
| "Which design pattern goes where, and why?" | `ARCHITECTURE_REVISION_JAVA.md`, §4 |
| "What is the exact build sequence, phase by phase, and who owns it?" | `BUILD_PHASES_JAVA.md` |

Every team member should be able to explain what federated learning does in this project and why it is not pooled data under another name (see §5).
