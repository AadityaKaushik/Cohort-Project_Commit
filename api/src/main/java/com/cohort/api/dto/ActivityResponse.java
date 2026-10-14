package com.cohort.api.dto;

import java.util.List;

public record ActivityResponse(List<ActivityDay> days, int currentStreak, int longestStreak) {
}
