package com.cohort.api.service.recommendation.template;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;

/**
 * US-53/54 / PROJECT_PLAN.md §21.7: one template per {@link RecommendationType},
 * declaring both the headline (imperative -- "do this") and the explanation
 * (associative -- "here is why," never causal) for that type. All
 * user-facing recommendation copy lives behind this interface and nowhere
 * else, which is what makes {@code test_no_causal_language} an exhaustive
 * check rather than a spot check: every string a user can ever see is
 * produced by exactly one of these five implementations.
 */
public interface RecommendationTemplate {

    RecommendationType type();

    RenderedRecommendationCopy render(RecommendationCandidate candidate, EvidenceTier tier);
}
