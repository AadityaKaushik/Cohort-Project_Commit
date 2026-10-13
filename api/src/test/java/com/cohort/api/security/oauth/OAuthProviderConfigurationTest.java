package com.cohort.api.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.security.oauth.OAuthProviderProperties.Credentials;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.core.authority.AuthorityUtils;
import java.util.Map;

class OAuthProviderConfigurationTest {

    @Test
    void noProviderIsOfferedWhenNothingIsConfigured() {
        ConfiguredClientRegistrations registrations = new ConfiguredClientRegistrations(
                new OAuthProviderProperties(new Credentials("", ""), new Credentials(null, null)));

        assertThat(registrations.providerIds()).isEmpty();
        assertThat(registrations.findByRegistrationId("google")).isNull();
    }

    @Test
    void aProviderNeedsBothItsIdAndItsSecret() {
        ConfiguredClientRegistrations registrations = new ConfiguredClientRegistrations(
                new OAuthProviderProperties(new Credentials("id-only", ""), new Credentials("gh-id", "gh-secret")));

        assertThat(registrations.providerIds()).containsExactly("github");
    }

    @Test
    void bothProvidersAreOfferedWhenBothAreConfigured() {
        ConfiguredClientRegistrations registrations = new ConfiguredClientRegistrations(
                new OAuthProviderProperties(new Credentials("g-id", "g-secret"), new Credentials("gh-id", "gh-secret")));

        assertThat(registrations.providerIds()).containsExactly("github", "google");
    }

    @Test
    void googleEmailVerifiedFlagIsReadFromTheProfile() {
        OAuthProfileMapper mapper = new OAuthProfileMapper();
        var verified = new DefaultOAuth2User(AuthorityUtils.NO_AUTHORITIES,
                Map.of("sub", "1", "email", "a@example.com", "email_verified", true, "name", "A"), "sub");
        var unverified = new DefaultOAuth2User(AuthorityUtils.NO_AUTHORITIES,
                Map.of("sub", "2", "email", "b@example.com", "email_verified", false), "sub");

        assertThat(mapper.google(verified).emailVerified()).isTrue();
        assertThat(mapper.google(unverified).emailVerified()).isFalse();
    }
}
