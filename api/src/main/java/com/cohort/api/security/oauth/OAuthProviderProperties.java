package com.cohort.api.security.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth")
public record OAuthProviderProperties(Credentials google, Credentials github, Credentials linkedin) {

    public record Credentials(String clientId, String clientSecret) {

        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
        }
    }

    public OAuthProviderProperties {
        google = google == null ? new Credentials("", "") : google;
        github = github == null ? new Credentials("", "") : github;
        linkedin = linkedin == null ? new Credentials("", "") : linkedin;
    }
}
