package com.cohort.api.repository;

import com.cohort.api.entity.PrivacyLedgerEntry;
import java.util.List;
import java.util.UUID;

/** Strictly user-owned (US-63, FR-95): every finder is scoped by userId via {@link UserScopedRepository}, plus the one ordering method the ledger UI (P56) needs. */
public interface PrivacyLedgerEntryRepository extends UserScopedRepository<PrivacyLedgerEntry> {

    List<PrivacyLedgerEntry> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
