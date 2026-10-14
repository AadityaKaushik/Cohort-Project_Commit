package com.cohort.api.mapper;

import com.cohort.api.dto.EvidenceItemResponse;
import com.cohort.api.dto.RecommendationResponse;
import com.cohort.api.entity.Recommendation;
import com.cohort.api.entity.RecommendationEvidence;
import com.cohort.api.service.recommendation.EvidenceItem;
import com.cohort.api.service.recommendation.RecommendationFacade;
import com.cohort.api.service.recommendation.RecommendationStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecommendationMapper {
    public RecommendationResponse toResponse(Recommendation entity, List<EvidenceItem> evidence) {
        return toResponse(entity, evidence, false, 0);
    }

    public RecommendationResponse toResponse(Recommendation entity, List<EvidenceItem> evidence, boolean federatedApplied, int rankDelta) {
        return build(entity, evidence.stream().map(e -> new EvidenceItemResponse(e.label(), e.detail(), e.occurredAt())).toList(), federatedApplied, rankDelta);
    }

    public RecommendationResponse toResponseFromPersistedEvidence(Recommendation entity, List<RecommendationEvidence> evidence) {
        return build(entity, evidence.stream()
                .map(e -> new EvidenceItemResponse(e.getLabel(), e.getDetail(), e.getOccurredAt()))
                .toList(), false, 0);
    }

    private RecommendationResponse build(Recommendation entity, List<EvidenceItemResponse> evidence, boolean federatedApplied, int rankDelta) {
        return new RecommendationResponse(
                entity.getId(),
                entity.getType(),
                entity.getEvidenceTier(),
                entity.getTargetTopicId(),
                entity.getTargetApplicationId(),
                entity.getScore(),
                entity.getHeadline(),
                entity.getExplanation(),
                entity.getObservationCount(),
                evidence,
                entity.getStatus(),
                entity.getSnoozedUntil(),
                entity.getDismissedReason(),
                entity.getAcceptedTaskId(),
                entity.getGeneratedAt(),
                federatedApplied,
                rankDelta,
                suppressedUntil(entity),
                entity.getStatus() == RecommendationStatus.active ? null : entity.getUpdatedAt());
    }

    private Instant suppressedUntil(Recommendation entity) {
        Instant handledAt = entity.getUpdatedAt();
        return switch (entity.getStatus()) {
            case active -> null;
            case accepted -> handledAt == null ? null : handledAt.plus(RecommendationFacade.ACCEPT_SUPPRESSION_DAYS, ChronoUnit.DAYS);
            case snoozed -> entity.getSnoozedUntil();
            case dismissed -> handledAt == null ? null : handledAt.plus(RecommendationFacade.DISMISS_SUPPRESSION_DAYS, ChronoUnit.DAYS);
        };
    }
}
