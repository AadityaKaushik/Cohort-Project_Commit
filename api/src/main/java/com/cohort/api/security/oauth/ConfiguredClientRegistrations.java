package com.cohort.api.security.oauth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

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
        this.registrations = configured.stream()
                .collect(Collectors.toUnmodifiableMap(ClientRegistration::getRegistrationId, registration -> registration));
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return registrations.get(registrationId);
    }

    public List<String> providerIds() {
        return registrations.keySet().stream().sorted().toList();
    }
}
