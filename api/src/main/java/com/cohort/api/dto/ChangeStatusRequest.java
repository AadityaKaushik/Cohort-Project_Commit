package com.cohort.api.dto;

import com.cohort.api.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

/** FR-20: {@code note} is optional in general, but required for a backward "correction" transition (§17.5). */
public record ChangeStatusRequest(@NotNull ApplicationStatus toStatus, String note) {
}
