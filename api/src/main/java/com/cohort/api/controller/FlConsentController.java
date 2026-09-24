package com.cohort.api.controller;

import com.cohort.api.dto.FlConsentResponse;
import com.cohort.api.dto.FlSchemaResponse;
import com.cohort.api.service.fl.FlConsentService;
import com.cohort.api.service.fl.FlFeatureSchema;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-57/US-64: opt-in/opt-out plus the schema the consent screen shows.
 * Deliberately thin (ARCHITECTURE_REVISION_JAVA.md §4.2) — no pipeline to
 * hide behind a facade here, just a boolean flip and a static allow-list.
 */
@RestController
@RequestMapping("/api/v1/fl")
public class FlConsentController {

    private final FlConsentService consentService;

    public FlConsentController(FlConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/consent")
    public FlConsentResponse getConsent(@AuthenticationPrincipal Jwt jwt) {
        return new FlConsentResponse(consentService.isOptedIn(userId(jwt)));
    }

    @PostMapping("/opt-in")
    public FlConsentResponse optIn(@AuthenticationPrincipal Jwt jwt) {
        return new FlConsentResponse(consentService.optIn(userId(jwt)));
    }

    @PostMapping("/opt-out")
    public FlConsentResponse optOut(@AuthenticationPrincipal Jwt jwt) {
        return new FlConsentResponse(consentService.optOut(userId(jwt)));
    }

    @GetMapping("/schema")
    public FlSchemaResponse schema() {
        List<FlSchemaResponse.FeatureBound> features = FlFeatureSchema.FEATURE_NAMES.stream()
                .map(name -> {
                    double[] bounds = FlFeatureSchema.FEATURE_BOUNDS.get(name);
                    return new FlSchemaResponse.FeatureBound(name, bounds[0], bounds[1]);
                })
                .toList();
        return new FlSchemaResponse(1, FlFeatureSchema.FEATURE_COUNT, features, FlFeatureSchema.NOT_COLLECTED);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
