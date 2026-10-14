package com.cohort.api.dto;

import com.cohort.api.entity.Goal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetGoalRequest(@NotNull Goal.Metric metric, @Min(1) @Max(1000) int weeklyTarget) {
}
