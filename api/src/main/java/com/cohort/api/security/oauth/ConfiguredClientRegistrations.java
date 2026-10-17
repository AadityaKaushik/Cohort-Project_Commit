package com.cohort.api.security.oauth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

public class ConfiguredClientRegistrations implements ClientRegistrationRepository {
    private final Map<String, ClientRegistration> registrations;

    public ConfiguredClientRegistrations(OAuthProviderProperties properties) {
        List<ClientRegistration> configured = new ArrayList<>();
        if (properties.google().isConfigured()) {
            configured.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(properties.google().clientId())
                    .clientSecret(properties.google().clientSecret())
                    .scope("openid", "email", "profile")
                    .build());
        }
        if (properties.github().isConfigured()) {
            configured.add(CommonOAuth2Provider.GITHUB.getBuilder("github")
                    .clientId(properties.github().clientId())
                    .clientSecret(properties.github().clientSecret())
                    .scope("read:user", "user:email")
                    .build());
        }
        if (properties.linkedin().isConfigured()) {
            configured.add(linkedin(properties.linkedin()));
        }
        this.registrations = configured.stream()
                .collect(Collectors.toUnmodifiableMap(ClientRegistration::getRegistrationId, registration -> registration));
    }

    // Spring has no built-in LinkedIn provider. LinkedIn uses OpenID Connect and wants the secret in the token request body.
    static ClientRegistration linkedin(OAuthProviderProperties.Credentials credentials) {
        return ClientRegistration.withRegistrationId("linkedin")
                .clientId(credentials.clientId())
                .clientSecret(credentials.clientSecret())
                .clientName("LinkedIn")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://www.linkedin.com/oauth/v2/authorization")
                .tokenUri("https://www.linkedin.com/oauth/v2/accessToken")
                .userInfoUri("https://api.linkedin.com/v2/userinfo")
                .jwkSetUri("https://www.linkedin.com/oauth/openid/jwks")
                .issuerUri("https://www.linkedin.com/oauth")
                .userNameAttributeName("sub")
                .build();
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return registrations.get(registrationId);
    }

    public List<String> providerIds() {
        return registrations.keySet().stream().sorted().toList();
    }
}
