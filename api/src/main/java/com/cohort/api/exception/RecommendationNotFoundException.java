package com.cohort.api.exception;

/** Thrown for both a missing recommendation ID and one belonging to another user -- FR-95: always 404, never 403. */
public class RecommendationNotFoundException extends RuntimeException {

    public RecommendationNotFoundException() {
        super("Recommendation not found.");
    }
}
