package com.cohort.api.controller;

import com.cohort.api.dto.DevFlRoundResponse;
import com.cohort.api.entity.FlRound;
import com.cohort.api.service.fl.FlFeatureExportJob;
import com.cohort.api.service.fl.PrivacyLedgerService;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Dev profile only: lets fl-service's demo command run a round now instead of waiting for the cron.
@Profile("dev")
@RestController
@RequestMapping("/api/v1/dev/fl")
public class DevFlRoundController {
    private final FlFeatureExportJob exportJob;
    private final PrivacyLedgerService privacyLedgerService;

    public DevFlRoundController(FlFeatureExportJob exportJob, PrivacyLedgerService privacyLedgerService) {
        this.exportJob = exportJob;
        this.privacyLedgerService = privacyLedgerService;
    }

    @PostMapping("/round")
    public DevFlRoundResponse runRound() {
        FlFeatureExportJob.ExportSummary export = exportJob.runExport();
        Optional<FlRound> round = privacyLedgerService.runNextRoundAndPersist();
        return new DevFlRoundResponse(
                export.succeeded(),
                export.failed(),
                round.isPresent(),
                round.map(FlRound::getRoundNumber).orElse(null),
                round.map(FlRound::isAccepted).orElse(null),
                round.map(FlRound::getReason).orElse(null),
                round.map(FlRound::getCohortSize).orElse(null));
    }
}
