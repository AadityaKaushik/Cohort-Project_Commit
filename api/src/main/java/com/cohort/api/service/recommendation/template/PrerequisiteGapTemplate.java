package com.cohort.api.service.recommendation.template;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import org.springframework.stereotype.Component;

/**
 * US-52/53: renders a {@link RecommendationType#prerequisite_gap} candidate
 * -- one that P40's {@code RootCauseRouter} has already redirected from a
 * weak topic to a weaker prerequisite. Required evidence field:
 * {@code routingChain}, a list of "Topic Name (NN% mastery)" strings from
 * the original topic down to the routed-to root cause (§21.5's own worked
 * example: "Shortest-path questions have gone poorly ... graph-traversal
 * mastery is lower still").
 */
@Component
public class PrerequisiteGapTemplate implements RecommendationTemplate {

    @Override
    public RecommendationType type() {
        return RecommendationType.prerequisite_gap;
    }

    @Override
    public RenderedRecommendationCopy render(RecommendationCandidate candidate, EvidenceTier tier) {
        String headline = "Work on " + candidate.targetTopicName() + " first";
        java.util.List<String> chain = candidate.routingChain();
        String origin = chain.isEmpty() ? candidate.targetTopicName() : chain.get(0);
        String root = chain.isEmpty() ? candidate.targetTopicName() : chain.get(chain.size() - 1);
        String explanation = "%s has gone poorly, and %s scores lower still — start there before circling back to the original topic.".formatted(origin, root);
        return new RenderedRecommendationCopy(headline, explanation);
    }
}
