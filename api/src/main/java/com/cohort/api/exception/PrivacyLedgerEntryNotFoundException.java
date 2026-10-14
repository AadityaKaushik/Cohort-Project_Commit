package com.cohort.api.exception;

/** Missing or owned by another user: 404, never 403. */
public class PrivacyLedgerEntryNotFoundException extends RuntimeException {

    public PrivacyLedgerEntryNotFoundException() {
        super("Privacy ledger entry not found.");
    }
}
