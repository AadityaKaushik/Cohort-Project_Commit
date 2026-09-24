package com.cohort.api.exception;

/** Thrown for both a missing Privacy Ledger entry ID and one belonging to another user -- FR-95: always 404, never 403. */
public class PrivacyLedgerEntryNotFoundException extends RuntimeException {

    public PrivacyLedgerEntryNotFoundException() {
        super("Privacy ledger entry not found.");
    }
}
