package com.cohort.api.repository;

import com.cohort.api.entity.PrivacyLedgerEntry;
import java.util.List;
import java.util.UUID;

/** User-owned (US-63). */
public interface PrivacyLedgerEntryRepository extends UserScopedRepository<PrivacyLedgerEntry> {

    List<PrivacyLedgerEntry> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
