package com.cohort.api.controller;

import com.cohort.api.dto.ConversionRatesResponse;
import com.cohort.api.dto.DwellTimesResponse;
import com.cohort.api.dto.FunnelResponse;
import com.cohort.api.dto.RejectionDistributionResponse;
import com.cohort.api.dto.TimelineResponse;
import com.cohort.api.service.analytics.AnalyticsService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/funnel")
    public FunnelResponse funnel(@AuthenticationPrincipal Jwt jwt) {
        return analyticsService.funnel(userId(jwt));
    }

    @GetMapping("/conversions")
    public ConversionRatesResponse conversions(@AuthenticationPrincipal Jwt jwt) {
        return analyticsService.conversions(userId(jwt));
    }

    @GetMapping("/timeline")
    public TimelineResponse timeline(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusWeeks(12);
        return analyticsService.timeline(userId(jwt), resolvedFrom, resolvedTo);
    }

    @GetMapping("/rejections")
    public RejectionDistributionResponse rejections(@AuthenticationPrincipal Jwt jwt) {
        return analyticsService.rejectionDistribution(userId(jwt));
    }

    @GetMapping("/dwell-times")
    public DwellTimesResponse dwellTimes(@AuthenticationPrincipal Jwt jwt) {
        return analyticsService.dwellTimes(userId(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
