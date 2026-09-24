package com.cohort.api.service.fl;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * US-61/US-63: periodically runs a federated round (over whatever
 * opted-in users {@link FlFeatureExportJob} has exported so far) and
 * records the result in the Privacy Ledger. Runs on its own, separate
 * cadence from the export job — a round only makes sense to attempt after
 * fresh exports have landed, but coupling the two jobs directly would make
 * one slow client's export block every other client's round.
 */
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
