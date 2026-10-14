package com.cohort.api.dto;

import com.cohort.api.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull ApplicationStatus toStatus, String note) {
}
