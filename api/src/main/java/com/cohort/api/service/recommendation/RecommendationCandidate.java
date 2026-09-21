package com.cohort.api.service.recommendation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A candidate recommendation as it flows through the pipeline (§21.2:
 * generate -&gt; route -&gt; score -&gt; template), before scoring or copy has
 * been attached. Deliberately carries only raw facts (mastery numbers,
 * observation counts, evidence) and no user-facing text -- {@code headline}
 * and {@code explanation} are the template registry's job (P41), not a
 * generator's, so that non-causal phrasing lives in exactly one place.
 *
 * <p>{@code severity} and {@code actionability} are the two candidate-supplied
 * inputs to §21.6's scoring formula (the other two, {@code recency} and
 * {@code tier_weight}, are derived later from {@code lastEvidenceAt} and the
 * assigned {@link EvidenceTier}). {@code masteryScore}/{@code rawScore} are
 * carried through only so the template registry can interpolate exact
 * numbers into the rendered explanation (e.g. "0.31 vs 0.46").
 */
public record RecommendationCandidate(
        RecommendationType type,
        UUID targetTopicId,
        String targetTopicName,
        UUID targetApplicationId,
        String targetApplicationLabel,
        double severity,
        double actionability,
        int observationCount,
        Instant lastEvidenceAt,
        double masteryScore,
        double rawScore,
        List<EvidenceItem> evidence,
        List<String> routingChain) {

    public RecommendationCandidate {
        evidence = List.copyOf(evidence);
        routingChain = List.copyOf(routingChain);
    }

    /** §21.6: "actionability = 1.0 (specific topic)". Used by {@link com.cohort.api.service.recommendation.generator.WeakTopicGenerator} and {@link com.cohort.api.service.recommendation.generator.DecayGenerator}. */
    public static final double ACTIONABILITY_SPECIFIC_TOPIC = 1.0;

    /** §21.6: "0.8 (application action)". Used by {@link com.cohort.api.service.recommendation.generator.StalledApplicationGenerator}. */
    public static final double ACTIONABILITY_APPLICATION_ACTION = 0.8;

    /** §21.6: "0.6 (broad)". Used by {@link com.cohort.api.service.recommendation.generator.BaselineGenerator} -- a heuristic with no specific target. */
    public static final double ACTIONABILITY_BROAD = 0.6;

    public static RecommendationCandidate weakTopic(TopicMasterySnapshot snapshot, List<EvidenceItem> evidence) {
        return new RecommendationCandidate(
                RecommendationType.weak_topic, snapshot.topicId(), snapshot.topicName(), null, null,
                1 - snapshot.masteryScore(), ACTIONABILITY_SPECIFIC_TOPIC, snapshot.observationCount(),
                snapshot.lastEvidenceAt(), snapshot.masteryScore(), snapshot.rawScore(), evidence, List.of());
    }

    public static RecommendationCandidate decay(TopicMasterySnapshot snapshot, List<EvidenceItem> evidence) {
        return new RecommendationCandidate(
                RecommendationType.decay, snapshot.topicId(), snapshot.topicName(), null, null,
                1 - snapshot.masteryScore(), ACTIONABILITY_SPECIFIC_TOPIC, snapshot.observationCount(),
                snapshot.lastEvidenceAt(), snapshot.masteryScore(), snapshot.rawScore(), evidence, List.of());
    }

    public static RecommendationCandidate stalledApplication(
            UUID applicationId, String applicationLabel, double severity, int observationCount, List<EvidenceItem> evidence) {
        return new RecommendationCandidate(
                RecommendationType.stalled_application, null, null, applicationId, applicationLabel,
                clamp(severity), ACTIONABILITY_APPLICATION_ACTION, observationCount, null, 0, 0, evidence, List.of());
    }

    public static RecommendationCandidate baseline(int interviewCount, List<EvidenceItem> evidence) {
        return new RecommendationCandidate(
                RecommendationType.baseline, null, null, null, null,
                0, ACTIONABILITY_BROAD, interviewCount, null, 0, 0, evidence, List.of());
    }

    /**
     * P40: rewrites this weak-topic candidate to target the root-cause
     * topic {@code routeToRootCause} (§21.5) walked down to, recording the
     * chain of topic names traversed so the template registry can render
     * "X has gone poorly ... Y is lower still" (§21.5's example).
     */
    public RecommendationCandidate routedTo(TopicMasterySnapshot rootSnapshot, List<EvidenceItem> rootEvidence, List<String> chain) {
        return new RecommendationCandidate(
                RecommendationType.prerequisite_gap, rootSnapshot.topicId(), rootSnapshot.topicName(), null, null,
                1 - rootSnapshot.masteryScore(), ACTIONABILITY_SPECIFIC_TOPIC, rootSnapshot.observationCount(),
                rootSnapshot.lastEvidenceAt(), rootSnapshot.masteryScore(), rootSnapshot.rawScore(), rootEvidence, chain);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
