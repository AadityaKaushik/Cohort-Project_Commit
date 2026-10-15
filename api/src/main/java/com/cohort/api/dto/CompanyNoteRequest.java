package com.cohort.api.dto;

import com.cohort.api.validation.MeaningfulText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyNoteRequest(@NotBlank @Size(max = 5000) @MeaningfulText String body) {
}
