# fl-service

Python side of Cohort's federated-learning slice (Flower + scikit-learn). Per
`docs/adr/ADR-010-java-spring-boot-main-api.md`, this service is deliberately
small: Spring Boot owns the whole application, and this directory exists only
for the Flower client/server processes and their scikit-learn training code.

## Status

**Sprint 3:** `spike/` — a throwaway, 1-day risk-reduction spike (P32)
proving a two-client FedAvg round works with this project's scikit-learn
model shape. It is **not** on the production path: nothing in `api/` or
`docker-compose.yml` calls it, and none of its code was reused as-is.
Findings are written up in `docs/adr/ADR-011-fl-spike-findings.md`.

**Sprint 4, P43-P49 (current):** `app/` — the real client/server pipeline:

- `app/common/schema.py` — feature schema v1, the 24-feature vector (US-58).
- `app/common/parameters.py` — shared model parameter shape (25 floats:
  24 coefficients + 1 intercept).
- `app/client/feature_extractor.py` — point-in-time-correct local feature
  extraction (US-58).
- `app/client/local_trainer.py` — local `SGDClassifier` training,
  deterministic under a fixed seed (US-59).
- `app/client/privacy.py` — L2 clipping + Gaussian noise before anything is
  transmitted (US-60).
- `app/server/strategy.py`, `app/server/round_runner.py` — a real Flower
  `Strategy` (uniform-weight FedAvg over deltas) with minimum-cohort
  refusal (US-61) and a norm-based poisoning filter (US-84).
- `app/server/model_registry.py` — append-only model versioning; a refused
  round never overwrites the previous model (US-62).
- `app/control/api.py` — a thin FastAPI wrapper exposing `GET /fl/model`.

All of this is tested via direct in-process calls (no real Flower
client-server networking yet -- see `app/server/strategy.py`'s docstring
and ADR-011) and has no database dependency; the sanitised export job from
`api/`'s own database, the Java-side gateway/adapter, opt-in/out, and the
docker-compose `fl` profile are P50+ scope, not yet built.

## Local setup

```bash
cd fl-service
python3 -m venv .venv
source .venv/bin/activate
pip install -e ".[dev]"
```

## Running the tests

```bash
cd fl-service
source .venv/bin/activate
pytest             # the whole suite: app/ (Sprint 4) and spike/ (Sprint 3, kept for the record)
pytest app/        # just the production pipeline
```

## Running the spike

```bash
cd fl-service
source .venv/bin/activate
python -m spike.run        # prints per-round accuracy, exits non-zero on failure
python -m pytest spike/    # same assertions, as a proper test
```
