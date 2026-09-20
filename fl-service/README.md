# fl-service

Python side of Cohort's federated-learning slice (Flower + scikit-learn). Per
`docs/adr/ADR-010-java-spring-boot-main-api.md`, this service is deliberately
small: Spring Boot owns the whole application, and this directory exists only
for the Flower client/server processes and their scikit-learn training code.

## Status

**Sprint 3 (current):** `spike/` only — a throwaway, 1-day risk-reduction
spike (P32) proving a two-client FedAvg round works with this project's
scikit-learn model shape, before Sprint 4 commits to a real implementation.
It is **not** on the production path: nothing in `api/` or
`docker-compose.yml` calls it, and none of its code is reused as-is in
Sprint 4. Findings are written up in
`docs/adr/ADR-011-fl-spike-findings.md`.

**Sprint 4:** the real `fl-service/app/` (feature schema, local trainer,
privacy/clipping, Flower server with min-cohort refusal, poisoning filter,
model versioning) lands per `docs/plan/BUILD_PHASES_JAVA.md` P43-P57.

## Local setup

```bash
cd fl-service
python3 -m venv .venv
source .venv/bin/activate
pip install -e .
```

## Running the spike

```bash
cd fl-service
source .venv/bin/activate
pip install pytest
python -m spike.run        # prints per-round accuracy, exits non-zero on failure
python -m pytest spike/    # same assertions, as a proper test
```
