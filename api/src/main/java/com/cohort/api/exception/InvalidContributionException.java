package com.cohort.api.exception;

import java.util.Map;

public class InvalidContributionException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public InvalidContributionException(String field, String message) {
        super(message);
        this.fieldErrors = Map.of(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
