package com.cohort.api.dto;

import jakarta.validation.constraints.Size;

/** PATCH semantics: only non-null fields are applied. */
public record UpdateCompanyRequest(@Size(max = 500) String website, @Size(max = 200) String industry) {
}
