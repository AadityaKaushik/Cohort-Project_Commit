package com.cohort.api.repository;

import com.cohort.api.entity.PracticeAttempt;
import java.util.List;
import java.util.UUID;

public interface PracticeAttemptRepository extends UserScopedRepository<PracticeAttempt> {
    List<PracticeAttempt> findAllByUserIdAndQuestionIdOrderByAttemptedAtDesc(UUID userId, UUID questionId);

    List<PracticeAttempt> findAllByUserIdAndQuestionIdIn(UUID userId, List<UUID> questionIds);
}
