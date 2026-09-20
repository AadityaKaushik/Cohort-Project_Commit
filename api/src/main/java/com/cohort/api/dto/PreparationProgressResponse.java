package com.cohort.api.dto;

import java.util.List;

/** {@code GET /preparation/progress} (US-41): category-level progress bars plus the full topic mastery grid for drill-down. */
public record PreparationProgressResponse(List<CategoryProgress> categories, List<TopicMasteryView> topics) {
}
