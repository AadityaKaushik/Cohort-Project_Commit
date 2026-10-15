package com.cohort.api.dto;

public record EmailDraftResponse(String kind, String subject, String body, String source, boolean aiAvailable) {
}
