package com.cohort.api.dto;

import com.cohort.api.service.recommendation.RecommendationStatus;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        boolean federatedApplied,
        int rankDelta,
        Instant suppressedUntil,
        Instant handledAt) {
}
