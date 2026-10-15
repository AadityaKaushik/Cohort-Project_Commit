package com.cohort.api.controller;

import com.cohort.api.dto.SampleDataResponse;
import com.cohort.api.service.SampleDataService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sample-data")
public class SampleDataController {

    private final SampleDataService sampleDataService;

    public SampleDataController(SampleDataService sampleDataService) {
        this.sampleDataService = sampleDataService;
    }

    @GetMapping
    public SampleDataResponse status(@AuthenticationPrincipal Jwt jwt) {
        return sampleDataService.status(userId(jwt));
    }

    @PostMapping
    public SampleDataResponse load(@AuthenticationPrincipal Jwt jwt) {
        return sampleDataService.load(userId(jwt));
    }

    @DeleteMapping
    public SampleDataResponse remove(@AuthenticationPrincipal Jwt jwt) {
        return sampleDataService.remove(userId(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
