package com.cohort.api.repository;

import com.cohort.api.entity.InterviewQuestion;
import java.util.List;
import java.util.UUID;

public interface InterviewQuestionRepository extends UserScopedRepository<InterviewQuestion> {
    List<InterviewQuestion> findAllByUserIdAndInterviewIdOrderByCreatedAtAsc(UUID userId, UUID interviewId);

    List<InterviewQuestion> findAllByUserIdAndTopicIdIsNotNullAndSelfRatingIsNotNull(UUID userId);
}
