package com.cohort.api.service.recommendation.generator;

import com.cohort.api.service.recommendation.EvidenceItem;
import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * PROJECT_PLAN.md §21.3, Tier D: "The user has fewer than 3 interviews
 * logged | None -- heuristic, and says so." The last stage in the chain: a
 * single generic candidate that only fires when nothing else has any real
 * evidence to work with yet, so a brand-new user is never shown an empty
 * recommendations list. Its evidence list is deliberately empty -- "None"
 * is the point, and the template registry (P41) renders that honestly
 * rather than inventing a number.
 */
@Component
public class BaselineGenerator extends RecommendationGenerator {

    static final int MINIMUM_INTERVIEWS = 3;

    @Override
    protected List<RecommendationCandidate> generate(RecommendationContext context) {
        if (context.interviewCount() >= MINIMUM_INTERVIEWS) {
            return List.of();
        }
        return List.of(RecommendationCandidate.baseline(context.interviewCount(), List.<EvidenceItem>of()));
    }
}
