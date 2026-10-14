package com.cohort.api.dto;

import java.util.List;

public record PreparationProgressResponse(List<CategoryProgress> categories, List<TopicMasteryView> topics) {
}
