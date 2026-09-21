package com.cohort.api.service.recommendation;

import java.time.Instant;

/**
 * One concrete observation behind a recommendation (US-51: "each cites the
 * observations behind it"), persisted 1:1 into {@code recommendation_evidence}
 * rows (V18) by {@code RecommendationFacade} (P42). {@code occurredAt} is
 * nullable -- a heuristic {@link RecommendationType#baseline} candidate has
 * no dated observation at all, by definition.
 */
public record EvidenceItem(String label, String detail, Instant occurredAt) {
}
