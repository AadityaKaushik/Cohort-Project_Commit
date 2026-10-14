# ADR-009: Uniform client weighting in FedAvg, not sample-count weighting

**Status:** Accepted
**Date:** Sprint 1 (design decision; implemented in Sprint 4)
**Source:** `PROJECT_PLAN.md` §22 (ML/FL design)

## Context

Standard FedAvg (McMahan et al.) weights each client's contribution to the aggregated model by the number of local training examples it holds — clients with more data get proportionally more influence. This is statistically efficient and is the default in most FedAvg reference implementations.

To weight by sample count, the aggregator needs to *know* each client's sample count. For this product, "sample count" means the number of practice attempts and interview records a user has logged — that is, their job-search activity volume. That number is itself sensitive: an unusually high or low count reveals how intense the user's job search is, in addition to what the clipped and noised update reveals. It would be a second side channel next to the one the clipping and noise pipeline protects.

## Decision

The Flower server aggregates with **uniform weighting** — every participating client's update counts equally, regardless of how much local data it was trained on. No client ever transmits its sample count, in the update payload or anywhere else.

## Consequences

- **Statistical cost.** Uniform weighting is less sample-efficient than count-weighted FedAvg; a client with 200 practice attempts influences the global model exactly as much as one with 5. Given this product's non-IID, generally small per-client datasets (DR-20), this cost is modest, and the design accepts it to remove a leak.
- **One fewer field to defend.** There is no "is sample count sensitive enough to also clip and noise" question to answer, because sample count is never sent. The threat model for this boundary is also simpler.
- Any future move to count-weighting would need its own explicit privacy analysis (e.g., transmitting only a noised or bucketed count) — it would not be a drop-in change.
