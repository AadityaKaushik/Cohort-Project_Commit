package com.cohort.api.security.oauth;

public record OAuthProfile(String provider, String subject, String email, boolean emailVerified, String displayName) {
}
