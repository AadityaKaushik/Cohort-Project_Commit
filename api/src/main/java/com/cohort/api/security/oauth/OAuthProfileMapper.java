package com.cohort.api.security.oauth;

import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Strategy per provider for reading a provider-specific user into the neutral {@link OAuthProfile}. */
@Component
public class OAuthProfileMapper {

    private static final String GITHUB_EMAILS_URL = "https://api.github.com/user/emails";

    private final RestClient restClient = RestClient.create();

    public OAuthProfile map(String registrationId, OAuth2User user, String accessToken) {
        return switch (registrationId) {
            case "google" -> google(user);
            case "github" -> github(user, accessToken);
            default -> throw new IllegalArgumentException("Unsupported provider " + registrationId);
        };
    }

    OAuthProfile google(OAuth2User user) {
        Object verified = user.getAttribute("email_verified");
        return new OAuthProfile(
                "google",
                String.valueOf((Object) user.getAttribute("sub")),
                user.getAttribute("email"),
                Boolean.TRUE.equals(verified) || "true".equalsIgnoreCase(String.valueOf(verified)),
                user.getAttribute("name"));
    }

    OAuthProfile github(OAuth2User user, String accessToken) {
        String subject = String.valueOf((Object) user.getAttribute("id"));
        String displayName = user.getAttribute("name") != null ? user.getAttribute("name") : user.getAttribute("login");
        List<Map<String, Object>> emails = fetchEmails(accessToken);
        for (Map<String, Object> entry : emails) {
            if (Boolean.TRUE.equals(entry.get("primary")) && Boolean.TRUE.equals(entry.get("verified"))) {
                return new OAuthProfile("github", subject, String.valueOf(entry.get("email")), true, displayName);
            }
        }
        // No primary verified address: whatever the profile shows is unverified and must never link an account.
        return new OAuthProfile("github", subject, user.getAttribute("email"), false, displayName);
    }

    private List<Map<String, Object>> fetchEmails(String accessToken) {
        try {
            List<Map<String, Object>> body = restClient.get()
                    .uri(GITHUB_EMAILS_URL)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return body == null ? List.of() : body;
        } catch (RuntimeException e) {
            return List.of();
        }
    }
}
