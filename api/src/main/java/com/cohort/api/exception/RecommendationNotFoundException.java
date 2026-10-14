package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class RecommendationNotFoundException extends RuntimeException {

    public RecommendationNotFoundException() {
        super("Recommendation not found.");
    }
}
