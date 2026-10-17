package com.cohort.api.dto;

/** Tokens are null when the account still has to confirm its email with a code. */
public record AuthResponse(UserSummary user, String accessToken, String refreshToken, boolean verificationRequired) {

    public static AuthResponse signedIn(UserSummary user, String accessToken, String refreshToken) {
        return new AuthResponse(user, accessToken, refreshToken, false);
    }

    public static AuthResponse awaitingVerification(UserSummary user) {
        return new AuthResponse(user, null, null, true);
    }
}
