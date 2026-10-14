package com.cohort.api.service.recommendation.template;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.service.recommendation.EvidenceItem;
import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationTemplateRegistryTest {
    private final RecommendationTemplateRegistry registry = new RecommendationTemplateRegistry(List.of(
            new WeakTopicTemplate(), new DecayTemplate(), new PrerequisiteGapTemplate(),
            new StalledApplicationTemplate(), new BaselineTemplate()));

    @Test
    void test_no_causal_language() {
        for (RecommendationCandidate candidate : representativeCandidates()) {
            RenderedRecommendationCopy copy = registry.render(candidate, EvidenceTier.A);
            List<String> violations = CausalLanguageLinter.findViolations(copy.headline() + " " + copy.explanation());
            assertThat(violations)
                    .as("template for %s must not use causal language, found: %s", candidate.type(), violations)
                    .isEmpty();
        }
    }

    // Proves the linter used above is not vacuously passing: rendering a deliberately bad phrase through the exact same checker must be flagged.
    @Test
    void theLinterCatchesAnInjectedBannedPhrase() {
        String deliberatelyBadCopy = "This caused your rejection because you failed the interview.";

        assertThat(CausalLanguageLinter.findViolations(deliberatelyBadCopy)).isNotEmpty();
    }

    @Test
    void everyTemplateRendersWithItsDeclaredEvidenceFieldsPresent() {
        TopicMasterySnapshot weakSnapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Graph Traversal", 0.3, 0.28, 6, true, Instant.now());
        RecommendationCandidate weakTopic = RecommendationCandidate.weakTopic(weakSnapshot, List.of());
        RenderedRecommendationCopy weakCopy = registry.render(weakTopic, EvidenceTier.A);
        assertThat(weakCopy.headline() + weakCopy.explanation()).contains("Graph Traversal").contains("28%").contains("6 observations");

        TopicMasterySnapshot decaySnapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Dynamic Programming", 0.6, 0.45, 6, true, Instant.now());
        RecommendationCandidate decay = RecommendationCandidate.decay(decaySnapshot, List.of());
        RenderedRecommendationCopy decayCopy = registry.render(decay, EvidenceTier.A);
        assertThat(decayCopy.headline() + decayCopy.explanation()).contains("Dynamic Programming").contains("60%").contains("45%");

        TopicMasterySnapshot rootSnapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Graph Traversal (BFS/DFS)", 0.3, 0.30, 6, true, Instant.now());
        RecommendationCandidate routed = weakTopic.routedTo(
                rootSnapshot, List.of(), List.of("Shortest Path (46% mastery)", "Graph Traversal (BFS/DFS) (30% mastery)"));
        RenderedRecommendationCopy routedCopy = registry.render(routed, EvidenceTier.A);
        assertThat(routedCopy.headline() + routedCopy.explanation())
                .contains("Shortest Path (46% mastery)")
                .contains("Graph Traversal (BFS/DFS) (30% mastery)");

        RecommendationCandidate stalled = RecommendationCandidate.stalledApplication(
                UUID.randomUUID(), "Acme Corp — Backend Engineer", 0.4, 4, List.of(new EvidenceItem("Days in stage", "20 days", null)));
        RenderedRecommendationCopy stalledCopy = registry.render(stalled, EvidenceTier.B);
        assertThat(stalledCopy.headline() + stalledCopy.explanation()).contains("Acme Corp — Backend Engineer");

        RecommendationCandidate baseline = RecommendationCandidate.baseline(1, List.of());
        RenderedRecommendationCopy baselineCopy = registry.render(baseline, EvidenceTier.D);
        assertThat(baselineCopy.headline() + baselineCopy.explanation()).contains("1 interview");
    }

    @Test
    void noTemplateRendersWithoutAnExplanation() {
        for (RecommendationCandidate candidate : representativeCandidates()) {
            RenderedRecommendationCopy copy = registry.render(candidate, EvidenceTier.A);
            assertThat(copy.explanation()).isNotBlank();
            assertThat(copy.headline()).isNotBlank();
        }
    }

    private List<RecommendationCandidate> representativeCandidates() {
        TopicMasterySnapshot weakSnapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Graphs", 0.3, 0.28, 6, true, Instant.now());
        TopicMasterySnapshot decaySnapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Dynamic Programming", 0.6, 0.45, 6, true, Instant.now());
        TopicMasterySnapshot rootSnapshot = new TopicMasterySnapshot(UUID.randomUUID(), "Two Pointers", 0.2, 0.18, 6, true, Instant.now());

        RecommendationCandidate weakTopic = RecommendationCandidate.weakTopic(weakSnapshot, List.of());
        RecommendationCandidate decay = RecommendationCandidate.decay(decaySnapshot, List.of());
        RecommendationCandidate prerequisiteGap = weakTopic.routedTo(
                rootSnapshot, List.of(), List.of("Sliding Window (46% mastery)", "Two Pointers (18% mastery)"));
        RecommendationCandidate stalled = RecommendationCandidate.stalledApplication(
                UUID.randomUUID(), "Acme Corp — Backend Engineer", 0.4, 4, List.of());
        RecommendationCandidate baseline = RecommendationCandidate.baseline(1, List.of());

        return List.of(weakTopic, decay, prerequisiteGap, stalled, baseline);
    }
}
