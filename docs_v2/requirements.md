# Requirements

## 1. Stakeholders

| Stakeholder | Interest | What they need from Cohort |
|---|---|---|
| Students and job seekers (main users) | Keep track of applications, prepare better, get offers | Quick data entry, works on phone, honest analytics, private data stays private |
| Project team | Build, test and present the project | Clear scope, clean code structure, CI, seed data |
| Course staff (IT314) | Review process and documentation | Scrum artifacts, requirements, visible contribution from each member |
| FL administrator (played by the team) | Run federated rounds safely | Minimum cohort, clipping and noise settings, round log, model versions |
| University | Rules on data and conduct | Student data handled responsibly, licences respected |

## 2. Goals

| ID | Goal | How we know |
|---|---|---|
| G1 | One place for the whole job search | You can see where every application stands from one screen |
| G2 | Capture interview details while they are fresh | A debrief takes under 3 minutes |
| G3 | Turn interviews into targeted preparation | Weak topics from interviews show up in recommendations and the review queue |
| G4 | Make search performance readable | Funnel, conversion rates and stall detection, each with its sample size |
| G5 | Learn across users without collecting their data | No raw records reach the FL server |
| G6 | Make privacy checkable | Users can see what was sent in the Privacy Ledger |

## 3. Requirement elicitation

We used two techniques.

**Survey.** An online form for students applying to internships and jobs. It asked how many places they apply to in a season, what they use to keep track, at which stage they usually drop off, which features they would actually use, and whether they would share anonymised model updates.

What came out of it:
- Most people track applications loosely and lose track of follow-ups, which is why stall flags and the action queue are on the dashboard.
- Very few write anything down after an interview. The debrief had to be short and mostly taps, not typing.
- People were open to a shared model only if they could see what is sent. That led to opt-in being off by default and the Privacy Ledger.

**Peer interviews.** One-to-one conversations of about 30 minutes with classmates who were in the middle of placements. We asked them to walk us through their actual spreadsheet or notes, what they do after an interview, and what they would never type into an app.

What came out of it:
- The columns people had added to their own sheets (referral, deadline, salary, work mode, recruiter) became fields on an application.
- Questions asked in interviews are seen as private. They are never shared or used as model features.
- People wanted to be told what to practise next, not just shown charts. This shaped the recommendations and the review queue.

## 4. Functional requirements

Priority: M = Must, S = Should, C = Could.

### Accounts

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | Register with email, password and display name | M |
| FR-02 | Reject duplicate emails without revealing that the account exists | M |
| FR-03 | Password of at least 10 characters, checked against common passwords | M |
| FR-04 | Hash passwords with Argon2id | M |
| FR-05 | Log in and get a short-lived access token and a refresh token | M |
| FR-06 | Log out and revoke the refresh token | M |
| FR-07 | View and edit profile | M |
| FR-08 | Change password | S |
| FR-09 | Delete account and all data | S |
| FR-10 | Export all data as JSON | S |
| FR-105 | Confirm a new account's email with a 6-digit code before password login | S |
| FR-106 | Reset a forgotten password with a 6-digit code | S |

### Applications

| ID | Requirement | Priority |
|---|---|---|
| FR-12 | Create an application with company, job title, status and date applied | M |
| FR-13 | Optional fields: job description, URL, location, work mode, salary, priority, source, referral, deadline, notes | M |
| FR-14 | Paginated, sortable list | M |
| FR-15 | Search by company and title | M |
| FR-16 | Combinable filters (status, priority, date range, work mode) | M |
| FR-17 | Detail page with interviews, resume and history | M |
| FR-18, 19 | Edit and delete an application | M |
| FR-20 | Change status, with every change saved in history | M |
| FR-21 | Only allow valid status changes (state machine) | S |
| FR-23 | One company record per user, reused across applications | M |
| FR-25 | Days in the current stage | M |
| FR-26 | Flag applications quiet for longer than the user's usual wait | S |

### Resumes

| ID | Requirement | Priority |
|---|---|---|
| FR-28 | Create resume versions | M |
| FR-29 | Structured sections (skills, experience, education, projects) | S |
| FR-30, 31 | Upload a PDF or DOCX up to 5 MB, checked by extension, type and file signature | M |
| FR-32 | Download only by the owner | M |
| FR-34 | Attach one resume to an application | M |
| FR-35 | Clone a resume into a new version | S |

### Interviews

| ID | Requirement | Priority |
|---|---|---|
| FR-38, 39 | Log interview rounds with date, round type, mode and interviewer | M |
| FR-40 | Several rounds per application | M |
| FR-41 | Record questions asked, with topic and difficulty | M |
| FR-42 | Save an interview question to the question bank | S |
| FR-43 | Debrief with self-rating, what went well and badly, and outcome | M |
| FR-45 | Debrief takes under 3 minutes | S |

### Preparation

| ID | Requirement | Priority |
|---|---|---|
| FR-48, 49 | Question bank with seeded and own questions, by category, topic and difficulty | M |
| FR-51 | Log practice attempts with confidence and time taken | M |
| FR-52 | Topic prerequisites | S |
| FR-53 | Mastery score per topic that decays over time | S |
| FR-54 | Preparation tasks | M |
| FR-56 | Progress per topic and category | M |
| FR-57 | Daily review queue | C |

### Analytics and recommendations

| ID | Requirement | Priority |
|---|---|---|
| FR-58 | Funnel from Applied to Offer | M |
| FR-59 | Stage-to-stage conversion rates | M |
| FR-60 | Activity over time | M |
| FR-62 | Where rejections happen | S |
| FR-66 | Median days per stage | S |
| FR-67 | Sample size next to every rate; under 5 marked low confidence | M |
| FR-70 | Ranked personal recommendations | M |
| FR-72 | Point to the weak prerequisite, not just the weak topic | S |
| FR-73 | Each recommendation has an evidence tier and a reason | M |
| FR-74 | No causal wording (checked by a test) | M |
| FR-75 | Accept, snooze or dismiss a recommendation | S |

### Federated learning

| ID | Requirement | Priority |
|---|---|---|
| FR-79 | Fixed numeric feature schema, no text or names | M |
| FR-80 | Train locally on each user's own data | M |
| FR-81, 82 | Clip each update and add Gaussian noise before it is sent | M |
| FR-83 | Average with FedAvg; refuse rounds with fewer than 3 people | M |
| FR-84 | Drop oversized (poisoned) updates | S |
| FR-85 | Version the model and log each round | M |
| FR-87, 88 | Opt in (off by default) and opt out at any time | M |
| FR-89 | Privacy Ledger of everything sent | S |
| FR-92 | FL server has no access to user data in the database | M |

### Notifications and reports

| ID | Requirement | Priority |
|---|---|---|
| FR-99, 100 | Notify on stalled applications, upcoming interviews and missing debriefs | S |
| FR-101 | Mark notifications as read | S |
| FR-102, 103 | Export applications and analytics as PDF or CSV | S |
| FR-104 | Exports downloadable only by the owner | M |

## 5. Non-functional requirements

| ID | Area | Requirement |
|---|---|---|
| NFR-01 | Security | Argon2id with at least 64 MiB memory and 3 iterations |
| NFR-02 | Security | Access tokens last 15 minutes, refresh tokens 7 days |
| NFR-03 | Security | Only parameterised SQL, checked in CI |
| NFR-04 | Security | Every endpoint except auth and health needs a token |
| NFR-05 | Security | Another user's data returns 404, never 403 |
| NFR-07 | Security | Login attempts are rate limited |
| NFR-08 | Security | Security headers (CSP, HSTS, nosniff, Referrer-Policy) on every response |
| NFR-09 | Security | No secrets in the repo, CI scans for them |
| NFR-10 | Privacy | Nothing but the 24 allowed numbers reaches the FL server |
| NFR-12 | Privacy | Federated learning is off by default |
| NFR-14 | Privacy | Deleting an account leaves no rows or files behind |
| NFR-17 | Performance | List and detail pages respond within 300 ms with 500 applications |
| NFR-20 | Performance | Lists are paginated, 25 rows per page |
| NFR-26 | Reliability | Writes are transactional |
| NFR-27 | Reliability | Migrations are versioned and have rollback scripts |
| NFR-28 | Reliability | A failed FL round never breaks the current model |
| NFR-30 | Usability | A new user can add their first application within a minute |
| NFR-31 | Usability | A debrief takes 3 minutes or less |
| NFR-35 | Usability | Errors say what happened and what to do next |
| NFR-37 | Maintainability | Backend line coverage of at least 70% |
| NFR-41 | Portability | The whole app starts with one `docker compose up` |
| NFR-44 | Accessibility | WCAG AA contrast in light and dark mode |
| NFR-45 | Accessibility | Everything works with a keyboard |
| NFR-47 | Accessibility | Status is never shown by colour alone |
| NFR-48 | Accessibility | Works on screens from phone width up |

## 6. User stories

### Accounts
- US-01: As a job seeker, I want to register with my email and a password, so that I have a private account.
- US-02, 03: As a job seeker, I want to log in and log out, so that only I can reach my data.
- US-05: As a job seeker, I want to edit my profile, so that defaults fit my situation.
- US-07, 08: As a job seeker, I want to export or delete all my data, so that I stay in control of it.
- US-91: As a new user, I want onboarding to ask my target role and date and give me a starter plan, so that I do not start on an empty dashboard.
- US-92: As a job seeker, I want to confirm my email with a code and reset a forgotten password the same way, so that only real addresses get accounts.
- US-93: As a job seeker, I want to sign in with Google, GitHub or LinkedIn, so that I do not need another password.

### Applications
- US-09: As a job seeker, I want to record an application, so that I stop losing track of where I applied.
- US-10, 16, 17: As a job seeker, I want to list, search and filter my applications, so that I can find any one quickly.
- US-14, 15: As a job seeker, I want every status change saved and shown as a timeline, so that I can see how an application progressed.
- US-18: As a job seeker, I want stalled applications flagged, so that I follow up in time.
- US-89: As a job seeker, I want a board view, so that I can move applications between stages at a glance.
- US-95, 96: As a job seeker, I want contacts and a page per company, so that everything about an employer is in one place.
- US-99: As a job seeker, I want one search box across everything, so that I can find any record.
- US-100: As a job seeker, I want to compare offers side by side, so that I decide well and on time.
- US-101: As a job seeker, I want weekly goals and an activity heatmap, so that I keep a steady pace.

### Resumes
- US-20, 21: As a job seeker, I want to keep resume versions with the actual file, so that I know what each employer received.
- US-23: As a job seeker, I want to edit resume sections, so that my resume has structure.
- US-24: As a job seeker, I want to attach a resume to an application, so that I know which version I sent.
- US-25: As a job seeker, I want to clone a resume, so that I can tailor it without starting over.

### Interviews
- US-27, 28: As a job seeker, I want to log interview rounds, so that they are tracked with the application.
- US-29: As a job seeker, I want to note the questions I was asked, so that I keep them before I forget.
- US-30: As a job seeker, I want a quick debrief after each round, so that I actually write something down.
- US-110: As a job seeker, I want a bank of STAR stories, so that behavioural answers are ready.
- US-118: As a job seeker, I want to save a question I was asked to my question bank, so that I can practise it.

### Preparation
- US-35, 36: As a job seeker, I want a question bank I can add to, so that I have something to practise from.
- US-37: As a job seeker, I want to log practice, so that my progress is measured.
- US-39: As a job seeker, I want a mastery score per topic that fades without practice, so that it reflects reality.
- US-40, 41: As a job seeker, I want prep tasks and a progress view, so that I know where I stand.
- US-42: As a job seeker, I want topics brought back as they fade, so that I keep what I learned.
- US-107: As a job seeker, I want a readiness score for each job, so that I prepare for the role in front of me.
- US-109: As a job seeker, I want a timed mock interview from my weakest topics, so that practice feels real.

### Analytics and recommendations
- US-43, 44: As a job seeker, I want a funnel and conversion rates, so that I can see where I lose applications.
- US-46: As a job seeker, I want to see where I get rejected, so that I work on the right skill.
- US-49: As a job seeker, I want the dashboard to tell me what to do today, so that the app is a tool and not just a report.
- US-50: As a job seeker, I want each number to show how much data it is based on, so that I do not over-read it.
- US-51, 52: As a job seeker, I want to be told which topics to work on, including the underlying gap, so that preparation is targeted.
- US-53, 54: As a job seeker, I want to know why each recommendation was made, without claims it cannot back up, so that I can judge it myself.
- US-55: As a job seeker, I want to accept, snooze or dismiss recommendations, so that the list stays useful.
- US-80: As a job seeker, I want to export my applications and analytics as PDF or CSV, so that I can share them or keep a copy.

### Federated learning
- US-57, 64: As a job seeker, I want to choose whether to join the shared model and leave any time, so that it is my decision.
- US-59, 60: As a job seeker, I want the model trained on my data locally and my update noised before it leaves, so that it cannot be traced back to me.
- US-61: As an FL administrator, I want rounds with fewer than 3 people refused, so that averaging hides individuals.
- US-63: As a job seeker, I want a log of everything sent from my account, so that I can check the privacy claim.
- US-65: As a job seeker, I want to compare recommendations with and without the shared model, so that I can see what it adds.
- US-84: As an FL administrator, I want oversized updates dropped, so that one bad client cannot steer the model.

### Notifications and AI
- US-76, 77, 78, 79: As a job seeker, I want notifications for stalled applications, upcoming interviews and missing debriefs, so that I do not have to keep checking.
- US-108: As a job seeker, I want to check my resume against a job description, so that I can tailor it.
- US-111: As a job seeker, I want a prep pack before each interview, so that I walk in prepared.
- US-112: As a job seeker, I want follow-up and negotiation emails drafted, so that I reply quickly. Drafts are never sent automatically.

## 7. Domain requirements

| ID | Fact about the domain | What it means for Cohort |
|---|---|---|
| DR-01 | Applications do not move in a straight line; rejection or withdrawal can happen at any stage | Status is a state machine, not a fixed pipeline |
| DR-02 | No reply is the most common outcome, and it never arrives as an event | "Gone quiet" is worked out from time in stage |
| DR-03 | You can apply to the same company more than once | Company is its own record |
| DR-04 | Interview stages and number of rounds differ between companies | A small general set of stages and round types |
| DR-05 | Weeks between stages is normal | Time in stage is compared with the user's own usual wait |
| DR-06 | Outcomes depend on things the app cannot see (referrals, hiring freezes, luck) | Recommendations never claim a cause |
| DR-07 | Interview details are remembered badly and fade quickly | The debrief is short and nothing is required that people cannot recall |
| DR-08 | Self-ratings are biased | The model predicts self-reported confidence, not job offers |
| DR-10 | Interview questions are often confidential | Question text is never shared or used as a model feature |
| DR-12 | People keep several resume versions | Versions with lineage |
| DR-13 | Resumes contain personal details | Never public, never used for the shared model |
| DR-15 | Topics depend on each other (graphs before shortest paths) | Topic prerequisites guide recommendations |
| DR-16 | Skills fade without practice | Mastery decays over time |
| DR-18 | Job-search data is sensitive | Privacy comes first, so people enter real data |
| DR-21 | Model updates can leak training data | Clipping and noise are required |
| DR-22 | A tiny group gives no anonymity | Rounds need at least 3 people |
| DR-23 | A client might send a crafted update | Oversized updates are dropped |
