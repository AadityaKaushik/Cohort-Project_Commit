package com.cohort.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/** Full detail view of a resume (US-20..22): metadata plus whatever is known about its stored file, if any. */
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
