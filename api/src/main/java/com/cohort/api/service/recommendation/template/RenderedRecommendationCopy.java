package com.cohort.api.service.recommendation.template;

public record RenderedRecommendationCopy(String headline, String explanation) {
    public RenderedRecommendationCopy {
        if (headline == null || headline.isBlank()) {
            throw new IllegalStateException("Headline is blank.");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalStateException("Explanation is blank.");
        }
    }
}
