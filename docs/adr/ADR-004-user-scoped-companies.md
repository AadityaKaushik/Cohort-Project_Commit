# ADR-004: Companies are user-scoped, not a single global table

Status: Accepted  
Date: Sprint 1  
Source: `PROJECT_PLAN.md` §17.2 (companies table definition)

## Context

Every application belongs to exactly one company (DR-03), and companies are reused across a user's own applications (e.g. applying to the same employer for two different roles). The usual normalised design is a single global `companies` table shared by every user and deduplicated by name.

This is not suitable for this product, for two reasons:

1. Inference channel. A shared global table means company *rows* are created the first time any user applies to that company, and updated (or at least read) whenever another user applies there too. Row creation timestamps, and any future feature that surfaces "N users have applied here," reveal who is job-hunting where, which is the cross-user signal DR-18 and NFR-15 exist to prevent. One user's activity must not be inferable from another user's view of a shared row.
2. Deduplication complexity. A global directory needs matching and merge logic ("Google", "Google LLC", "Google Inc.") to stay clean. That is ongoing work with no clear owner in a small team.

## Decision

`companies` is scoped per user: every row carries `created_by_user_id`, and uniqueness (on `normalized_name`) is enforced per user, not globally. Two different users can each have their own "Acme Corp" row with no relationship between them. Company find-or-create (`ApplicationService.findOrCreateCompany`) looks up by `(userId, normalizedName)` only ever within the caller's own rows, and `CompanyRepository` extends the same `UserScopedRepository<T>` base as every other owned entity, isolation is inherited and needs no special handling.

## Consequences

- Row duplication: if 500 users all apply to "Google," there are 500 nearly-identical company rows instead of one. This is accepted: storage is cheap and cross-user inference is not.
- No cross-user company analytics ("companies with the most applicants across all users") are possible from this schema, because the data is not stored as a joinable relation. Such a feature would have to be built separately with explicit consent.
- No deduplication or merge logic is needed, which reduces Sprint 1 scope.
