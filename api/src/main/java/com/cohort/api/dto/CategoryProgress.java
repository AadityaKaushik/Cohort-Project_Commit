package com.cohort.api.dto;

import com.cohort.api.entity.Topic;

public record CategoryProgress(
        Topic.Category category, int totalTopics, long tasksTotal, long tasksCompleted, Double averageMastery) {
}
