package com.cohort.api.repository;

import com.cohort.api.entity.InterviewFeedback;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Every finder is inherited from {@link UserScopedRepository} except the two below. */
public interface InterviewFeedbackRepository extends UserScopedRepository<InterviewFeedback> {

    Optional<InterviewFeedback> findByUserIdAndInterviewId(UUID userId, UUID interviewId);

    /** Bulk existence check backing the interviews list's "has_feedback" flag, avoiding one query per row. */
    List<InterviewFeedback> findAllByUserIdAndInterviewIdIn(UUID userId, Collection<UUID> interviewIds);
}
