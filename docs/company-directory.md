# Company directory

Part of the company pages (US-96, P84). Reference data only: `company_profiles` and `company_role_patterns`, seeded by migrations V71 and V72. Read through `GET /api/v1/directory/companies` and `/{slug}`; a signed-in user is required, there is no per-user data.

## Sourcing rules

- A company or role pattern is added only when at least one public source describes it. The source URL goes into the row's `sources` and the check date into `last_checked`.
- Rounds, durations and what each round tests are recorded as the source states them. A field with no support is left empty, not guessed.
- Candidate accounts are written up as "reported" and say how old the account is. No quotes, names or review text are copied.
- Company facts (industry, locations, size band, summary) stay empty until a source for them has been read.
- No numeric ratings or difficulty scores. The API returns `community_rating: null` and `community_min_contributors: 5`; ratings are shown only once five candidates have shared.

## Pay

Stored per role pattern as a range with currency, unit, year and level. A figure is added only when a public source states it. If the source gives no year, `year` is null and the page says the year is not stated. When sources disagree the range is kept and noted as varying by source. With no figure the page shows "No sourced figure yet". Pages whose terms forbid reuse are linked, not copied.

## Interview experience summary

One short paragraph per role pattern, written from the cited sources in neutral wording. It covers what the sources say about difficulty, process length and preparation, and nothing that the sources do not support.

## Refresh

Re-check each source about every six months and before a new intake season. Update `last_checked` only after re-reading the source. Add changes in a new migration; do not edit V71 or V72 after release.
