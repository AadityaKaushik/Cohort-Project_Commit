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
        String explanation = "%s scored %.0f%% on the underlying evidence, but mastery has settled at %.0f%% simply from time passing since you last practiced it — a quick refresh should bring it back up.".formatted(
                candidate.targetTopicName(), candidate.rawScore() * 100, candidate.masteryScore() * 100);
        return new RenderedRecommendationCopy(headline, explanation);
    }
}
