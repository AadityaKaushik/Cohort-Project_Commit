package com.cohort.api.service.recommendation.template;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import org.springframework.stereotype.Component;

/**
 * PROJECT_PLAN.md §21.5's own Tier D example: "You have no interviews
 * logged. Start with a broad baseline." Required evidence field:
 * {@code observationCount} (here, the interview count) -- there is
 * deliberately no topic/application evidence to cite, and the copy says so
 * plainly rather than inventing specificity (§21.3: "None -- heuristic, and
 * says so").
 */
@Component
public class BaselineTemplate implements RecommendationTemplate {

    @Override
    public RecommendationType type() {
        return RecommendationType.baseline;
    }

    @Override
    public RenderedRecommendationCopy render(RecommendationCandidate candidate, EvidenceTier tier) {
        String headline = "Start with a broad baseline";
        String explanation = "You have %d interview%s logged so far — not enough yet to point at a specific weak topic. This is a general heuristic, not evidence-based guidance; more targeted recommendations will appear once you log a few interviews.".formatted(
                candidate.observationCount(), candidate.observationCount() == 1 ? "" : "s");
        return new RenderedRecommendationCopy(headline, explanation);
    }
}
