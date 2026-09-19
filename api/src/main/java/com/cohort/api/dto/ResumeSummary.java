package com.cohort.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/** One row of the resumes list (US-20): enough to identify and pick a version without the full detail payload. */
public record ResumeSummary(
        UUID id,
        String name,
        String versionLabel,
        String targetRole,
        @JsonProperty("is_default") boolean isDefault,
        @JsonProperty("has_file") boolean hasFile,
        UUID parentResumeId,
        Instant createdAt,
        Instant updatedAt) {
}
