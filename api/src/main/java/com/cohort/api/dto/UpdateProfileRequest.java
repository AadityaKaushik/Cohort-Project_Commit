package com.cohort.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** A null field is left unchanged. */
public record UpdateProfileRequest(
        @Email String email,
        @Size(max = 120) String displayName,
        @Size(max = 120) String targetRole,
        Short graduationYear,
        java.time.LocalDate targetDate,
        @Size(max = 60) String timezone) {
}
