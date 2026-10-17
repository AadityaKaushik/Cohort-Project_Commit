package com.cohort.api.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.security.oauth.OAuthProviderProperties.Credentials;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.core.authority.AuthorityUtils;
import java.util.Map;

class OAuthProviderConfigurationTest {

    @Test
    void noProviderIsOfferedWhenNothingIsConfigured() {
        ConfiguredClientRegistrations registrations = new ConfiguredClientRegistrations(
                new OAuthProviderProperties(new Credentials("", ""), new Credentials(null, null), null));

        assertThat(registrations.providerIds()).isEmpty();
        assertThat(registrations.findByRegistrationId("google")).isNull();
    }

    @Test
    void aProviderNeedsBothItsIdAndItsSecret() {
        ConfiguredClientRegistrations registrations = new ConfiguredClientRegistrations(
                new OAuthProviderProperties(new Credentials("id-only", ""), new Credentials("gh-id", "gh-secret"), new Credentials("li-id", "")));

        assertThat(registrations.providerIds()).containsExactly("github");
    }

    @Test
    void everyProviderIsOfferedWhenAllAreConfigured() {
        ConfiguredClientRegistrations registrations = new ConfiguredClientRegistrations(new OAuthProviderProperties(
                new Credentials("g-id", "g-secret"), new Credentials("gh-id", "gh-secret"), new Credentials("li-id", "li-secret")));

        assertThat(registrations.providerIds()).containsExactly("github", "google", "linkedin");
    }

    @Test
    void linkedinUsesOpenIdConnectWithTheSecretInTheRequestBody() {
        ClientRegistration linkedin = new ConfiguredClientRegistrations(new OAuthProviderProperties(
                null, null, new Credentials("li-id", "li-secret"))).findByRegistrationId("linkedin");

        assertThat(linkedin.getClientId()).isEqualTo("li-id");
        assertThat(linkedin.getScopes()).containsExactlyInAnyOrder("openid", "profile", "email");
        assertThat(linkedin.getClientAuthenticationMethod()).isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        assertThat(linkedin.getProviderDetails().getTokenUri()).isEqualTo("https://www.linkedin.com/oauth/v2/accessToken");
        assertThat(linkedin.getProviderDetails().getIssuerUri()).isEqualTo("https://www.linkedin.com/oauth");
        assertThat(linkedin.getRedirectUri()).isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
    }

    @Test
    void linkedinEmailVerifiedFlagIsReadFromTheProfile() {
        OAuthProfileMapper mapper = new OAuthProfileMapper();
        var verified = new DefaultOAuth2User(AuthorityUtils.NO_AUTHORITIES,
                Map.of("sub", "li-1", "email", "a@example.com", "email_verified", true, "name", "A"), "sub");
        var unverified = new DefaultOAuth2User(AuthorityUtils.NO_AUTHORITIES,
                Map.of("sub", "li-2", "email", "b@example.com", "email_verified", false), "sub");

        OAuthProfile profile = mapper.map("linkedin", verified, "token");
        assertThat(profile.provider()).isEqualTo("linkedin");
        assertThat(profile.subject()).isEqualTo("li-1");
        assertThat(profile.emailVerified()).isTrue();
        assertThat(mapper.linkedin(unverified).emailVerified()).isFalse();
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
