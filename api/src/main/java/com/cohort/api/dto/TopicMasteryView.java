package com.cohort.api.dto;

import com.cohort.api.entity.Topic;
import java.util.UUID;

/**
 * One row of the progress view's topic grid (US-41): mastery is always
 * present as a number (PROJECT_PLAN.md §21.4: "the score is stored") but
 * {@code sufficientEvidence} tells the frontend whether to present it with
 * confidence or as "insufficient data" — an accessible non-colour
 * indicator alongside the coloured grid cell.
 */
public record TopicMasteryView(
        UUID topicId,
        String topicName,
        String slug,
        Topic.Category category,
        double masteryScore,
        int observationCount,
        boolean sufficientEvidence) {
}
