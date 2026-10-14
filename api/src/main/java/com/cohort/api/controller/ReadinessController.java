package com.cohort.api.controller;

import com.cohort.api.dto.ReadinessResponse;
import com.cohort.api.service.preparation.ReadinessService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/readiness")
public class ReadinessController {
    private final ReadinessService readinessService;

    public ReadinessController(ReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    @GetMapping
    public ReadinessResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId) {
        return readinessService.forApplication(UUID.fromString(jwt.getSubject()), applicationId);
    }
}
