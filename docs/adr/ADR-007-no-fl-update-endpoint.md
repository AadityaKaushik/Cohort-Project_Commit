# ADR-007: `POST /fl/update` is not part of the API

Status: Accepted  
Date: Sprint 1 (design decision; implemented in Sprint 4)  
Source: `PROJECT_PLAN.md` §18.9 (Federated learning & privacy endpoints)

## Context

The first endpoint list included a REST path for a federated client to submit its local model update. The simple implementation routes that update through the same Spring Boot API that holds every user's application, resume, interview and note data, as every other authenticated write does.

That implementation breaks the main privacy property of the project. D1 states that the Flower server has no credentials capable of reading user content, which can be checked by stopping the container and inspecting its filesystem. If model updates, even clipped and noised ones, travel through the Java API first, then:

- The one process that holds full database credentials also sees every user's federated update, linked to their authenticated session, before it is forwarded.
- The claim that the FL server never had the data is true only of the Flower server, not of the boundary as a whole. The API could log an update alongside the user's identity.

## Decision

Client model updates travel over Flower's own gRPC channel, directly from each Flower `NumPyClient` to the Flower server, and never through `/api/v1/*`. There is no `POST /fl/update` endpoint. The Java API's only involvement in the FL boundary is:

- exporting an allow-listed 24-feature numeric snapshot into the FL service's own storage before a round starts (§22.2's feature schema, enforced by an allow-list test, P51), and
- reading back round metadata and the current global model (`GET /fl/status`, `GET /fl/model`, `GET /fl/rounds`) for display.

The gradient and weight traffic never enters a process that also holds credentials to `applications`, `resumes`, `interviews`, or `notes`.

## Consequences

- This departs from the first endpoint list on purpose, and this ADR records why.
- It requires the Flower client processes to be independently network-reachable to the Flower server (a gRPC port), which the `docker-compose` FL profile (P50) has to configure explicitly instead of routing everything through the API port.
- It makes the "no FL server credentials" claim (D1, FR-92) true for the whole path a client update travels, not only its destination.
