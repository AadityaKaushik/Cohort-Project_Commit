package com.cohort.api.service.recommendation.generator;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import java.util.ArrayList;
import java.util.List;

public abstract class RecommendationGenerator {
    private RecommendationGenerator next;

    public RecommendationGenerator linkTo(RecommendationGenerator next) {
        this.next = next;
        return next;
    }

    public final List<RecommendationCandidate> handle(RecommendationContext context, List<RecommendationCandidate> candidatesSoFar) {
        List<RecommendationCandidate> combined = new ArrayList<>(candidatesSoFar);
        combined.addAll(generate(context));
        return next == null ? combined : next.handle(context, combined);
    }

    protected abstract List<RecommendationCandidate> generate(RecommendationContext context);
}
