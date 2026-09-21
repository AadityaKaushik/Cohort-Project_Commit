package com.cohort.api.service.recommendation;

import java.time.Instant;
import java.util.UUID;

/**
 * One topic's mastery state for one user, as of a given instant --
 * {@link TopicMasteryLoader}'s output and the input every topic-based
 * candidate generator ({@link com.cohort.api.service.recommendation.generator.WeakTopicGenerator},
 * {@link com.cohort.api.service.recommendation.generator.DecayGenerator}) reasons over.
 *
 * <p>{@code rawScore} is the blended, un-decayed evidence score and
 * {@code masteryScore} is {@code rawScore} after §21.4's time decay -- both
 * come straight from {@link com.cohort.api.service.preparation.MasteryEngine#computeMastery};
 * carrying both (rather than just the decayed score, as
 * {@code TopicMasteryView} does for the progress screen) is what lets
 * {@link com.cohort.api.service.recommendation.generator.DecayGenerator}
 * tell "always been weak" apart from "went stale."
 */
public record TopicMasterySnapshot(
        UUID topicId,
        String topicName,
        double rawScore,
        double masteryScore,
        int observationCount,
        boolean sufficientEvidence,
        Instant lastEvidenceAt) {
}
