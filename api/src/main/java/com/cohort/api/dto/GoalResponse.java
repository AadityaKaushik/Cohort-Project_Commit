package com.cohort.api.dto;

import com.cohort.api.entity.Goal;
import java.util.UUID;

public record GoalResponse(UUID id, Goal.Metric metric, int weeklyTarget, int progress, boolean achieved) {
}
