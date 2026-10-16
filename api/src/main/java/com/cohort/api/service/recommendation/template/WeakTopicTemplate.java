package com.cohort.api.service.recommendation.template;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import org.springframework.stereotype.Component;

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
                ? "Early signal only (%s): %s may be a gap. Mastery is %.0f%%.".formatted(
                        observations, candidate.targetTopicName(), candidate.masteryScore() * 100)
                : "Across %s of %s, average confidence has been low. Mastery is %.0f%%, and your self-ratings on related questions are also lower.".formatted(
                        observations, candidate.targetTopicName(), candidate.masteryScore() * 100);
        return new RenderedRecommendationCopy(headline, explanation);
    }
}
