package com.cohort.api.service.recommendation.generator;

import com.cohort.api.service.recommendation.EvidenceItem;
import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import java.util.List;
import org.springframework.stereotype.Component;

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
