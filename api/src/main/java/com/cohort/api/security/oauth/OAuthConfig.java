package com.cohort.api.security.oauth;

import com.cohort.api.security.SecurityHeaders;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableConfigurationProperties(OAuthProviderProperties.class)
public class OAuthConfig {
    @Bean
    public ConfiguredClientRegistrations clientRegistrationRepository(OAuthProviderProperties properties) {
        return new ConfiguredClientRegistrations(properties);
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ConfiguredClientRegistrations registrations) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain oauthLoginFilterChain(
            HttpSecurity http,
            ConfiguredClientRegistrations registrations,
            OAuth2AuthorizedClientService authorizedClientService,
            OAuthLoginHandlers handlers) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(login -> login
                        .clientRegistrationRepository(registrations)
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(
                                new KnownProvidersOnlyResolver(registrations)))
                        .authorizedClientService(authorizedClientService)
                        .successHandler(handlers)
                        .failureHandler(handlers));
        SecurityHeaders.apply(http);
        return http.build();
    }

    static final class KnownProvidersOnlyResolver implements OAuth2AuthorizationRequestResolver {
        private final ClientRegistrationRepository registrations;
        private final OAuth2AuthorizationRequestResolver delegate;

        KnownProvidersOnlyResolver(ClientRegistrationRepository registrations) {
            this.registrations = registrations;
            this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                    registrations, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        }

        @Override
        public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
            return knows(request) ? delegate.resolve(request) : null;
        }

        @Override
        public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
            return registrations.findByRegistrationId(clientRegistrationId) == null
                    ? null
                    : delegate.resolve(request, clientRegistrationId);
        }

        private boolean knows(HttpServletRequest request) {
            String prefix = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/";
            String path = request.getRequestURI();
            int start = path.indexOf(prefix);
            if (start < 0) {
                return false;
            }
            return registrations.findByRegistrationId(path.substring(start + prefix.length())) != null;
        }
    }
}
