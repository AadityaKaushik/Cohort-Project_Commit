package com.cohort.api.dto;

import java.util.List;

public record TextCheckResponse(boolean valid, List<Finding> errors, List<Finding> warnings) {
    public record Finding(String code, String message) {
    }
}
