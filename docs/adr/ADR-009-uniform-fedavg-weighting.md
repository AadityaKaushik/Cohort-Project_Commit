# ADR-009: Uniform client weighting in FedAvg, not sample-count weighting

**Status:** Accepted
**Date:** Sprint 1 (design decision; implemented in Sprint 4)
**Source:** `PROJECT_PLAN.md` §22 (ML/FL design)

## Context

Standard FedAvg (McMahan et al.) weights each client's contribution to the aggregated model by the number of local training examples it holds — clients with more data get proportionally more influence, which is the statistically efficient thing to do and is what most FedAvg reference implementations do by default.

To weight by sample count, the aggregator needs to *know* each client's sample count. For this product, "sample count" means the number of practice attempts and interview records a user has logged — i.e. their job-search activity volume. That number is itself sensitive: a user with an unusually high or low count is revealing something about the intensity of their job search, on top of whatever the (already clipped, noised) model update reveals. It would be a second, unnecessary side channel sitting right next to the one the clipping-and-noise pipeline was built specifically to protect.

## Decision

The Flower server aggregates with **uniform weighting** — every participating client's update counts equally, regardless of how much local data it was trained on. No client ever transmits its sample count, in the update payload or anywhere else.

## Consequences

- **Statistical cost.** Uniform weighting is less sample-efficient than count-weighted FedAvg; a client with 200 practice attempts influences the global model exactly as much as one with 5. Given this product's non-IID, generally small per-client datasets (DR-20), this cost is real but modest, and it's a trade the design makes deliberately in favour of removing a leak, not an oversight.
- **One fewer field to defend.** There is no "is sample count sensitive enough to also clip and noise" question to answer, because sample count is simply never sent. The threat model for this boundary gets simpler, not just more private.
- Any future move to count-weighting would need its own explicit privacy analysis (e.g., transmitting only a noised or bucketed count) — it is not a drop-in change.
