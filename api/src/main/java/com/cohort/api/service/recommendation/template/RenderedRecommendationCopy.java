package com.cohort.api.service.recommendation.template;

public record RenderedRecommendationCopy(String headline, String explanation) {
    public RenderedRecommendationCopy {
        if (headline == null || headline.isBlank()) {
            throw new IllegalStateException("A recommendation template must always render a non-blank headline.");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalStateException("A recommendation template must always render a non-blank explanation.");
        }
    }
}
