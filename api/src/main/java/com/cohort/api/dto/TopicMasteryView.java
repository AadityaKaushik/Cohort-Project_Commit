package com.cohort.api.dto;

import com.cohort.api.entity.Topic;
import java.util.UUID;

public record TopicMasteryView(
        UUID topicId,
        String topicName,
        String slug,
        Topic.Category category,
        double masteryScore,
        int observationCount,
        boolean sufficientEvidence) {
}
