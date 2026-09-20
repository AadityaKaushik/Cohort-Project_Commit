package com.cohort.api.repository;

import com.cohort.api.entity.ApplicationStatusHistory;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Deliberately NOT a {@link UserScopedRepository} — this table has no
 * {@code user_id} column of its own (PROJECT_PLAN.md §17.2); isolation is
 * enforced one level up, by always resolving the owning {@code Application}
 * (or applications) via {@code ApplicationRepository.findByIdAndUserId} /
 * {@code findAllByUserId} first — in {@code ApplicationService} and, for the
 * bulk finder below, in {@code AnalyticsService} — before ever touching
 * history rows.
 */
public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {

    List<ApplicationStatusHistory> findByApplicationIdOrderByChangedAtDesc(UUID applicationId);

    /** Bulk fetch backing {@code AnalyticsService}'s per-user funnel/conversion/rejection/dwell computations — one query for every history row across every application the caller already scoped to a single user, instead of one query per application. */
    List<ApplicationStatusHistory> findByApplicationIdIn(Collection<UUID> applicationIds);
}
