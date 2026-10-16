package com.cohort.api.service.recommendation.generator;

import com.cohort.api.service.application.StallDetectionService;
import com.cohort.api.service.recommendation.EvidenceItem;
import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StalledApplicationGenerator extends RecommendationGenerator {
    @Override
    protected List<RecommendationCandidate> generate(RecommendationContext context) {
        return context.stalledApplications().stream().map(this::toCandidate).toList();
    }

    private RecommendationCandidate toCandidate(StallDetectionService.StalledApplication stalled) {
        StallDetectionService.StallEvaluation evaluation = stalled.evaluation();
        double severity = evaluation.thresholdDays() <= 0
                ? 1.0
                : (evaluation.daysInStage() / evaluation.thresholdDays()) - 1.0;
        String label = stalled.companyName() + ", " + stalled.jobTitle();
        List<EvidenceItem> evidence = List.of(new EvidenceItem(
                "Days in stage",
                "%s has been in %s for %.0f days (threshold %.0f days, %s basis)".formatted(
                        label, stalled.currentStage(), evaluation.daysInStage(), evaluation.thresholdDays(), evaluation.basis()),
                null));
        return RecommendationCandidate.stalledApplication(
                stalled.applicationId(), label, severity, evaluation.personalSampleSize(), evidence);
    }
}
