package com.cohort.api.service.recommendation.template;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import org.springframework.stereotype.Component;

/**
 * US-51/53: renders a {@link RecommendationType#weak_topic} candidate.
 * Required evidence fields: {@code targetTopicName}, {@code masteryScore},
 * {@code observationCount}. Tier B gets the "early signal only" framing
 * from PROJECT_PLAN.md's tier table example; Tier A states the observation
 * count plainly, since it is already backed by enough evidence to not need
 * hedging.
 */
@Component
public class WeakTopicTemplate implements RecommendationTemplate {

    @Override
    public RecommendationType type() {
        return RecommendationType.weak_topic;
    }

    @Override
    public RenderedRecommendationCopy render(RecommendationCandidate candidate, EvidenceTier tier) {
        String headline = "Practice " + candidate.targetTopicName();
        String observations = "%d observation%s".formatted(
                candidate.observationCount(), candidate.observationCount() == 1 ? "" : "s");
        String explanation = tier == EvidenceTier.B
                ? "Early signal only (%s): %s may be a gap — mastery is sitting at %.0f%%.".formatted(
                        observations, candidate.targetTopicName(), candidate.masteryScore() * 100)
                : "Across %s of %s, average confidence has been low — mastery is at %.0f%%, associated with weaker self-rated performance on related questions.".formatted(
                        observations, candidate.targetTopicName(), candidate.masteryScore() * 100);
        return new RenderedRecommendationCopy(headline, explanation);
    }
}
