package com.cohort.api.dto;

/** source is "model" or "template"; aiAvailable is false when the provider could not be reached. */
public record AiDraftResponse(String draft, String source, boolean aiAvailable) {
}
