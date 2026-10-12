package com.cohort.api.dto;

import jakarta.validation.constraints.NotBlank;

/** US-08: the typed confirmation, which must equal {@code DELETE} exactly. */
public record DeleteAccountRequest(@NotBlank String confirmation) {
}
