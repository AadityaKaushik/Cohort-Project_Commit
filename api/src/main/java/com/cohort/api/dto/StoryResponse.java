package com.cohort.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoryResponse(
        UUID id,
        String title,
        String situation,
        String task,
        String action,
        String result,
        List<String> competencies,
        UUID interviewId,
        Instant createdAt,
        Instant updatedAt) {
}
