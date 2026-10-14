package com.cohort.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record ResumeDetailResponse(
        UUID id,
        String name,
        String versionLabel,
        String targetRole,
        String summary,
        @JsonProperty("is_default") boolean isDefault,
        UUID parentResumeId,
        @JsonProperty("has_file") boolean hasFile,
        String fileOriginalName,
        Long fileSizeBytes,
        String fileMime,
        Instant fileUploadedAt,
        Instant createdAt,
        Instant updatedAt) {
}
