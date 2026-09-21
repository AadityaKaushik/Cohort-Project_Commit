package com.cohort.api.service.recommendation;

import com.cohort.api.service.application.StallDetectionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything the candidate-generation chain (P39) and the root-cause router
 * (P40) need, precomputed once by {@code RecommendationFacade} (P42) and
 * handed down unchanged. Generators never touch a repository directly --
 * this is what makes {@link com.cohort.api.service.recommendation.generator.RecommendationGenerator#handle}
 * callable in a unit test with no Spring context and no database.
 */
public record RecommendationContext(
        UUID userId,
        Instant asOf,
        List<TopicMasterySnapshot> topicMasteries,
        Map<UUID, TopicMasterySnapshot> masteryByTopicId,
        Map<UUID, List<UUID>> prerequisitesByTopic,
        List<StallDetectionService.StalledApplication> stalledApplications,
        int interviewCount) {
}
