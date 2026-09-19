package com.cohort.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/** Partial update: a null field means "leave unchanged" (same convention as UpdateApplicationRequest). */
public record UpdateResumeRequest(
        @Size(max = 200) String name,
        @Size(max = 50) String versionLabel,
        @Size(max = 200) String targetRole,
        String summary,
        @JsonProperty("is_default") Boolean isDefault) {
}
