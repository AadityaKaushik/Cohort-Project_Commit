package com.cohort.api.exception;

/** 404 for missing or foreign records. */
public class PrivacyLedgerEntryNotFoundException extends RuntimeException {

    public PrivacyLedgerEntryNotFoundException() {
        super("Privacy ledger entry not found.");
    }
}
