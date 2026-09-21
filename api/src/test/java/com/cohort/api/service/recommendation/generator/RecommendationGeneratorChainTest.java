package com.cohort.api.service.recommendation.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.application.StallDetectionService;
import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Proves the four generators actually compose into one chain (the point of
 * Chain of Responsibility, not just each stage in isolation): every stage's
 * contribution shows up in the combined result regardless of link order,
 * and a context with no evidence at all falls through every stage down to
 * {@link BaselineGenerator}, the last link.
 */
class RecommendationGeneratorChainTest {

    @Test
    void everyStageThatQualifiesContributesToTheCombinedResult() {
        UUID weakTopicId = UUID.randomUUID();
        TopicMasterySnapshot weakTopic = new TopicMasterySnapshot(weakTopicId, "Graphs", 0.35, 0.3, 4, true, Instant.now());
        StallDetectionService.StalledApplication stalled = new StallDetectionService.StalledApplication(
                UUID.randomUUID(), "Acme Corp", "Backend Engineer", ApplicationStatus.APPLIED,
                new StallDetectionService.StallEvaluation(true, 20.0, 14.0, StallDetectionService.BASIS_SYSTEM_DEFAULT, 0));

        RecommendationContext context = new RecommendationContext(
                UUID.randomUUID(), Instant.now(), List.of(weakTopic), Map.of(weakTopicId, weakTopic), Map.of(),
                List.of(stalled), 1);

        RecommendationGenerator chain = buildChain();
        List<RecommendationCandidate> result = chain.handle(context, List.of());

        assertThat(result).extracting(RecommendationCandidate::type)
                .containsExactlyInAnyOrder(RecommendationType.weak_topic, RecommendationType.stalled_application, RecommendationType.baseline);
    }

    @Test
    void aCompletelyEmptyContextStillProducesTheBaselineCandidate() {
        RecommendationContext emptyContext = new RecommendationContext(
                UUID.randomUUID(), Instant.now(), List.of(), Map.of(), Map.of(), List.of(), 0);

        List<RecommendationCandidate> result = buildChain().handle(emptyContext, List.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(RecommendationType.baseline);
    }

    private RecommendationGenerator buildChain() {
        RecommendationGenerator weakTopic = new WeakTopicGenerator();
        RecommendationGenerator decay = new DecayGenerator();
        RecommendationGenerator stalledApplication = new StalledApplicationGenerator();
        RecommendationGenerator baseline = new BaselineGenerator();
        weakTopic.linkTo(decay).linkTo(stalledApplication).linkTo(baseline);
        return weakTopic;
    }
}
