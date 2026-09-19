package com.cohort.api.repository;

import com.cohort.api.entity.Interview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Every finder except the two below is inherited from {@link
 * UserScopedRepository}. {@code JpaSpecificationExecutor} backs the small,
 * combinable filter set on {@code GET /interviews} (application_id,
 * upcoming, round_type) built in {@code InterviewService} — every
 * Specification it builds still starts from a {@code userId} equality
 * predicate, so isolation is never at risk of being left out of one branch.
 */
public interface InterviewRepository extends UserScopedRepository<Interview>, JpaSpecificationExecutor<Interview> {

    List<Interview> findAllByUserIdAndApplicationIdOrderByRoundNumberAsc(UUID userId, UUID applicationId);

    boolean existsByApplicationIdAndRoundNumber(UUID applicationId, short roundNumber);

    boolean existsByApplicationIdAndRoundNumberAndIdNot(UUID applicationId, short roundNumber, UUID id);
}
