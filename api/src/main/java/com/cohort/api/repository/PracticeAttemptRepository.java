package com.cohort.api.repository;

import com.cohort.api.entity.PracticeAttempt;
import java.util.List;
import java.util.UUID;

/**
 * Practice attempts are strictly user-owned (US-37, FR-95) — unlike
 * {@code questions}, there is no shared/system row here, so this extends
 * {@link UserScopedRepository} like every other owned entity in the
 * codebase.
 */
public interface PracticeAttemptRepository extends UserScopedRepository<PracticeAttempt> {

    List<PracticeAttempt> findAllByUserIdAndQuestionIdOrderByAttemptedAtDesc(UUID userId, UUID questionId);

    List<PracticeAttempt> findAllByUserIdAndQuestionIdIn(UUID userId, List<UUID> questionIds);
}
