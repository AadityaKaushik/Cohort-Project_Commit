package com.cohort.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateResumeRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 50) String versionLabel,
        @Size(max = 200) String targetRole,
        String summary,
        @JsonProperty("is_default") Boolean isDefault) {
}
