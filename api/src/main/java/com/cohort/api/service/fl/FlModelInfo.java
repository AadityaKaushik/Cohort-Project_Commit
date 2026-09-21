package com.cohort.api.service.fl;

import java.time.Instant;
import java.util.List;

/**
 * Java's own view of fl-service's {@code GET /fl/model} response (US-62):
 * the current global model's version, which round produced it, how many
 * clients participated, and its flat parameter vector. {@code parameters}
 * is 25 floats (24 coefficients + 1 intercept, per {@code app/common/parameters.py}) —
 * used by the Federated Lift viewer (P57) to compute the FL-adjusted score
 * for a candidate recommendation.
 */
public record FlModelInfo(int version, int roundNumber, int participantCount, Instant createdAt, List<Double> parameters) {
}
