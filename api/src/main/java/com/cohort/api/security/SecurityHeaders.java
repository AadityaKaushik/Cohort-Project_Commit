package com.cohort.api.security;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/** NFR-03: response headers shared by every filter chain. The API only serves JSON and files, so nothing may load. */
public final class SecurityHeaders {
    public static final String CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'";
    public static final long HSTS_MAX_AGE_SECONDS = 31_536_000;

    private SecurityHeaders() {
    }

    public static void apply(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_MAX_AGE_SECONDS))
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(Customizer.withDefaults()));
    }
}
