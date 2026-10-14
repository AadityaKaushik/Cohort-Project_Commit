package com.cohort.api.dto;

import java.util.List;
import java.util.UUID;

public record ReadinessResponse(
        Double score, String formatted, int sampleSize, boolean sufficientData, List<TopicContribution> topics) {

    public record TopicContribution(UUID topicId, String topicName, int weight, double masteryScore, boolean hasEvidence) {
    }
}
