package com.cohort.api.dto;

import com.cohort.api.entity.User;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        String targetRole,
        Short graduationYear,
        String timezone,
        boolean flOptIn,
        String flPrivacyPosture,
        Instant createdAt) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getTargetRole(),
                user.getGraduationYear(),
                user.getTimezone(),
                user.isFlOptIn(),
                user.getFlPrivacyPosture().name(),
                user.getCreatedAt());
    }
}
