# API

Base URL: `http://localhost:8080/api/v1`. Requests and responses are JSON with snake_case fields.

Apart from the auth endpoints and `/health`, every request needs `Authorization: Bearer <access token>`. You can only ever see your own data: asking for someone else's record returns 404.

## Auth and account

### `/auth`

| Method | Path | What it does |
|---|---|---|
| POST | `/auth/register` | Create an account. A 6-digit code is emailed to confirm the address |
| POST | `/auth/email-verification/confirm` | Confirm the email with the code; returns tokens and signs you in |
| POST | `/auth/email-verification/request` | Send a new code (once per 60 seconds) |
| POST | `/auth/login` | Log in with email and password. Returns 403 `EMAIL_NOT_VERIFIED` until the email is confirmed |
| POST | `/auth/refresh` | Swap a refresh token for a new access and refresh token |
| POST | `/auth/logout` | Revoke the refresh token |
| POST | `/auth/password-reset/request` | Email a reset code |
| POST | `/auth/password-reset/confirm` | Set a new password with the code |

### `/auth/oauth`

| Method | Path | What it does |
|---|---|---|
| GET | `/auth/oauth/providers` | Which social sign-ins are set up (`google`, `github`, `linkedin`) |
| GET | `/oauth2/authorization/{provider}` | Starts Google, GitHub or LinkedIn sign-in (not under `/api/v1`) |
| GET | `/login/oauth2/code/{provider}` | Provider callback; redirects to the frontend with tokens (not under `/api/v1`) |

### `/users`

| Method | Path | What it does |
|---|---|---|
| GET | `/users/me` | Your profile |
| PATCH | `/users/me` | Update profile. Changing the email sends a code to the new address |
| POST | `/users/me/password` | Change password |
| POST | `/users/me/onboarding` | Save target role and date and create a starter plan |
| GET | `/users/me/export` | Download all your data as JSON |
| DELETE | `/users/me` | Delete your account and everything in it |

## Applications

| Method | Path | What it does |
|---|---|---|
| GET | `/applications` | List with paging, sort, search and filters |
| POST | `/applications` | Create an application |
| GET / PATCH / DELETE | `/applications/{id}` | Get, edit or delete one |
| POST | `/applications/{id}/status` | Change status (only legal moves are allowed) |
| GET | `/applications/{id}/history` | Status history |
| POST | `/applications/{id}/resume` | Attach a resume version |
| GET | `/applications/stalled` | Applications quiet for longer than usual |
| POST | `/applications/import` | CSV import (`?dryRun=true` previews it) |
| GET | `/applications/{id}/readiness` | Readiness score for the job |
| GET | `/applications/{id}/match` | Resume vs job description match (AI) |
| POST | `/applications/{id}/cover-letter-draft` | Cover letter draft (AI) |
| POST | `/applications/{id}/email-drafts` | Follow-up or negotiation email draft (AI) |
| POST | `/validate/text` | Check free text for junk before saving |

## Companies, contacts and directory

| Method | Path | What it does |
|---|---|---|
| GET | `/companies` | Your companies |
| GET / PATCH | `/companies/{id}` | Company page with its applications, interviews and contacts |
| POST | `/companies/{id}/notes` | Add a note |
| PUT / DELETE | `/companies/{id}/notes/{noteId}` | Edit or delete a note |
| GET / POST | `/contacts` | List or add contacts |
| GET / PATCH / DELETE | `/contacts/{id}` | One contact |
| POST | `/contacts/{id}/contacted` | Mark that you got in touch today |
| POST | `/contacts/{id}/message-drafts` | Referral or reconnect message draft |
| GET | `/directory/companies` | Company interview directory |
| GET | `/directory/companies/{slug}` | One company's general interview pattern |
| POST | `/directory/contributions` | Share your experience anonymously |
| GET | `/directory/contributions/mine` | Your contributions |
| DELETE | `/directory/contributions/{id}` | Withdraw a contribution |

## Offers and goals

| Method | Path | What it does |
|---|---|---|
| GET / POST | `/offers` | List or add offers |
| GET / PATCH / DELETE | `/offers/{id}` | One offer |
| PUT | `/offers/{id}/ratings` | Rate an offer on your criteria |
| GET | `/offers/decision` | Side-by-side comparison |
| PUT | `/offers/decision/weights` | How much each criterion matters to you |
| GET / PUT | `/goals` | Weekly goals |
| DELETE | `/goals/{id}` | Remove a goal |
| GET | `/goals/activity` | Activity heatmap data |

## Resumes

| Method | Path | What it does |
|---|---|---|
| GET / POST | `/resumes` | List or create resume versions |
| GET / PATCH / DELETE | `/resumes/{id}` | One version |
| POST | `/resumes/{id}/file` | Upload the PDF or DOCX (max 5 MB, checked by content) |
| GET | `/resumes/{id}/file` | Download it |
| POST | `/resumes/{id}/clone` | Copy into a new version |
| GET | `/resumes/{id}/export` | Export the sections as a PDF |
| GET / POST | `/resumes/{resumeId}/sections` | List or add sections |
| PUT / DELETE | `/resumes/{resumeId}/sections/{sectionId}` | Edit or delete a section |
| PUT | `/resumes/{resumeId}/sections/order` | Reorder sections |

## Interviews

| Method | Path | What it does |
|---|---|---|
| GET / POST | `/interviews` | List or add interview rounds |
| GET / PATCH / DELETE | `/interviews/{id}` | One round |
| GET / PUT | `/interviews/{id}/feedback` | The debrief |
| GET / POST | `/interviews/{id}/questions` | Questions asked in the round |
| PATCH / DELETE | `/interview-questions/{id}` | Edit or delete a question |
| POST | `/interview-questions/{id}/save-to-bank` | Save it to your question bank |
| GET | `/interviews/{id}/prep-pack` | One-page prep pack |
| POST | `/interviews/{id}/prep-pack/summary` | AI summary for the prep pack |
| GET | `/interviews/{id}/calendar.ics` | Add the round to a calendar |
| GET | `/calendar/upcoming.ics` | All upcoming rounds and deadlines |

## Preparation

| Method | Path | What it does |
|---|---|---|
| GET / POST | `/questions` | Question bank (seeded + your own) |
| GET / PATCH / DELETE | `/questions/{id}` | One question (seeded ones are read-only) |
| POST | `/questions/{id}/fork` | Copy a seeded question so you can edit it |
| GET / POST | `/questions/{id}/attempts` | Practice attempts |
| GET | `/topics` | The 48 topics |
| GET | `/topics/graph` | Topic prerequisites |
| GET | `/preparation/progress` | Progress and mastery per topic and category |
| GET / POST | `/preparation/tasks` | Prep tasks |
| PATCH / DELETE | `/preparation/tasks/{id}` | One task |
| GET | `/prep/review-queue` | Topics due for review today |
| GET | `/prep/mock-interview` | Questions for a mock interview (`?count=`) |
| GET / POST | `/stories` | STAR stories |
| GET / PATCH / DELETE | `/stories/{id}` | One story |

## Analytics, recommendations and reports

| Method | Path | What it does |
|---|---|---|
| GET | `/analytics/funnel` | Funnel counts per stage |
| GET | `/analytics/conversions` | Stage-to-stage conversion rates |
| GET | `/analytics/timeline` | Applications and interviews per week |
| GET | `/analytics/rejections` | Where rejections happen |
| GET | `/analytics/dwell-times` | Median days per stage |
| GET | `/recommendations` | Ranked recommendations (`?federated=true` adds the shared model) |
| POST | `/recommendations/{id}/accept` | Turn it into a prep task |
| POST | `/recommendations/{id}/snooze` | Hide for 7 days |
| POST | `/recommendations/{id}/dismiss` | Dismiss |
| POST | `/recommendations/{id}/restore` | Undo a snooze or dismiss |
| GET | `/recommendations/handled` | Snoozed and dismissed ones |
| GET / POST | `/reports` | List or create a PDF/CSV report |
| GET | `/reports/{id}/file` | Download it (owner only, expires after 7 days) |
| DELETE | `/reports/{id}` | Delete a report |

## Federated learning and privacy

| Method | Path | What it does |
|---|---|---|
| GET | `/fl/schema` | The 24 features that can be shared |
| GET | `/fl/consent` | Whether you are opted in |
| POST | `/fl/opt-in`, `/fl/opt-out` | Turn taking part on or off |
| GET | `/fl/privacy-ledger` | Every round you took part in |
| GET | `/fl/privacy-ledger/{id}` | One entry with the exact update that was sent |

Dev profile only (used by the Federated learning demo page):

| Method | Path | What it does |
|---|---|---|
| GET | `/dev/fl/status` | Current model, its weights, versions and recent rounds |
| POST | `/dev/fl/round` | Run a round now (optional `noise_multiplier`) |
| PUT | `/dev/fl/synthetic-clients` | Set how many synthetic people join |
| POST | `/dev/fl/reset` | Remove synthetic people and go back to model version 0 |

## Other

| Method | Path | What it does |
|---|---|---|
| GET | `/search?q=` | Search across applications, companies, contacts, questions and notes |
| GET | `/notifications` | Your notifications |
| GET | `/notifications/unread-count` | Count for the bell |
| POST | `/notifications/{id}/read`, `/notifications/read-all` | Mark as read |
| GET / POST / DELETE | `/sample-data` | Check, load or remove sample data |
| GET | `/ai/status` | Whether AI is available and which provider is active |
| GET | `/health` | Health check (not under `/api/v1`, no login needed) |

## FL service (internal)

The FL service runs on port 8000 and is only called by the backend, not the browser.

| Method | Path | What it does |
|---|---|---|
| PUT / GET / DELETE | `/fl/client-features/{client_id}` | A user's current features and rated rounds |
| POST | `/fl/round` | Train, clip, noise, average and publish |
| GET | `/fl/model`, `/fl/models`, `/fl/model/{version}` | Current model, all versions, one version |
| GET | `/fl/schema` | Feature list |
| PUT / DELETE | `/fl/demo/synthetic-clients` | Demo only (`FL_DEMO_ENABLED=true`) |
| POST | `/fl/demo/reset`, GET `/fl/demo/status` | Demo only |
