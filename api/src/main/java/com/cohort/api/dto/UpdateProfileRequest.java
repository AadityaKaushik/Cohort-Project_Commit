package com.cohort.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Partial update (FR-07): a null field means "leave unchanged," not
 * "clear this field" — there's currently no field a user needs to blank
 * out, so that distinction isn't worth the extra API surface yet.
 */
public record UpdateProfileRequest(
        @Email String email,
        @Size(max = 120) String displayName,
        @Size(max = 120) String targetRole,
        Short graduationYear,
        @Size(max = 60) String timezone) {
}
