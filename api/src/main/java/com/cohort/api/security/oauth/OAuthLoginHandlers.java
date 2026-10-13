package com.cohort.api.security.oauth;

import com.cohort.api.entity.User;
import com.cohort.api.exception.OAuthLinkRefusedException;
import com.cohort.api.service.OAuthAccountService;
import com.cohort.api.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Ends the provider round trip by issuing the application's own access and refresh tokens, exactly as
 * password login does, and handing them to the single-page app in a URL fragment (never sent to a server).
 */
@Component
public class OAuthLoginHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final OAuthProfileMapper profileMapper;
    private final OAuthAccountService accountService;
    private final TokenService tokenService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final String frontendBaseUrl;

    public OAuthLoginHandlers(
            OAuthProfileMapper profileMapper,
            OAuthAccountService accountService,
            TokenService tokenService,
            OAuth2AuthorizedClientService authorizedClientService,
            @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.profileMapper = profileMapper;
        this.accountService = accountService;
        this.tokenService = tokenService;
        this.authorizedClientService = authorizedClientService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String registrationId = token.getAuthorizedClientRegistrationId();
        OAuth2AuthorizedClient client =
                authorizedClientService.loadAuthorizedClient(registrationId, authentication.getName());
        String accessToken = client == null ? "" : client.getAccessToken().getTokenValue();
        OAuth2User principal = token.getPrincipal();

        try {
            User user = accountService.resolveUser(profileMapper.map(registrationId, principal, accessToken));
            String fragment = "access_token=" + encode(tokenService.issueAccessToken(user))
                    + "&refresh_token=" + encode(tokenService.issueRefreshToken(user));
            response.sendRedirect(frontendBaseUrl + "/oauth/callback#" + fragment);
        } catch (OAuthLinkRefusedException refused) {
            response.sendRedirect(frontendBaseUrl + "/login?oauth_error=" + refused.getReason().name().toLowerCase());
        } finally {
            authorizedClientService.removeAuthorizedClient(registrationId, authentication.getName());
        }
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.sendRedirect(frontendBaseUrl + "/login?oauth_error=failed");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
