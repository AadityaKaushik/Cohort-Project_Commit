package com.cohort.api.service.fl;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PrivacyLedgerRoundJob {
    private final PrivacyLedgerService privacyLedgerService;

    public PrivacyLedgerRoundJob(PrivacyLedgerService privacyLedgerService) {
        this.privacyLedgerService = privacyLedgerService;
    }

    @Scheduled(cron = "${app.fl.round.cron}")
    public void runOnSchedule() {
        privacyLedgerService.runNextRoundAndPersist();
    }
}
