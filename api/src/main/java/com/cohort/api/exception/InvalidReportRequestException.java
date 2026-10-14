package com.cohort.api.exception;

import java.util.Map;

public class InvalidReportRequestException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public InvalidReportRequestException(String field, String message) {
        super(message);
        this.fieldErrors = Map.of(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
