package com.cohort.api.controller;

import com.cohort.api.dto.DevFlRoundResponse;
import com.cohort.api.dto.FlDemoStatusResponse;
import com.cohort.api.entity.FlRound;
import com.cohort.api.service.fl.FlDemoService;
import com.cohort.api.service.fl.FlFeatureExportJob;
import com.cohort.api.service.fl.PrivacyLedgerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Dev profile only: runs a round now instead of waiting for the schedule, and drives the demo page.
@Profile("dev")
@RestController
@RequestMapping("/api/v1/dev/fl")
public class DevFlRoundController {
    private final FlFeatureExportJob exportJob;
    private final PrivacyLedgerService privacyLedgerService;
    private final FlDemoService demoService;

    public DevFlRoundController(FlFeatureExportJob exportJob, PrivacyLedgerService privacyLedgerService, FlDemoService demoService) {
        this.exportJob = exportJob;
        this.privacyLedgerService = privacyLedgerService;
        this.demoService = demoService;
    }

    @PostMapping("/round")
    public DevFlRoundResponse runRound(@RequestBody(required = false) RoundRequest request) {
        FlFeatureExportJob.ExportSummary export = exportJob.runExport();
        Optional<FlRound> round = privacyLedgerService.runNextRoundAndPersist(request == null ? null : request.noiseMultiplier());
        return new DevFlRoundResponse(
                export.succeeded(),
                export.failed(),
                round.isPresent(),
                round.map(FlRound::getRoundNumber).orElse(null),
                round.map(FlRound::isAccepted).orElse(null),
                round.map(FlRound::getReason).orElse(null),
                round.map(FlRound::getCohortSize).orElse(null),
                round.map(FlRound::getSatOutCount).orElse(null),
                round.map(FlRound::getModelVersion).orElse(null),
                round.map(FlRound::getCheckAccuracy).orElse(null));
    }

    @GetMapping("/status")
    public FlDemoStatusResponse status(@AuthenticationPrincipal Jwt jwt) {
        return demoService.status(UUID.fromString(jwt.getSubject()));
    }

    @PutMapping("/synthetic-clients")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setSyntheticClients(@Valid @RequestBody SyntheticClientsRequest request) {
        demoService.setSyntheticClients(request.count(), request.seed() == null ? 7 : request.seed());
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        demoService.reset();
    }

    public record RoundRequest(Double noiseMultiplier) {
    }

    public record SyntheticClientsRequest(@Min(0) @Max(200) int count, Integer seed) {
    }
}
