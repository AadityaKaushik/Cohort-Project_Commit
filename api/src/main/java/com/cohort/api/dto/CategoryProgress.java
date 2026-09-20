package com.cohort.api.dto;

import com.cohort.api.entity.Topic;

/**
 * US-41: "progress bars per category" — {@code averageMastery} is null
 * when no topic in the category has sufficient evidence yet, rather than a
 * misleading 0.
 */
public record CategoryProgress(
        Topic.Category category, int totalTopics, long tasksTotal, long tasksCompleted, Double averageMastery) {
}
