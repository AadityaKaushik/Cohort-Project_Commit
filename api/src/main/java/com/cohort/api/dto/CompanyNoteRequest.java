package com.cohort.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyNoteRequest(@NotBlank @Size(max = 5000) String body) {
}
