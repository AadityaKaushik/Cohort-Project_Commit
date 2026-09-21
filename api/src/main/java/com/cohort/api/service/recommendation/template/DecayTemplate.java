package com.cohort.api.service.recommendation.template;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import org.springframework.stereotype.Component;

/**
 * US-51/53: renders a {@link RecommendationType#decay} candidate. Required
 * evidence fields: {@code targetTopicName}, {@code rawScore}, {@code masteryScore}.
 * Distinguishes "went stale" from "always weak" explicitly, per §21.3's own
 * definition of this generator.
 */
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
