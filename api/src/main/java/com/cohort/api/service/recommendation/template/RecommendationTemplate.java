package com.cohort.api.service.recommendation.template;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;

public interface RecommendationTemplate {
    RecommendationType type();

    RenderedRecommendationCopy render(RecommendationCandidate candidate, EvidenceTier tier);
}
