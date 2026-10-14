package com.cohort.api.service.recommendation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    public static final double ACTIONABILITY_SPECIFIC_TOPIC = 1.0;

    public static final double ACTIONABILITY_APPLICATION_ACTION = 0.8;

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
