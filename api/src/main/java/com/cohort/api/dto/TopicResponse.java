package com.cohort.api.dto;

import com.cohort.api.entity.Difficulty;
import com.cohort.api.entity.Topic;
import java.time.Instant;
import java.util.UUID;

public record TopicResponse(
        UUID id,
        String name,
        String slug,
        Topic.Category category,
        String description,
        Difficulty defaultDifficulty,
        short taxonomyVersion,
        Instant createdAt) {
}
