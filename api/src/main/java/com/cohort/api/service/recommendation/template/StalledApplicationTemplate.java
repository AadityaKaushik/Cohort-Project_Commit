package com.cohort.api.service.recommendation.template;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import org.springframework.stereotype.Component;

/**
 * US-56/53: renders a {@link RecommendationType#stalled_application}
 * candidate. Required evidence field: {@code targetApplicationLabel}
 * (company + role, US-56: "includes the specific application").
 */
@Component
public class StalledApplicationTemplate implements RecommendationTemplate {

    @Override
    public RecommendationType type() {
        return RecommendationType.stalled_application;
    }

    @Override
    public RenderedRecommendationCopy render(RecommendationCandidate candidate, EvidenceTier tier) {
        String headline = "Follow up on " + candidate.targetApplicationLabel();
        String explanation = "%s has gone longer without a status update than similar applications typically do. A short follow-up message is a reasonable next move, though there's no way to know how the company will respond.".formatted(
                candidate.targetApplicationLabel());
        return new RenderedRecommendationCopy(headline, explanation);
    }
}
