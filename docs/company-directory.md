# Company directory

Part of the company pages (US-96). Sourced reference data in `company_profiles` and `company_role_patterns`, seeded by migrations V71, V72 and V75 to V78, read through `GET /api/v1/directory/companies` and `/{slug}`. A signed-in user is required. Candidates can add anonymous experience contributions on top, described under Experience contributions.

## Sourcing rules

- A company or role pattern is added only when at least one public source describes it. The source URL goes into the row's `sources` and the check date into `last_checked`.
- Rounds, durations and what each round tests are recorded as the source states them. A field with no support is left empty, not guessed.
- Candidate accounts are written up as "reported" and say how old the account is. No quotes, names or review text are copied.
- Company facts (industry, locations, size band, summary) stay empty until a source for them has been read.
- Community ratings stay separate from the indicative difficulty level below. They come only from contributions and are shown only once five candidates have shared for that company and role family.

## Pay

Stored per role pattern as a range with currency, unit, year and level. A figure is added only when a public source states it. If the source gives no year, `year` is null and the page says the year is not stated. When sources disagree the range is kept and noted as varying by source. With no figure the page shows "No sourced figure yet". Pages whose terms forbid reuse are linked, not copied.

## Interview experience summary

One short paragraph per role pattern, written from the cited sources in neutral wording. It covers what the sources say about difficulty, process length and preparation, and nothing that the sources do not support.

## Difficulty level

`difficulty_level` (1 to 5) and `difficulty_basis` are stored per role pattern and are null when no source says anything about difficulty. The basis is one sentence naming the wording it came from. The level is indicative only and is shown as "Indicative, from reported descriptions".

| Source wording | Level |
|---|---|
| easy | 1 |
| easy to moderate, easy to medium | 2 |
| moderate, medium | 3 |
| moderate to hard, medium-high, medium-to-hard | 4 |
| hard, very hard | 5 |

Structural facts stated by a source can also be used, for example a single complex problem in a 3 to 4 hour test (4). The company card shows the software engineer pattern's level when it has one, otherwise the first level found.

## Role families

`role_family` is a stable slug. The page turns it into a label by replacing underscores with spaces and capitalising.

| Slug | Label |
|---|---|
| software_engineer | Software Engineer |
| data_scientist | Data Scientist |
| product_manager | Product Manager |
| consulting_analyst | Consulting Analyst |
| investment_banking_analyst | Investment Banking Analyst |

New slugs are added here when a sourced pattern is seeded. Non-software patterns leave `topic_slugs` empty, because the question bank has no topic for them.

## Experience contributions

A candidate can share one structured account per company, role family and year. Nothing is required: the form is offered on a profile and after a debrief is saved, and it has a consent line and a checkbox.

Fields: company (from the directory, or a free name), role family, level (fresher or experienced), year (within the last 10 years), the rounds gone through (online test, coding, technical, system design, case study, group discussion, HR, managerial) each with a difficulty from 1 to 5, overall difficulty and overall experience from 1 to 5, outcome (offer, rejected, withdrawn, pending) and one optional tip of up to 280 characters. The tip goes through the text-quality rules in `data-quality-rules.md`, and an email address or phone number in it is rejected, not just warned about.

A free company name that matches no profile creates a stub: a name, a slug and no sourced facts or patterns. It shows community figures only.

### Anonymity

- The `experience_contributions` table has no user column. Each row stores `contributor_hash`, an HMAC-SHA256 of the company id and user id, keyed with `cohort.contributions.salt` (env `CONTRIBUTION_SALT`).
- The hash differs per company, so rows from one user at two companies cannot be matched. Inside one company it is stable, which gives the one-per-user-per-company-per-role-per-year check (a unique index) and the distinct contributor count.
- `GET /mine` and `DELETE /{id}` recompute the hash from the signed-in user id. A row that belongs to someone else returns 404, the same as a missing one.
- Aggregates are computed on each profile read from the rows of that company. They only use the hash for counting.
- Individual submissions are never returned to other users. The only free text shown is the tip, and only inside the aggregate.

Anonymity is limited by group size. With the minimum of 5 contributors a tip could still be recognised by someone who knows who applied, so tips are short and shown without time or author. The salt must be kept secret in production; with the salt and the user ids the hashes can be recomputed.

### Display rules

- Nothing but the contributor count is returned for a company and role family below 5 distinct contributors. The profile says "Ratings appear when 5 or more candidates have shared (n so far)".
- At 5 or more: average experience (`community_rating`), average difficulty (`community_difficulty`), the share of submissions that include each round type with its average difficulty, the outcome split in percent, and up to 5 tips.
- Tips are an unordered list in alphabetical order, so the position carries no information about time or author.
- The profile response has `community_rating`, `community_difficulty`, `community_contributors` and `community_min_contributors` for the role family with the most contributors, and `community_summaries` with the same figures per role family.
- A contributor can withdraw an entry from the profile (`DELETE /api/v1/directory/contributions/{id}`). Figures are recomputed on the next read, so a group that drops below 5 is hidden again.

### Endpoints

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/directory/contributions` | 201, 409 on a repeat for the same company, role family and year |
| GET | `/api/v1/directory/contributions/mine` | The caller's own entries |
| DELETE | `/api/v1/directory/contributions/{id}` | 204, 404 for another user's entry |

## Refresh

Re-check each source about every six months and before a new intake season. Update `last_checked` only after re-reading the source. Add changes in a new migration; do not edit V71 or V72 after release.
