package com.cohort.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

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
