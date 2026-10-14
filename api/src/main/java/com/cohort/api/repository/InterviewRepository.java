package com.cohort.api.repository;

import com.cohort.api.entity.Interview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InterviewRepository extends UserScopedRepository<Interview>, JpaSpecificationExecutor<Interview> {
    List<Interview> findAllByUserIdAndApplicationIdOrderByRoundNumberAsc(UUID userId, UUID applicationId);

    boolean existsByApplicationIdAndRoundNumber(UUID applicationId, short roundNumber);

    boolean existsByApplicationIdAndRoundNumberAndIdNot(UUID applicationId, short roundNumber, UUID id);
}
