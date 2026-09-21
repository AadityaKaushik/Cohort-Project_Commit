package com.cohort.api.service.recommendation.template;

/**
 * The output of rendering one {@link RecommendationTemplate}: an imperative
 * headline and an associative (never causal, US-54) explanation. Both are
 * mandatory, non-blank strings -- US-53: "a unit test asserts no
 * recommendation renders without an explanation."
 */
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
