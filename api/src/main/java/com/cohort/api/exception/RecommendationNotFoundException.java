package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class RecommendationNotFoundException extends RuntimeException {

    public RecommendationNotFoundException() {
        super("Recommendation not found.");
    }
}
