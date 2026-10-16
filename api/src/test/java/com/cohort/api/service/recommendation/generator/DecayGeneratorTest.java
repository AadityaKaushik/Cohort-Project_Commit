package com.cohort.api.service.recommendation.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import com.cohort.api.service.recommendation.RecommendationType;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DecayGeneratorTest {

    private final DecayGenerator generator = new DecayGenerator();

    @Test
    void firesWhenRawScoreWasFineButDecayedBelowComfortable() {
        TopicMasterySnapshot snapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Dynamic Programming", 0.55, 0.45, 6, true, Instant.now());

        List<RecommendationCandidate> result = generator.handle(contextWith(snapshot), List.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(RecommendationType.decay);
    }

    @Test
    void doesNotFireWhenRawScoreWasAlreadyWeak() {
        // rawScore below the 0.5 floor: a WeakTopicGenerator case, not decay.
        TopicMasterySnapshot snapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Dynamic Programming", 0.45, 0.42, 6, true, Instant.now());

        assertThat(generator.handle(contextWith(snapshot), List.of())).isEmpty();
    }

    @Test
    void doesNotFireWhenMasteryIsBelowTheWeakTopicThreshold() {
        // Already below 0.4: left to WeakTopicGenerator.
        TopicMasterySnapshot snapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Dynamic Programming", 0.6, 0.35, 6, true, Instant.now());

        assertThat(generator.handle(contextWith(snapshot), List.of())).isEmpty();
    }

    @Test
    void doesNotFireWhenMasteryIsAlreadyComfortable() {
        TopicMasterySnapshot snapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Dynamic Programming", 0.7, 0.65, 6, true, Instant.now());

        assertThat(generator.handle(contextWith(snapshot), List.of())).isEmpty();
    }

    @Test
    void doesNotFireWhenEvidenceIsInsufficient() {
        TopicMasterySnapshot snapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Dynamic Programming", 0.55, 0.45, 1, false, Instant.now());

        assertThat(generator.handle(contextWith(snapshot), List.of())).isEmpty();
    }

    private RecommendationContext contextWith(TopicMasterySnapshot snapshot) {
        return new RecommendationContext(
                UUID.randomUUID(), Instant.now(), List.of(snapshot), Map.of(snapshot.topicId(), snapshot), Map.of(), List.of(), 0);
    }
}
