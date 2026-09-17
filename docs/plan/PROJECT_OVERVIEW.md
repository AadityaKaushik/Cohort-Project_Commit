# Cohort — Project Overview

**A complete, plain-language guide to what we're building, why, and how.** Read this first — the other documents (`PROJECT_PLAN.md`, `ARCHITECTURE_REVISION_JAVA.md`, `EVALUATION_DEFENSE_PACK.md`) go deeper on specific parts, but this is the one document that explains the whole project end to end, meant to be read once, fully, by every team member — not just whoever ends up touching a given part of the code.

---

## 1. What is Cohort, in one paragraph

Cohort is a web application that helps students and job seekers manage their entire job-search campaign — every application, every interview, every resume version — in one place, and turns what they learn from each interview into targeted preparation instead of generic practice. Its standout feature is **Federated Learning**: the app gets smarter over time by learning from many users' preparation patterns at once, *without ever collecting anyone's actual data on a server*. Each user's device trains privately on their own history and shares only a small, deliberately blurred summary — never the real data.

That's the whole pitch. Everything else in this document is detail behind that one paragraph.

---

## 2. The problem, in plain terms

If you've ever job-hunted seriously, you've lived this:

- You apply to 40, 80, 150 places. A spreadsheet or a notes app tracks *some* of it, until it doesn't — applications go quiet, and you don't notice which ones stalled until it's too late to follow up.
- You have an interview. It goes well, or badly, or somewhere in between — and the single most useful thing that came out of it (what was actually asked, what you struggled with) evaporates within a day or two because nobody writes it down in a structured way.
- You prepare for the next interview the same generic way everyone does — grinding a popular question list — instead of targeting the specific things that have actually been tripping *you* up.

Existing tools (spreadsheets, generic trackers) solve the first problem as a status board and ignore the other two completely. Cohort is built to close all three gaps in one system.

---

## 3. What the app actually does — every feature, explained

### 3.1 Accounts & Authentication
You register with an email and password. Passwords are never stored in readable form — they're run through Argon2id, a modern password-hashing algorithm that's deliberately slow and memory-hungry so that even if a database were ever stolen, the passwords in it can't practically be cracked. Login gives you a short-lived access pass plus a longer-lived refresh pass that can be revoked (so logging out on a shared computer actually means something).

### 3.2 Application Tracking
The core, daily-use part of the app. You record a company, a role, and a status, and the app tracks it from `Applied` all the way through `Offer` or `Rejected` — with every status change permanently logged, so you get a real timeline, not just a current snapshot. You can search, filter (by status, priority, tag, date), and the app flags applications that have gone quiet longer than your own typical wait time for that stage — so you know who to follow up with today, not three weeks from now.

### 3.3 Resume Management
You can keep multiple versions of your resume (tailored per role, as most serious job-seekers actually do), attach the right one to each application, and — once you have enough applications on a given version — see whether one version is landing more interviews than another. That comparison is only shown once there's enough data to be meaningful; below a threshold, the app says so honestly instead of showing a misleading percentage.

### 3.4 Interview Tracking & Debrief
After an interview, a short guided flow (designed to take under 3 minutes, so you'll actually use it while the details are fresh) captures what was asked, how it went, what you'd do differently. This is the single most valuable data source in the whole app — it's what makes the next feature possible.

### 3.5 Interview Preparation
A question bank (both pre-loaded and your own), practice tracking with a confidence rating, and — the more interesting part — a **topic mastery score** that fades over time if you stop practising a subject, the same way real skill does. Topics have real prerequisite relationships (you can't be solid on shortest-path algorithms without graph traversal first), so when you're struggling with something advanced, the app can trace *why* — pointing you at the actual root gap instead of the surface symptom.

### 3.6 Analytics
A funnel showing where your applications actually go (applied → screening → interview → final round → offer), conversion rates between each stage, and — critically — every single number in this section shows its own sample size, and gets visually flagged as low-confidence if it's based on too little data. A conversion rate computed from 2 data points looks exactly like one computed from 200 unless the app tells you otherwise — so it always tells you.

### 3.7 Recommendations
Ranked, personalised suggestions for what to work on next — but every recommendation shows *why* it was generated (which topics, which evidence) and how confident the app actually is in it, using a four-tier evidence system (from "backed by your own solid history" down to "a starting-point guess because you have no data yet"). The language is deliberately never causal — the app will never claim something *caused* an outcome, only that a pattern was observed. This rule is enforced by an automated test that fails the build if banned phrasing (like "this will get you the job") ever appears.

### 3.8 Federated Learning & Privacy (the innovation)
Covered in full in §5 below — it's substantial enough to deserve its own section.

### 3.9 Notifications *(added feature, beyond the original brief)*
An in-app notification centre: get nudged when an application stalls, when an interview is coming up, or when you haven't logged a debrief for a past interview. Built so the detection logic (a spreadsheet check, essentially) is completely separate from how you're notified — today it's in-app only, but the system is built so adding email or push notifications later doesn't require touching the detection code at all.

### 3.10 Report Export *(added feature, beyond the original brief)*
Export your applications and analytics summary as a PDF or CSV — useful for sharing progress with a mentor, career advisor, or just keeping an offline record. Reuses the exact same analytics formulas as the in-app version, so the exported numbers are never a second, possibly-inconsistent calculation.

---

## 4. What makes this different from "just another job tracker"

Worth being explicit about, because this is what separates the project from the dozen similar-sounding submissions every cohort produces:

- **The Federated Learning is real, not simulated.** Each client genuinely trains on isolated data, and the central server has zero database credentials for any user's actual content — provably, not just by promise.
- **A Privacy Ledger** shows each user exactly what their device has ever sent — not a privacy *policy*, a privacy *log* they can inspect.
- **Recommendations carry honest uncertainty**, with a linter that fails the build if the copy ever oversells a correlation as a cause.
- **Analytics never hide their own sample size.**
- **A dashboard that tells you what to do**, not just a report of what already happened.

---

## 5. The Federated Learning idea, explained without jargon

Imagine five friends are each trying to improve their fitness. They don't want to share their actual diaries with each other — too personal. But they'd genuinely like to know, as a group, "does eating breakfast seem to help?"

So instead of sharing diaries, each friend privately looks at *their own* data and writes down one small, private conclusion: "for me, it seemed to help a bit." They share only that one small number — never the diary. Someone averages the five small numbers together, and now the group has a shared insight, without anyone's actual diary ever leaving their hands.

That's federated learning. In Cohort:

1. The server sends out the current shared model.
2. Every opted-in user's device improves it slightly using *only their own* data — never sent anywhere.
3. Before sending anything back, two safety steps happen: the update is capped so no one person can dominate, and it's deliberately blurred with statistical noise so it can't be reverse-engineered.
4. Only that capped, blurred summary is sent — never the real data.
5. The server waits for a minimum group of people (fewer than 3 wouldn't hide anyone), averages what it received, and the improved shared model goes back out to everyone.

This repeats in rounds, each one making the shared model slightly better, with nobody's actual job-search history ever leaving their own device.

**What's genuinely learned:** whether a user is likely to feel confident (self-rated 4–5 out of 5) after an interview round, based on their preparation pattern beforehand. Deliberately not "will you get the job" — that would be a much bigger, more ethically fraught claim than the data actually supports, and the project is explicit about not making it.

---

## 6. Technology stack — what's used, and why

| Layer | Technology | Why |
|---|---|---|
| Frontend | React + TypeScript + Tailwind CSS + shadcn/ui + Recharts | Component-based UI for a data-heavy dashboard; TypeScript catches mismatches at build time; shadcn gives accessible components out of the box |
| Main backend | **Java 21 + Spring Boot 3** | Owns everything user-facing — auth, applications, resumes, interviews, prep, analytics, recommendations, notifications, reports. Spring Security ships Argon2id and JWT handling essentially built-in. |
| Database | PostgreSQL 16 + Spring Data JPA/Hibernate | A genuinely relational domain (users own applications own interviews own questions); Postgres schemas and roles are what make the FL data-isolation guarantee real, not just claimed |
| Migrations | Flyway | Versioned, reversible database changes |
| Federated Learning | **Python + Flower + scikit-learn** — the *only* part of the backend still in Python | No Java equivalent to Flower exists; kept deliberately small in scope (just the FL client/server), not the whole backend |
| Resilience | Resilience4j (circuit breaker) | Protects the app if the Python FL service is ever slow or down — recommendations gracefully fall back to local-only instead of the page breaking |
| Testing | JUnit 5, MockMvc, Testcontainers (Java) · Pytest (Python) · React Testing Library (frontend) | |
| DevOps | Docker Compose, GitHub Actions CI | One-command local startup; automated lint/test/coverage gates on every pull request |

**Why two languages instead of one:** almost the entire application is plain Java — CRUD, business rules, analytics, recommendations. The *only* thing that has to be Python is the Flower client/server pair, because that specific library has no Java equivalent. Everything else being Java was a team choice (tooling comfort, Spring Security's built-in security features), not a technical requirement.

---

## 7. How it's actually built — architecture in plain terms

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
   fl-service (Python)  ──►  Its own tiny slice of Postgres,
   Flower client + server     with credentials that CANNOT
                               read applications, resumes,
                               interviews, or notes — only
                               3 bookkeeping tables
```

The Java side is organised in the usual layers — controllers handle HTTP, services hold business logic, repositories talk to the database — and a handful of **design patterns** are used deliberately, each solving a specific real problem rather than being added for show:

- **State pattern** for application status — each status (Applied, Interview, Offer, etc.) knows its own legal next-steps, so an invalid transition is structurally impossible rather than something a test has to catch.
- **Chain of Responsibility** for both file-upload validation (extension → file type → file signature, each step independent) and the recommendation pipeline (generate candidates → tier by evidence → score → rank).
- **Strategy** for swappable pieces — how files are stored (local disk now, cloud storage later, one class to change), which notification channel is used (in-app now, email later), how each evidence tier scores a recommendation.
- **Observer** for notifications — the code that detects "this application stalled" doesn't need to know who cares about that fact; it just announces it, and whoever's listening (today: the notification system) reacts.
- **Template Method** for analytics — every metric (funnel, conversion rate, dwell time) follows the exact same "compute → check sample size → flag if too little data" skeleton, so a new metric literally cannot forget the sample-size rule.
- **Adapter** for the Java↔Python boundary — one class knows how to talk to the FL service; if that contract ever changes, exactly one file changes.

Equally important: plain CRUD controllers and services stay plain — no pattern is forced onto code that's already simple and clear. That restraint is itself a design decision, not an oversight.

---

## 8. Privacy & security, in one place

- Every database query is scoped to the logged-in user automatically at the code level — it's not something a developer has to remember to add each time.
- Requesting someone else's data returns "not found," not "forbidden" — so an attacker can't even confirm another user's record exists.
- Resume files and other uploads are checked three ways (extension, declared type, and the actual file's binary signature) before being accepted, so a disguised malicious file gets rejected.
- Federated learning participation is opt-in and off by default — nobody's data contributes to the shared model unless they explicitly choose to.
- The Privacy Ledger lets any user see exactly what their device has ever sent to the FL server, not just trust that it was safe.

---

## 9. Scope — what's in, what's deliberately out (for now)

**In the build:** everything in §3 above.

**Deliberately not built (and why that's fine):** email/password-reset flows (no mail server set up for this project), a mobile app, browser extensions to auto-capture job listings, secure aggregation (a more advanced cryptographic privacy technique — a real future improvement, not something this project claims to have), and multi-factor authentication. None of these were forgotten — they're explicitly listed as future scope because building them now would either need infrastructure the team doesn't have yet, or would compete for time against the actual innovation (the FL system) without adding much to what the project needs to prove.

---

## 10. The five sprints, at a glance

| Sprint | Focus |
|---|---|
| 1 | Foundation — accounts, application tracking, the dev environment everyone can run |
| 2 | Resumes and interview capture, including the debrief flow |
| 3 | Preparation tracking, topic mastery, and analytics |
| 4 | Recommendations and the full Federated Learning system — the highest-risk, highest-payoff sprint |
| 5 | Notifications, report export, security hardening, and getting ready to demo |

335 total story points across a 10-person team, which comfortably fits the team's capacity even in the heaviest sprint (Sprint 4, at 90 points).

---

## 11. Where to look for more detail

This document is the map, not the whole territory. When you need more:

| Question | Document |
|---|---|
| "What exactly does requirement FR-XX say, word for word?" | `PROJECT_PLAN.md` |
| "Why was this database table designed this way?" | `PROJECT_PLAN.md`, §17 |
| "What does the Java/Python split actually look like in code terms?" | `ARCHITECTURE_REVISION_JAVA.md` |
| "Which design pattern goes where, and why?" | `ARCHITECTURE_REVISION_JAVA.md`, §4 |
| "What's the exact build sequence, phase by phase?" | `BUILD_PHASES_JAVA.md` |
| "What do I say if the professor challenges a specific decision?" | `EVALUATION_DEFENSE_PACK.md` |

**One thing every team member should be able to do after reading this document:** explain, in your own words, what federated learning is doing in this project and why it's not just "we pooled everyone's data and called it AI." If you can't yet, re-read §5 — that's the one idea the whole innovation rests on.
