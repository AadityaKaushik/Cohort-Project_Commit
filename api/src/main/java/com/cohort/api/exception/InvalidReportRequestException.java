package com.cohort.api.exception;

import java.util.Map;

/** A report request whose fields are individually valid but don't make sense together (e.g. a CSV progress report, or an inverted date range). */
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
