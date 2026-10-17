package com.cohort.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailVerificationConfirmRequest(@NotBlank @Email String email, @NotBlank @Size(max = 12) String code) {
}
