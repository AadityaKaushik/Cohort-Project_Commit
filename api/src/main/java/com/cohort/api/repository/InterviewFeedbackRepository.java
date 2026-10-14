package com.cohort.api.repository;

import com.cohort.api.entity.InterviewFeedback;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewFeedbackRepository extends UserScopedRepository<InterviewFeedback> {
    Optional<InterviewFeedback> findByUserIdAndInterviewId(UUID userId, UUID interviewId);

    List<InterviewFeedback> findAllByUserIdAndInterviewIdIn(UUID userId, Collection<UUID> interviewIds);
}
