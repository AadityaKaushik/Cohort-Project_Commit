package com.cohort.api.mapper;

import com.cohort.api.dto.EvidenceItemResponse;
import com.cohort.api.dto.RecommendationResponse;
import com.cohort.api.entity.Recommendation;
import com.cohort.api.entity.RecommendationEvidence;
import com.cohort.api.service.recommendation.EvidenceItem;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * DTO + Mapper pattern (ARCHITECTURE_REVISION_JAVA.md §4.1), matching
 * PreparationMapper/InterviewMapper's style: hand-written, no MapStruct.
 * Two overloads exist because evidence arrives in two different shapes
 * depending on where in the pipeline a response is built from: freshly
 * generated recommendations (US-51) still have their evidence as in-memory
 * {@link EvidenceItem} records from the candidate that produced them, while
 * accept/snooze/dismiss (US-55) load a persisted recommendation back and
 * must re-read its {@link RecommendationEvidence} rows instead.
 */
@Component
public class RecommendationMapper {

    public RecommendationResponse toResponse(Recommendation entity, List<EvidenceItem> evidence) {
        return toResponse(entity, evidence, false, 0);
    }

    /**
     * {@code federatedApplied}: P53/US-65 -- true only when a federated
     * model signal was actually folded into this response's ranking (see
     * RecommendationFacade#generateRecommendations). {@code rankDelta}:
     * P57/US-65 -- this recommendation's position change vs. the
     * local-only order; always 0 when {@code federatedApplied} is false.
     */
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
                rankDelta);
    }
}
