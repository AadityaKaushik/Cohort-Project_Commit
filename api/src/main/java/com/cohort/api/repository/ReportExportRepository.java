package com.cohort.api.repository;

import com.cohort.api.entity.ReportExport;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReportExportRepository extends UserScopedRepository<ReportExport> {
    List<ReportExport> findAllByUserIdAndExpiresAtAfterOrderByGeneratedAtDesc(UUID userId, Instant now);

    List<ReportExport> findAllByExpiresAtLessThanEqual(Instant now);

    void delete(ReportExport export);
}
