package com.cohort.api.service.recommendation.template;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import org.springframework.stereotype.Component;

@Component
public class DecayTemplate implements RecommendationTemplate {
    @Override
    public RecommendationType type() {
        return RecommendationType.decay;
    }

    @Override
    public RenderedRecommendationCopy render(RecommendationCandidate candidate, EvidenceTier tier) {
        String headline = "Refresh " + candidate.targetTopicName();
        String explanation = "%s scored %.0f%% when you practised it. Mastery is now %.0f%% since some time has passed. A short review should bring it back up.".formatted(
                candidate.targetTopicName(), candidate.rawScore() * 100, candidate.masteryScore() * 100);
        return new RenderedRecommendationCopy(headline, explanation);
    }
}
