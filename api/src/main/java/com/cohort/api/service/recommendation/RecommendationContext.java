package com.cohort.api.service.recommendation;

import com.cohort.api.service.application.StallDetectionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RecommendationContext(
        UUID userId,
        Instant asOf,
        List<TopicMasterySnapshot> topicMasteries,
        Map<UUID, TopicMasterySnapshot> masteryByTopicId,
        Map<UUID, List<UUID>> prerequisitesByTopic,
        List<StallDetectionService.StalledApplication> stalledApplications,
        int interviewCount) {
}
