package com.cohort.api.dto;

public record AuthResponse(UserSummary user, String accessToken, String refreshToken) {
}
