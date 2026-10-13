package com.cohort.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** US-91: the target role and date seed the starter plan; {@code skip} completes onboarding without one. */
public record OnboardingRequest(
        @Size(max = 120) String targetRole,
        @Future LocalDate targetDate,
        boolean skip) {
}
