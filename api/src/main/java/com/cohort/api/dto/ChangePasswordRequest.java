package com.cohort.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** US-06: the current password proves the session holder is the account owner; the new one is re-validated against the password policy. */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 10, max = 128) String newPassword) {
}
