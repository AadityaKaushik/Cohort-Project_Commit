# ADR-004: Companies are user-scoped, not a single global table

**Status:** Accepted
**Date:** Sprint 1
**Source:** `PROJECT_PLAN.md` §17.2 (companies table definition)

## Context

Every application belongs to exactly one company (DR-03), and companies are reused across a user's own applications (e.g. applying to the same employer for two different roles). The obvious normalized-database instinct is a single global `companies` table shared by every user, deduplicated by name, the way a real company directory would work.

That instinct is wrong for this product specifically, for two reasons:

1. **Inference channel.** A shared global table means company *rows* are created the first time any user applies to that company, and updated (or at least read) whenever another user applies there too. Row creation timestamps, and any future feature that surfaces "N users have applied here," leak information about who is job-hunting where — exactly the kind of cross-user signal DR-18 and NFR-15 exist to prevent. Job searching is career-sensitive; nothing about one user's activity should be inferable from another user's view of a shared row.
2. **Deduplication complexity.** A real global directory needs matching/merge logic ("Google", "Google LLC", "Google Inc.") to stay clean, which is real, ongoing work with no clear owner in a solo/small-team build, and gets it wrong more often than it gets it right at this scale.

## Decision

`companies` is scoped per user: every row carries `created_by_user_id`, and uniqueness (on `normalized_name`) is enforced **per user**, not globally. Two different users can each have their own "Acme Corp" row with no relationship between them. Company find-or-create (`ApplicationService.findOrCreateCompany`) looks up by `(userId, normalizedName)` only ever within the caller's own rows, and `CompanyRepository` extends the same `UserScopedRepository<T>` base as every other owned entity — isolation isn't a special case here, it's inherited automatically.

## Consequences

- Row duplication: if 500 users all apply to "Google," there are 500 nearly-identical company rows instead of one. This is deliberately accepted — storage is cheap, cross-user inference is not.
- No cross-user company analytics ("companies with the most applicants across all users") are possible from this schema, by design — that data literally doesn't exist as a joinable relation. If that kind of aggregate feature is ever wanted, it would have to be built as an explicitly-consented, separately-aggregated feature, not an incidental side effect of a shared table.
- No deduplication/merge logic was needed for Sprint 1, which was real, immediate scope savings.
