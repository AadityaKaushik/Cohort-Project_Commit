package com.cohort.api.service.recommendation.template;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import org.springframework.stereotype.Component;

@Component
public class BaselineTemplate implements RecommendationTemplate {
    @Override
    public RecommendationType type() {
        return RecommendationType.baseline;
    }

    @Override
    public RenderedRecommendationCopy render(RecommendationCandidate candidate, EvidenceTier tier) {
        String headline = "Start with a broad baseline";
        String explanation = "You have %d interview%s logged so far, which is not enough to point at a weak topic. This is a general suggestion, not based on your data. More specific ones appear once you log a few interviews.".formatted(
                candidate.observationCount(), candidate.observationCount() == 1 ? "" : "s");
        return new RenderedRecommendationCopy(headline, explanation);
    }
}
