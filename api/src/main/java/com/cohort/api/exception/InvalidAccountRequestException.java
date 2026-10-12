package com.cohort.api.exception;

import java.util.Map;

/** An account-management request whose fields are well-formed but wrong: a mistyped current password or deletion confirmation. */
public class InvalidAccountRequestException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidAccountRequestException(String field, String message) {
        super(message);
        this.fieldErrors = Map.of(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
