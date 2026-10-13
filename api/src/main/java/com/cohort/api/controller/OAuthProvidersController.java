package com.cohort.api.controller;

import com.cohort.api.security.oauth.ConfiguredClientRegistrations;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tells the sign-in pages which provider buttons to show, so an unconfigured provider is never offered. */
@RestController
@RequestMapping("/api/v1/auth/oauth")
public class OAuthProvidersController {

    private final ConfiguredClientRegistrations registrations;

    public OAuthProvidersController(ConfiguredClientRegistrations registrations) {
        this.registrations = registrations;
    }

    @GetMapping("/providers")
    public Map<String, List<String>> providers() {
        return Map.of("providers", registrations.providerIds());
    }
}
