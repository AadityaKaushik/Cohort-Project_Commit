package com.cohort.api.security.oauth;

/** What a provider told us about the person who just signed in, reduced to the fields account linking needs. */
public record OAuthProfile(String provider, String subject, String email, boolean emailVerified, String displayName) {
}
