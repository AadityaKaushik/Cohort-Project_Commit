# ADR-011: Findings from the P32 Flower two-client FedAvg spike

**Status:** Accepted
**Date:** Sprint 3, P32
**Source:** `fl-service/spike/` (throwaway code, not on the production path); informs Sprint 4 (P43-P57)

## Context

`docs/plan/BUILD_PHASES_JAVA.md` schedules a 1-day throwaway spike before Sprint 4 commits real engineering time to the federated-learning slice: stand up Flower, run a synthetic two-client FedAvg round, and write down what worked and what didn't. The goal is to reduce risk, not to build a feature. Nothing here is imported by `api/` or wired into `docker-compose.yml`, and none of `fl-service/spike/`'s code is expected to survive into the real `fl-service/app/` Sprint 4 builds.

The spike lives at `fl-service/spike/` (`data.py`, `client.py`, `run.py`, `test_run.py`), uses `flwr==1.37.0` (the current release as of this spike; see pip environment below), `scikit-learn==1.9.1`, and `numpy==2.5.3`, and was run against Python 3.14.7 (this machine's only available interpreter).

## What worked

1. **`flwr`, `scikit-learn`, and `numpy` all installed and imported cleanly on Python 3.14.7.** Flower has lagged new CPython releases in the past, so no interpreter downgrade is needed for Sprint 4.
2. **A linear scikit-learn model (`SGDClassifier`) fits FedAvg.** Its entire trainable state is exactly two fixed-shape numpy arrays, `coef_` and `intercept_`, which is exactly what `flwr.common.ndarrays_to_parameters` / `parameters_to_ndarrays` and `flwr.server.strategy.aggregate.aggregate` expect (same-shaped ndarray lists, weighted-averaged by example count). Round-tripping real local model parameters through Flower's `Parameters`/`FitRes` types and back worked without changes.
3. **`flwr.client.NumPyClient` suits Sprint 4's real client.** Implementing `get_parameters`/`set_parameters`/`fit`/`evaluate` against a local shard was straightforward and matches P44/P45's planned `feature_extractor.py` + `local_trainer.py` split — the client class in the spike (`spike/client.py`) never touches another client's data, which is the whole point.
4. **`flwr.server.strategy.FedAvg.aggregate_fit` can be used directly.** Its source shows that it never calls back into the `ClientProxy` objects it is given and only uses them as dict keys, so the spike drives it with a placeholder `ClientProxy` and still exercises the strategy class Sprint 4 will use.
5. **The federated model outperformed the local models.** On a synthetic, deliberately non-IID two-client split (class-skewed shards, see `spike/data.py`), the FedAvg global model reached ~85-87% held-out accuracy across 5 rounds, versus ~42% and ~69% for each client's local-only model. This matches the premise of FedAvg, that pooling non-IID shards beats either shard alone.
6. **Determinism under a fixed seed held**, which P45's "deterministic under fixed seed" verify and the golden-file testing approach (NFR-51) depend on: `spike/test_run.py::test_run_is_deterministic_under_the_fixed_seed` runs the whole round twice and asserts bit-identical results.

## What didn't work / had to be routed around

1. **The legacy `fl.server.start_server` + `fl.client.start_numpy_client` client-server networking pattern shown in most Flower tutorials is no longer public API in 1.37.** `flwr.client` no longer exposes any `start_client`/`start_numpy_client` function at all (confirmed by inspecting `dir(flwr.client)`); `flwr.server.start_server` is still present but has no client-side counterpart to connect to it. Flower has moved the client-server path to a different architecture: standalone `SuperLink`/`SuperNode` processes plus `ClientApp`/`ServerApp` objects built around a `Context`/`RecordDict` message model (`flwr.clientapp.ClientApp`, `flwr.serverapp.ServerApp`). This is a larger and still-changing surface than a 1-day spike can cover.
2. **`flwr.simulation.run_simulation`, the in-process alternative to real networking, defaults to a Ray-backed execution backend, and Ray is not installed** (`pip show ray` confirms it's absent, and it is a heavy package with native dependencies that was not added for a throwaway spike). `flwr.simulation.start_simulation` (the older, simpler simulation entry point) is also Ray-based. Neither was exercised.
3. **Net effect: this spike validates the FedAvg *aggregation* mechanics end-to-end (client-side parameter production, serialisation, and server-side strategy aggregation), but not Flower's actual client-server *transport*.** That gap is listed as a Sprint 4 risk below.
4. **`SGDClassifier` needs a "priming" `partial_fit` call with an explicit `classes=` argument before `coef_`/`intercept_` exist at all**, which matters for P45 (`local_trainer.py`): the first local model on every client must declare the full label set, or a client whose shard is missing a class in round 1 will crash.

## Risks flagged for Sprint 4 as a direct result

- **P47 (Flower server, FedAvg strategy, min-cohort refusal) and P43-P46 (client-side pipeline) should budget time to choose and learn one of: (a) the new SuperLink/SuperNode + ClientApp/ServerApp architecture on the current `flwr` release, or (b) pinning `flwr` to an older release line (pre-1.16-ish) that still ships the simpler, well-tutorialised `start_server`/`start_numpy_client` API.** This decision should be recorded in its own ADR before P47 starts. The spike does not decide it, because it needs more than a day.
- **If option (a) is chosen, `docker-compose fl profile` (P50) needs to account for SuperLink/SuperNode being separate long-running processes**, not only a Flower server and N clients as originally scoped. This may add one or two containers to P50's scope.
- **The model family choice (fixed-shape numeric parameters) constrains US-58's feature extractor and US-59's local trainer**: whatever model Sprint 4 picks for the real recommendation re-ranker must expose its trainable state as fixed-shape numpy arrays the way `SGDClassifier.coef_`/`intercept_` do, or `aggregate()`'s shape-matched weighted averaging breaks. P45's design should state this requirement.
- **Non-IID behaviour was only spot-checked on one synthetic split.** Sprint 4's P47 min-cohort refusal and P48 poisoning filter should be tested against a range of skew severities, not only the one split built here.

## Decision

Proceed to Sprint 4 with Flower + scikit-learn as planned (no change to ADR-007/ADR-009/ADR-010's decisions), but treat the client-server transport choice (legacy API pin vs. new SuperLink/ClientApp/ServerApp architecture) as an **open decision to make at the start of P47**, which P43-P46 must not assume. This spike's code is not carried forward as-is.
