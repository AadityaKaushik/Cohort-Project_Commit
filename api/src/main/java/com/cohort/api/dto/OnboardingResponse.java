package com.cohort.api.dto;

public record OnboardingResponse(UserProfileResponse profile, int tasksCreated) {
}
