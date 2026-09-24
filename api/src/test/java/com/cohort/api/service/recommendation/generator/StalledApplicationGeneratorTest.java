package com.cohort.api.service.recommendation.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.application.StallDetectionService;
import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import com.cohort.api.service.recommendation.RecommendationType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StalledApplicationGeneratorTest {

    private final StalledApplicationGenerator generator = new StalledApplicationGenerator();

    @Test
    void emitsOneCandidatePerStalledApplication() {
        UUID applicationId = UUID.randomUUID();
        StallDetectionService.StallEvaluation evaluation =
                new StallDetectionService.StallEvaluation(true, 20.0, 14.0, StallDetectionService.BASIS_SYSTEM_DEFAULT, 0);
        StallDetectionService.StalledApplication stalled = new StallDetectionService.StalledApplication(
                applicationId, "Acme Corp", "Backend Engineer", ApplicationStatus.APPLIED, evaluation, Instant.now());

        List<RecommendationCandidate> result = generator.handle(contextWith(stalled), List.of());

        assertThat(result).hasSize(1);
        RecommendationCandidate candidate = result.get(0);
        assertThat(candidate.type()).isEqualTo(RecommendationType.stalled_application);
        assertThat(candidate.targetApplicationId()).isEqualTo(applicationId);
        assertThat(candidate.severity()).isGreaterThan(0);
        assertThat(candidate.evidence()).isNotEmpty();
    }

    @Test
    void emitsNothingWhenThereAreNoStalledApplications() {
        assertThat(generator.handle(emptyContext(), List.of())).isEmpty();
    }

    private RecommendationContext contextWith(StallDetectionService.StalledApplication stalled) {
        return new RecommendationContext(UUID.randomUUID(), Instant.now(), List.of(), Map.of(), Map.of(), List.of(stalled), 0);
    }

    private RecommendationContext emptyContext() {
        return new RecommendationContext(UUID.randomUUID(), Instant.now(), List.of(), Map.of(), Map.of(), List.of(), 0);
    }
}
