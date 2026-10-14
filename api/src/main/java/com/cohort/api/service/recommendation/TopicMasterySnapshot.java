package com.cohort.api.service.recommendation;

import java.time.Instant;
import java.util.UUID;

public record TopicMasterySnapshot(
        UUID topicId,
        String topicName,
        double rawScore,
        double masteryScore,
        int observationCount,
        boolean sufficientEvidence,
        Instant lastEvidenceAt) {
}
