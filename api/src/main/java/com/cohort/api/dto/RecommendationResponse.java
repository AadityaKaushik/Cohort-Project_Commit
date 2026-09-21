package com.cohort.api.dto;

import com.cohort.api.service.recommendation.RecommendationStatus;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mirrors {@code Recommendation} (US-51..56). {@code targetTopicId}/
 * {@code targetApplicationId} let the frontend link straight to the topic
 * grid or the application detail page; the human-readable name is already
 * baked into {@code headline}/{@code explanation} by the template registry
 * (P41), so no separate name field is needed here.
 */
public record RecommendationResponse(
        UUID id,
        RecommendationType type,
        EvidenceTier tier,
        UUID targetTopicId,
        UUID targetApplicationId,
        double score,
        String headline,
        String explanation,
        int observationCount,
        List<EvidenceItemResponse> evidence,
        RecommendationStatus status,
        Instant snoozedUntil,
        String dismissedReason,
        UUID acceptedTaskId,
        Instant generatedAt,
        // P53/US-65: whether a federated model signal was actually folded into `score` for this
        // response -- always false on the plain (non-federated, or fl-service-unavailable) path,
        // which is exactly the local-only fallback the circuit breaker (P53) degrades to.
        boolean federatedApplied) {
}
