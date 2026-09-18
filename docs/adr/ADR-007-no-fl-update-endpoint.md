# ADR-007: `POST /fl/update` is deliberately absent from the API

**Status:** Accepted
**Date:** Sprint 1 (design decision; implemented in Sprint 4)
**Source:** `PROJECT_PLAN.md` §18.9 (Federated learning & privacy endpoints)

## Context

The original brief's endpoint sketch implies a REST path for a federated client to submit its local model update. The naive implementation routes that update through the same Spring Boot API that holds every user's application, resume, interview, and note data — after all, that's where every other authenticated write goes.

That naive implementation quietly destroys the core privacy property this project exists to demonstrate. D1's whole claim is that the Flower server has *no credentials* capable of reading user content, verifiable by inspection (`docker compose stop`, dump the container filesystem, show there's no user data in it). If model updates — even clipped, noised ones — travel through the Java API first, then:

- The one process that indisputably holds full database credentials is now also the one process that sees every user's federated update, linked to their authenticated session, before it's forwarded onward.
- The "the FL server never had the data" claim becomes true only of the Flower server, not of the boundary as a whole — and an evaluator asking "could the API log the update alongside the user's identity?" gets an uncomfortable "technically, yes."

## Decision

Client model updates travel over **Flower's own gRPC channel**, directly from each Flower `NumPyClient` to the Flower server — never through `/api/v1/*`. There is no `POST /fl/update` endpoint, and there never will be one. The Java API's only involvement in the FL boundary is:

- exporting an already-sanitised, allow-listed 24-feature numeric snapshot into the FL service's own storage before a round starts (§22.2's feature schema, enforced by an allow-list test — P51), and
- reading back round metadata and the current global model (`GET /fl/status`, `GET /fl/model`, `GET /fl/rounds`) for display.

The actual gradient/weight traffic never enters a process that also holds credentials to `applications`, `resumes`, `interviews`, or `notes`.

## Consequences

- This is a considered deviation from the brief's literal endpoint list — worth being upfront about with an evaluator, which is exactly what this ADR is for.
- It requires the Flower client processes to be independently network-reachable to the Flower server (a gRPC port), which the `docker-compose` FL profile (P50) has to wire up explicitly, rather than everything hanging off the one API port.
- It makes the "no FL server credentials" claim (D1, FR-92) true of the *entire path* a client update travels, not just its final resting place — which is the point.
