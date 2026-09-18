package com.cohort.api.repository;

import com.cohort.api.entity.ApplicationStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Deliberately NOT a {@link UserScopedRepository} — this table has no
 * {@code user_id} column of its own (PROJECT_PLAN.md §17.2); isolation is
 * enforced one level up, by always resolving the owning {@code Application}
 * via {@code ApplicationRepository.findByIdAndUserId} first, in
 * {@code ApplicationService}, before ever touching history rows.
 */
public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {

    List<ApplicationStatusHistory> findByApplicationIdOrderByChangedAtDesc(UUID applicationId);
}
