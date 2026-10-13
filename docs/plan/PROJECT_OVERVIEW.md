# Cohort — Project Overview

**A plain-language guide to what we are building, why, and how.** Read this first. The other documents (`PROJECT_PLAN.md`, `ARCHITECTURE_REVISION_JAVA.md`, `BUILD_PHASES_JAVA.md`) go deeper on specific parts; this one explains the whole project end to end and is meant to be read once, fully, by every team member.

---

## 1. What is Cohort, in one paragraph

Cohort is a web application that helps students and job seekers manage their entire job-search campaign (every application, interview, contact, offer and resume version) in one place, and turns what they learn from each interview into targeted preparation instead of generic practice. Its standout feature is **Federated Learning**: the app learns from many users' preparation patterns at once *without collecting anyone's actual data on a server*. Each user's data is used for training privately, and only a small, deliberately blurred summary is shared, never the real data.

That is the whole pitch. Everything else in this document is detail behind that paragraph.

---

## 2. The problem, in plain terms

If you have ever job-hunted seriously, you have lived this:

- You apply to 40, 80, 150 places. A spreadsheet or a notes app tracks *some* of it, until it doesn't; applications go quiet, and you don't notice which ones stalled until it is too late to follow up.
- You have an interview. The most useful thing that came out of it (what was actually asked, what you struggled with) evaporates within a day or two because nobody writes it down in a structured way.
- You prepare for the next interview the same generic way everyone does, grinding a popular question list, instead of targeting the specific things that have been tripping *you* up.

Spreadsheets and generic trackers solve the first problem as a status board and ignore the other two. Cohort closes all three gaps in one system.

---

## 3. What the app does — every feature, explained

### 3.1 Accounts and authentication
You register with an email and password, or sign in with Google or GitHub. Passwords are never stored in readable form: they are hashed with Argon2id, a deliberately slow, memory-hungry algorithm, so even a stolen database cannot practically be cracked. Login gives you a short-lived access pass plus a longer-lived refresh pass that can be revoked, so logging out on a shared computer really ends the session. You can reset a forgotten password, verify your email, change your password, export all your data, and delete your account.

### 3.2 Application tracking
The core, daily-use part of the app. You record a company, a role and a status, and the app tracks it from `Applied` through `Offer` or `Rejected`, with every status change permanently logged, so you get a real timeline. You can work in a table or on a Kanban board (illegal moves are refused with a reason), search and filter, and the app flags applications that have gone quiet longer than your own typical wait for that stage.

### 3.3 Job-search workflow
Beyond single applications: contacts and referrals per company (with a nudge when a contact goes cold), a page per company that gathers every application, interview, question, contact and note, side-by-side offer comparison with deadline countdowns, weekly goals with an activity heatmap and streak, and one search box that finds anything you have entered.

### 3.4 Resume management
Keep multiple resume versions, edit their sections (experience, projects, skills), attach the right one to each application, export a clean PDF, and, once there is enough data, see which version is landing more interviews. Below a minimum sample size the app says so honestly instead of showing a misleading percentage.

### 3.5 Interview tracking and debrief
After an interview, a short guided flow (designed to take under 3 minutes) captures what was asked, how it went and what you would do differently. This is the single most valuable data source in the app; everything in preparation builds on it.

### 3.6 Interview preparation
A question bank of real, original interview problems across every topic, plus your own questions; practice logging with a confidence rating; and a **topic mastery score** that fades if you stop practising, the way real skill does. Topics have prerequisite relationships (shortest-path algorithms need graph traversal first), so when you struggle with something advanced the app points you at the root gap instead of the symptom. A daily spaced-review queue resurfaces topics as they fade, and each application shows how ready you are for that specific job.

### 3.7 Analytics
A funnel showing where your applications go (applied → screening → interview → final round → offer), conversion rates between stages, activity over time, and resume performance. Every number shows its own sample size and is flagged as low-confidence when it rests on too little data. A conversion rate from 2 data points looks exactly like one from 200 unless the app tells you otherwise, so it always does.

### 3.8 Recommendations
Ranked, personalised suggestions for what to work on next. Every recommendation shows *why* it was generated and how confident the app is, using a four-tier evidence system (from "backed by your own solid history" down to "a starting-point guess because you have no data yet"). The language is never causal: the app never claims something *caused* an outcome, only that a pattern was observed. An automated test fails the build if banned phrasing (like "this will get you the job") ever appears.

### 3.9 Federated learning and privacy
Covered in §5 below. Users also see cohort benchmarks learned from the shared model and a privacy budget meter showing how much privacy cost their participation has used.

### 3.10 Notifications
An in-app notification centre: a nudge when an application stalls, when an interview is coming up, or when a past interview has no debrief. The detection logic is separate from how you are notified, so further channels can be added without touching detection.

### 3.11 Report export
Export your applications and analytics summary as PDF or CSV, for a mentor or career advisor or an offline record. Exports reuse the exact in-app formulas, so the numbers never disagree.

### 3.12 AI assistance
An AI coach that answers questions from your own data, a job-posting parser that fills in a new application, debrief notes turned into tagged interview questions, resume and job-description matching, mock interviews that record graded practice, a STAR story bank, one-page interview prep packs, follow-up and negotiation email drafts (never sent automatically), and a weekly review. AI runs on a **local model by default**, so your data never leaves the server; a hosted provider is optional and clearly disclosed when active.

### 3.13 First run and help
Onboarding asks for your target role and date and builds a starter plan; you can load clearly marked sample data and take a short guided tour; errors, missing pages, offline state and expired sessions show clear, helpful screens; privacy, AI disclosure and help pages explain what the app does with your data.

---

## 4. What makes this different from "just another job tracker"

- **The Federated Learning is real, not simulated.** Training happens on isolated data, and the FL server has zero database credentials for any user's content, provably rather than by promise.
- **A Privacy Ledger** shows each user exactly what was ever sent on their behalf: not a privacy *policy*, a privacy *log* they can inspect.
- **Recommendations carry honest uncertainty**, with a linter that fails the build if the copy ever oversells a correlation as a cause.
- **Analytics never hide their own sample size.**
- **AI is grounded and private**: it answers from your own data on a local model.
- **A dashboard that tells you what to do**, not only a report of what already happened.

---

## 5. The Federated Learning idea, explained without jargon

Imagine five friends each trying to improve their fitness. They don't want to share their diaries, which are too personal, but as a group they would like to know whether eating breakfast seems to help.

Instead of sharing diaries, each friend privately looks at *their own* data and writes down one small conclusion: "for me, it seemed to help a bit." They share only that small number. Someone averages the five numbers, and the group has a shared insight without any diary leaving its owner's hands.

That is federated learning. In Cohort:

1. The server sends out the current shared model.
2. For every opted-in user, the model is improved slightly using *only that user's* data, which is never sent anywhere.
3. Before anything is sent back, two safety steps happen: the update is capped so no one person can dominate, and it is deliberately blurred with statistical noise so it cannot be reverse-engineered.
4. Only that capped, blurred summary is sent.
5. The server waits for a minimum group (fewer than 3 would not hide anyone), averages what it received, and the improved shared model goes back out to everyone.

This repeats in rounds, each making the shared model slightly better, with nobody's job-search history leaving their own data partition.

**What is learned:** whether a user is likely to feel confident (self-rated 4–5 out of 5) after an interview round, based on their preparation beforehand. Deliberately not "will you get the job"; that would be a bigger, more ethically fraught claim than the data supports, and the project is explicit about not making it.

---

## 6. Technology stack — what is used, and why

| Layer | Technology | Why |
|---|---|---|
| Frontend | React 19 + TypeScript + Vite + Tailwind CSS + shadcn/ui + TanStack Query + Recharts | Component-based UI for a data-heavy dashboard; TypeScript catches mismatches at build time; shadcn gives accessible components |
| Main backend | **Java 21 + Spring Boot 3** | Owns everything user-facing: auth, applications, resumes, interviews, prep, analytics, recommendations, notifications, reports, AI features. Spring Security ships Argon2id, JWT and OAuth2 sign-in support |
| Database | PostgreSQL 16 + Spring Data JPA/Hibernate | A relational domain (users own applications own interviews own questions); Postgres roles make the FL data-isolation guarantee real |
| Migrations | Flyway | Versioned, reversible database changes |
| Federated learning | **Python + Flower + scikit-learn**, the *only* Python part of the backend | Flower has no Java equivalent; the Python service is limited to the FL client and server |
| AI | Ollama (local model) by default; Google Gemini optional | Keeps user data on the server unless a hosted provider is explicitly enabled and disclosed |
| Resilience | Resilience4j (circuit breaker) | If the FL service is slow or down, recommendations fall back to local-only instead of the page breaking |
| Testing | JUnit 5, MockMvc, Testcontainers (Java) · pytest (Python) · React Testing Library + Vitest (frontend) | |
| DevOps | Docker Compose, GitHub Actions CI | One-command local start-up; automated build, test and coverage gate on every pull request |

**Why two languages:** almost the entire application is plain Java. The only part that has to be Python is the Flower client and server, because that library has no Java equivalent. Java for everything else is a team choice (tooling comfort, Spring Security's built-in features), not a technical requirement.

---

## 7. How it is built — architecture in plain terms

```
   Browser (React app)
          │
          ▼
   Spring Boot API  ──────►  Talks to Postgres directly for
   (Java, owns almost         everything except FL round data
   everything)
          │
          │  (only this one call: "start a round,
          │   give me a status") — protected by a
          │   circuit breaker so a slow/dead FL
          │   service never breaks the main app
          ▼
   fl-service (Python)  ──►  Its own small slice of Postgres,
   Flower client + server     with credentials that CANNOT
                               read applications, resumes,
                               interviews, or notes — only
                               3 bookkeeping tables
```

The Java side uses the usual layers (controllers handle HTTP, services hold business logic, repositories talk to the database), and a handful of **design patterns** are used deliberately, each solving a specific problem:

- **State** for application status: each status knows its own legal next steps, so an invalid transition is structurally impossible; the Kanban board relies on it.
- **Chain of Responsibility** for file-upload validation (extension → file type → file signature) and the recommendation pipeline (generate candidates → tier by evidence → score → rank).
- **Strategy** for swappable pieces: file storage, notification channel, evidence-tier scoring, mail sender, and the AI provider (local model or hosted).
- **Observer** for notifications: the code that detects "this application stalled" announces it, and whoever is listening reacts.
- **Template Method** for analytics: every metric follows the same "compute → check sample size → flag if too little data" skeleton, so a new metric cannot forget the sample-size rule.
- **Adapter** for the Java↔Python boundary: one class knows how to talk to the FL service.

Plain CRUD controllers and services stay plain; no pattern is forced onto code that is already simple. That restraint is a design decision.

---

## 8. Privacy and security, in one place

- Every database query is scoped to the logged-in user at the code level; it is not something a developer has to remember each time.
- Requesting someone else's data returns "not found", not "forbidden", so nobody can confirm another user's record exists.
- Uploads are checked three ways (extension, declared type, binary signature) before being accepted, so a disguised malicious file is rejected.
- Federated learning participation is opt-in and off by default.
- The Privacy Ledger lets any user see exactly what was ever sent to the FL server on their behalf.
- AI prompts are assembled only from the requesting user's own data, and run on a local model unless a hosted provider is enabled and disclosed.
- Every response carries security headers, CI scans for committed secrets and string-built SQL, and an automated suite checks every endpoint for 401 and cross-user 404 behaviour.

---

## 9. Scope

**In the build:** everything in §3 above.

**Out of scope:** multi-factor authentication, native mobile apps (the web app is fully responsive), a browser extension, job-board scraping or auto-apply, two-way calendar or email sync, a calendar view or .ics export, CSV import, tag management and bulk updates, recruiter feedback capture, email notifications, an admin console, sharing between users, a production deployment profile, and secure aggregation (a more advanced cryptographic privacy technique). `PROJECT_PLAN.md` §31.2 has the full list.

---

## 10. The nine sprints, at a glance

| Sprint | Focus |
|---|---|
| 1 | Foundation — accounts, application tracking, the dev environment everyone can run |
| 2 | Resumes and interview capture, including the debrief flow |
| 3 | Preparation and analytics — question bank and content, topic mastery, analytics, the dashboard |
| 4 | Recommendations and federated learning — the highest-risk, highest-payoff sprint |
| 5 | Notifications, reports and account self-service |
| 6 | Experience and core gaps — design system, board view, onboarding, social sign-in, security headers |
| 7 | Job-search workflow — contacts, company pages, offers, goals, global search, resume editor |
| 8 | AI and intelligence — AI assistance, prep intelligence, cohort benchmarks, privacy budget |
| 9 | Hardening and delivery — sample data and tour, error screens, privacy pages, security and accessibility verification |

110 phases across a 10-person team in three departments (Backend, Frontend, AI/ML); see `BUILD_PHASES_JAVA.md` for the team and every phase.

---

## 11. Where to look for more detail

| Question | Document |
|---|---|
| "What exactly does requirement FR-XX say?" | `PROJECT_PLAN.md` |
| "Which user stories are in which epic and sprint?" | `PROJECT_PLAN.md`, §9–§12 |
| "Why was this database table designed this way?" | `PROJECT_PLAN.md`, §17 |
| "What does the Java/Python split look like in code?" | `ARCHITECTURE_REVISION_JAVA.md` |
| "Which design pattern goes where, and why?" | `ARCHITECTURE_REVISION_JAVA.md`, §4 |
| "What is the exact build sequence, phase by phase, and who owns it?" | `BUILD_PHASES_JAVA.md` |

**One thing every team member should be able to do after reading this document:** explain, in your own words, what federated learning is doing in this project and why it is not "we pooled everyone's data and called it AI." If you can't yet, re-read §5.
