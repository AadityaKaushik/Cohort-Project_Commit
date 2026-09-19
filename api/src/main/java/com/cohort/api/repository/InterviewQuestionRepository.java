package com.cohort.api.repository;

import com.cohort.api.entity.InterviewQuestion;
import java.util.List;
import java.util.UUID;

/**
 * Every finder except the one below is inherited from {@link
 * UserScopedRepository}. Deleting an interview cascades to its questions at
 * the DB level (V9/V10 {@code ON DELETE CASCADE}), so no explicit
 * bulk-delete method is needed here.
 */
public interface InterviewQuestionRepository extends UserScopedRepository<InterviewQuestion> {

    List<InterviewQuestion> findAllByUserIdAndInterviewIdOrderByCreatedAtAsc(UUID userId, UUID interviewId);
}
