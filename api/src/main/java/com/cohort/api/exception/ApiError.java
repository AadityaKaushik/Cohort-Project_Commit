package com.cohort.api.exception;

import java.util.Map;

/**
 * The error envelope every endpoint uses (PROJECT_PLAN.md §18):
 * {"error": {"code", "message", "field_errors"}}.
 */
public record ApiError(ErrorBody error) {

    public record ErrorBody(String code, String message, Map<String, String> fieldErrors) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(new ErrorBody(code, message, null));
    }

    public static ApiError of(String code, String message, Map<String, String> fieldErrors) {
        return new ApiError(new ErrorBody(code, message, fieldErrors));
    }
}
