package com.cohort.api.service.recommendation.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.service.recommendation.RecommendationContext;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// Exercises {@link WeakTopicGenerator} in complete isolation -- no Spring context, no other generator, per US-51's independent-testability requirement.
class WeakTopicGeneratorTest {
    private final WeakTopicGenerator generator = new WeakTopicGenerator();

    @Test
    void firesForATopicWithLowMasteryAndSufficientEvidence() {
        UUID topicId = UUID.randomUUID();
        TopicMasterySnapshot snapshot = new TopicMasterySnapshot(topicId, "Graphs", 0.35, 0.3, 4, true, Instant.now());

        List<com.cohort.api.service.recommendation.RecommendationCandidate> result =
                generator.handle(contextWith(snapshot), List.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(RecommendationType.weak_topic);
        assertThat(result.get(0).targetTopicId()).isEqualTo(topicId);
        assertThat(result.get(0).evidence()).isNotEmpty();
    }

    @Test
    void doesNotFireWhenEvidenceIsInsufficient() {
        TopicMasterySnapshot snapshot =
                new TopicMasterySnapshot(UUID.randomUUID(), "Graphs", 0.2, 0.15, 1, false, Instant.now());

        assertThat(generator.handle(contextWith(snapshot), List.of())).isEmpty();
    }

    @Test
    void doesNotFireWhenMasteryIsAtOrAboveTheThreshold() {
        TopicMasterySnapshot snapshot =
                new TopicMasterySnapshot(UUID.randomUUID(), "Graphs", 0.5, 0.4, 5, true, Instant.now());

        assertThat(generator.handle(contextWith(snapshot), List.of())).isEmpty();
    }

    @Test
    void forwardsToTheNextGeneratorInTheChain() {
        RecommendationGenerator noOp = new RecommendationGenerator() {
            @Override
            protected List<com.cohort.api.service.recommendation.RecommendationCandidate> generate(RecommendationContext context) {
                return List.of();
            }
        };
        generator.linkTo(noOp);
        TopicMasterySnapshot snapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Graphs", 0.35, 0.3, 4, true, Instant.now());

        assertThat(generator.handle(contextWith(snapshot), List.of())).hasSize(1);
    }

    private RecommendationContext contextWith(TopicMasterySnapshot snapshot) {
        return new RecommendationContext(
                UUID.randomUUID(), Instant.now(), List.of(snapshot), Map.of(snapshot.topicId(), snapshot), Map.of(), List.of(), 0);
    }
}
