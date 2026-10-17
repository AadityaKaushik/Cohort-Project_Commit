# Recommendations and federated learning

How Cohort scores topic mastery, turns a user's own data into explainable recommendations, and runs federated learning without the FL side ever seeing user data. The rest of the system is described in [`architecture.md`](architecture.md).

## 1. Topic mastery

Prep topics form a directed prerequisite graph (for example Arrays, then Two Pointers, then Sliding Window; Recursion, then Trees, then Graphs, then Shortest Path). The graph is kept acyclic, and a test checks this. Each user has a mastery score per topic between 0 and 1, computed by `MasteryEngine`.

Evidence comes from two sources: practice attempts (confidence 1 to 5) and interview questions (self-rating 1 to 5). Each rating is mapped to [0, 1] with `(rating - 1) / 4` and weighted by how recent it is:

```
w = exp(-ln(2) * days_ago / H)        H = 90 days for practice, 120 days for interviews

practice_component  = sum(w * (confidence - 1) / 4)  / sum(w)
interview_component = sum(w * (self_rating - 1) / 4) / sum(w)

raw = 0.4 * practice_component + 0.6 * interview_component
      (only one component is used when only one has evidence)
```

Interview evidence weighs more because it is scarcer and closer to the real thing. The blended score then decays with the time since the last evidence of any kind, so a topic untouched for months is not shown as mastered:

```
mastery = raw * exp(-ln(2) * days_since_last_evidence / H_difficulty)

H_difficulty = 60 days (hard), 90 (medium), 120 (easy)
```

With fewer than two observations the score is stored but shown as "insufficient data" instead of a number, so one bad answer does not brand a topic as a weakness. On the prep page each topic is placed on a readiness step: no data yet, needs work (below 0.4), developing (0.4 to 0.7) or strong (0.7 and above).

The function is pure and pinned by golden-file tests, so an accidental change to the formula fails the build.

Other prep features use the same scores:

- Spaced review queue. After each attempt on a question the next review is scheduled 1, 3, 7, 14 or 30 days later, moving up one step for each attempt in a row that was not missed and back to 1 day after a miss. The dashboard and prep page list what is due.
- Job readiness. Topics mentioned in an application's job description are weighted by how often they appear and combined with the user's mastery. The score is shown only when at least two of those topics have evidence.
- Self-graded mock interview. A timed set of questions drawn from the user's weakest topics, including topics with only a few attempts. The user grades each answer, and each grade is saved as a practice attempt, so it feeds mastery directly.
- Saving interview questions. A question asked in a real interview can be saved into the user's own question bank under a topic, so it can be practised and counted like any other question.

## 2. Recommendation engine

### 2.1 Approach

The engine is rules first, with a learned adjustment on top:

1. Each suggestion must explain itself. A rule fires on specific evidence rows, so the explanation is exact.
2. A new user has no history. Rules give something sensible on the first day and get better as evidence builds up.
3. The federated model only re-ranks what the rules produced. Its effect can be measured and switched on and off.

### 2.2 Pipeline

```
[1] Candidate generation   generator chain over the user's own data, each candidate with evidence rows
[2] Root-cause routing     a weak topic may be redirected to a weaker prerequisite
[3] Suppression            drop candidates the user recently accepted, snoozed or dismissed
[4] Evidence tier          A, B, C or D
[5] Base scoring           severity x recency x actionability x tier weight
[6] Federated re-rank      optional, bounded multiplier from the global model
[7] Caps                   at most 3 stalled-application items, 5 recommendations in total
[8] Rendering              wording from the template registry, checked by the phrasing linter
```

`RecommendationFacade` runs the whole pipeline behind one method, and `RecommendationCard` is assembled with a builder.

### 2.3 Generators

The generators form a Chain of Responsibility; each adds its candidates and passes the list on.

| Generator | Fires when | Evidence |
|---|---|---|
| `WeakTopicGenerator` | A topic has enough evidence and its mastery is below 0.4 | Low-rated interview questions and low-confidence practice attempts |
| `DecayGenerator` | Mastery has dropped below 0.5 through time decay while the raw score was 0.5 or more | Last practice date, current and raw score |
| `StalledApplicationGenerator` | An application has been in its stage longer than the stall threshold | The application, days in stage, the threshold and its basis |
| `BaselineGenerator` | The user has fewer than 3 interviews logged | None; a heuristic, and the wording says so |

Stall threshold: 1.5 times the user's own median time in that stage when there are at least 5 past transitions, otherwise a default (Saved 30 days, Applied 14, OA 10, Interview 10, Final Round 7, Offer 7).

### 2.4 Root-cause routing

```
routeToRootCause(topic t, depth d = 0):
    if d >= 2: return t
    p = the prerequisite of t with the lowest mastery
    if p exists and mastery(p) < mastery(t) - 0.15:
        return routeToRootCause(p, d + 1)
    return t
```

The 0.15 margin stops routing on noise, and the depth cap keeps the chain short enough to read. The recommendation shows the chain, for example "graph traversal is weaker than shortest path, so start there". These are rendered with the prerequisite-gap template.

### 2.5 Evidence tiers and scoring

| Tier | Source | Weight | Example wording |
|---|---|---|---|
| A | 5 or more of the user's own observations | 1.0 | "In 6 of your last 8 technical rounds, graph questions were marked low-confidence." |
| B | 2 to 4 of the user's own observations | 0.7 | "Early signal only (2 rounds): dynamic programming may be a gap." |
| C | Ranked with the federated model | 0.6 | Associations across the cohort, never guarantees |
| D | Heuristic, no data yet | 0.4 | "You have no interviews logged. Start with a broad baseline." |

```
score = severity * recency * actionability * tier_weight * federated_adjustment

severity      = 1 - mastery (or the equivalent for an application)
recency       = exp(-ln(2) * days_since_evidence / 45)
actionability = how specific the action is (a named topic scores higher than a broad one)
```

Each tier has its own scoring Strategy class.

Federated adjustment. When the user turns on "Local + Federated", the API fetches the current global model through the circuit breaker and gives each candidate a multiplier of `1 + mean(model parameters) * (severity - 0.5)`, clamped to [0.75, 1.25]. The shared model can reorder suggestions but never create or hide one, and the user's own evidence always dominates. The page shows how far each item moved. If the FL service is unreachable, the ranking stays local-only and the page still loads.

### 2.6 Wording rules

All recommendation text lives in one template registry. Each template has an imperative headline and an associative explanation. `CausalLanguageLinter` fails the build if any template contains a banned phrase: "guarantees", "will get you", "ensures", "because of this you were rejected", "caused", "proven to", "always leads to", "you failed because", "this caused", "because you", "led to". Further tests check that every template renders with its evidence fields and always has an explanation.

### 2.7 User actions

A recommendation can be accepted (this creates a linked preparation task), snoozed for 7 days or dismissed. Accepted items stay hidden for 14 days and dismissed ones for 30. Snoozed and dismissed items can be viewed and restored.

## 3. Federated learning

### 3.1 What is learned

The task is whether a user is likely to rate an interview round 4 or 5 out of 5 for confidence, given their preparation before it. The label is self-reported confidence, not a hiring outcome, and the interface says so. Hiring outcomes are too sparse and too confounded to learn from, and predicting them would invite exactly the causal claims the product avoids. Text and company-level learning are ruled out because they would need raw text or would identify companies and, through them, users.

### 3.2 Feature schema v1

Everything that leaves a user's data is a fixed vector of 24 bounded floats, defined once in `fl-service/app/common/schema.py`, mirrored in `FlFeatureSchema` on the Java side, published at `/api/v1/fl/schema` and shown on the opt-in screen.

| # | Feature | Definition | Range |
|---:|---|---|---|
| 1 | `mastery_mean` | Mean decayed mastery over topics with evidence | [0, 1] |
| 2 | `mastery_min` | Lowest mastery among tested topics | [0, 1] |
| 3 | `mastery_std` | Spread of mastery | [0, 1] |
| 4 | `practice_volume` | `log1p(attempts) / log1p(500)` | [0, 1] |
| 5 | `topic_breadth` | Distinct topics practised / taxonomy size | [0, 1] |
| 6 | `practice_recency` | `exp(-days_since_last_attempt / 30)` | [0, 1] |
| 7 | `practice_consistency` | Share of the last 8 weeks with at least one attempt | [0, 1] |
| 8 | `confidence_mean` | Mean confidence over the last 10 attempts | [0, 1] |
| 9 | `confidence_trend` | Slope of confidence over the last 20 attempts | [-1, 1] |
| 10 | `hard_ratio` | Share of attempts on hard questions | [0, 1] |
| 11-14 | `share_dsa`, `share_technical`, `share_behavioural`, `share_system_design` | Category mix of practice | [0, 1] |
| 15 | `task_completion_rate` | Completed / created preparation tasks | [0, 1] |
| 16 | `prep_lead_time` | `min(days of prep, 60) / 60` | [0, 1] |
| 17 | `interview_experience` | `log1p(prior rounds) / log1p(50)` | [0, 1] |
| 18 | `rounds_per_application` | Mean rounds per application / 6 | [0, 1] |
| 19 | `recent_self_rating` | Mean self-rating over the last 5 rounds | [0, 1] |
| 20 | `coverage_ratio` | Share of a round's topics practised beforehand | [0, 1] |
| 21 | `repeat_topic_ratio` | Share of a round's topics seen in earlier rounds | [0, 1] |
| 22 | `application_volume` | `log1p(applications) / log1p(300)` | [0, 1] |
| 23 | `resume_variants` | `min(resume versions, 5) / 5` | [0, 1] |
| 24 | `round_type_ordinal` | Round type from OA to final, normalised | [0, 1] |

Never included: free text, names, company names, job titles, dates, interviewer names, resume content, question text, salary or location. Counts are log-scaled or capped because a raw count such as 137 applications is close to a fingerprint. Extraction takes an `as_of` time and ignores every record after it, so later data never leaks into an earlier example. Tests check that the payload is exactly these 24 names in this order and that values stay inside their bounds.

### 3.3 Isolation

- Participation is opt-in and off by default. Opting out stops the user's features from being exported for the next round.
- The Java API is the only process that reads user content. Every 6 hours `FlFeatureExportJob` computes the 24 features for each opted-in user and sends them to the FL service, keyed by the user's random UUID. Nothing else is sent.
- The fl-service container has no database credentials. A CI job starts the stack and fails if any main-database credential is present in its environment. The database role `cohort_fl` exists with `USAGE` on the `fl` schema only, `SELECT` and `INSERT` on the two FL tables, and no rights on any user table; an integration test checks these grants.
- Round results and ledger entries are written by the Java API into the `fl` schema.
- Deleting an account also deletes that user's exported features in the FL service.

### 3.4 A round

Thirty minutes after each export, `PrivacyLedgerRoundJob` asks the FL service to run a round for the opted-in users. For each participant the service:

1. measures the norm of the exported vector,
2. clips it to an L2 norm of at most S = 1.0,
3. adds Gaussian noise `N(0, (sigma * S)^2 I)` with sigma = 1.0,
4. records the norms before and after clipping, the noise multiplier, an approximate epsilon and the exact noised vector.

If fewer than k = 3 clients take part, the round is refused and the reason is recorded: averaging over a smaller group would not hide anyone. The API saves the round and one Privacy Ledger entry per real participant.

### 3.5 Training and aggregation

The training and aggregation code lives in the same service:

- Local trainer: `SGDClassifier(loss="log_loss")`, a logistic regression. Its parameters are 24 coefficients and one intercept, a flat fixed-length array, which is what FedAvg averages. Training is deterministic under a fixed seed. A client with fewer than 5 examples sits the round out.
- Privacy: each client computes its update as the difference from the current global model, clips it to S and adds noise, the same functions as above.
- Strategy: `MinCohortFedAvg`, a Flower `Strategy`. It refuses a round below the minimum cohort, drops any update whose norm is above `S * tau` with tau = 3 (a clipped update can never be that large, so this catches tampered or poisoned updates), and averages the rest with equal weight per client. No client sends its sample count.
- Model registry: append-only versions. A refused round leaves the previous version active. `/fl/model` on the FL service returns the current version and its parameters; the service starts from a zero model as version 0. With a zero model the federated multiplier in section 2.5 is exactly 1.0, so rankings move only once a trained version is published.

### 3.6 Demo round with synthetic clients

A cohort of 3 is hard to reach with one or two real users, so `python -m app.demo.simulate` fills the cohort with synthetic clients (`synthetic-client-01`, ...). Their features come from a seeded generator that stays inside the schema bounds. They exist only in the FL service's memory, are never written to a user table and are removed when the command ends. Each demo round calls `POST /api/v1/dev/fl/round`, which exists only under the `dev` profile and runs the normal export and round. Clipping, noise, the minimum cohort and the poisoning filter are unchanged; the API drops ledger entries for synthetic clients. Running it with one client shows the refusal path.

### 3.7 Privacy Ledger

The Privacy page lists every round the user's client took part in: round number, time, feature schema version, the norm before and after clipping, the noise multiplier, the cohort size, the approximate epsilon and a list of what was not sent. Each entry expands to show the exact noised vector. Entries are append-only and visible only to their owner (404 for anyone else); account deletion removes them.

## 4. Threat model

Defended:

- A compromised FL service cannot read user data. It has no credentials and no data path into the main database.
- Network interception gets clipped, noised numbers, never records.
- One malicious client's influence is bounded by the clipping norm, and oversized updates are dropped by the poisoning filter.
- Small-group de-anonymisation is blocked by the minimum cohort rule.
- Sample-count leakage is avoided by uniform weighting.

Not defended:

- The FL service sees each client's individual noised vector. There is no secure aggregation, so a curious service could try inference on one client; the noise makes this hard, not impossible.
- Many fake clients working together (Sybil attack) could still bias the model. That needs identity and reputation checks.
- Epsilon is reported per round as an approximation, without a formal composition engine, and is labelled approximate in the interface.
- A compromised Java API exposes user data directly. Federated learning protects the cross-user learning channel, not the application itself; the controls in `architecture.md` section 11 cover that.

## 5. Evaluation

The behaviour above is pinned by automated tests:

| Property | Test |
|---|---|
| Mastery formula and decay | Golden-file tests against fixed fixtures |
| Root-cause routing | Unit tests including the depth cap and margin |
| One test per generator, tier and template | Unit tests |
| No causal wording | `CausalLanguageLinter` test fails the build on a banned phrase |
| Federated adjustment stays in [0.75, 1.25] | Unit test on `FederatedLiftCalculator` |
| FL service down | Fault-injection test: the Recommendations endpoint returns 200 with local-only results |
| Feature schema | Exactly 24 named floats in order, inside their bounds; no record after `as_of` |
| Export payload | Matches the allow-list exactly, no free text |
| Clipping and noise | The sent vector differs from the raw one and the clipped norm is at most S |
| Minimum cohort | A 2-client round is refused and logged |
| Poisoning filter | A crafted over-norm update is dropped |
| Model registry | A refused round leaves the previous model servable |
| Determinism | Local training gives identical results under a fixed seed |
| Credential isolation | CI checks the running fl-service container and the `cohort_fl` grants |
| Ledger | A real noised vector is stored per participant per round, and only its owner can read it |
| Demo command | Synthetic clients stay in bounds, are removed afterwards and never reach the ledger |

Results produced with synthetic clients check that the mechanism works. They are not evidence about real job seekers.
