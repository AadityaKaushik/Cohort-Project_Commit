package com.cohort.api.repository;

import com.cohort.api.entity.ReportExport;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Strictly user-owned (FR-104, FR-95): every finder a request can reach is
 * scoped by {@code userId} via {@link UserScopedRepository}.
 * {@link #findAllByExpiresAtLessThanEqual} is the one deliberate exception --
 * it backs the system-level expiry cleanup job, which has no requesting user
 * and never returns anything to a client.
 */
public interface ReportExportRepository extends UserScopedRepository<ReportExport> {

    List<ReportExport> findAllByUserIdAndExpiresAtAfterOrderByGeneratedAtDesc(UUID userId, Instant now);

    List<ReportExport> findAllByExpiresAtLessThanEqual(Instant now);

    void delete(ReportExport export);
}
