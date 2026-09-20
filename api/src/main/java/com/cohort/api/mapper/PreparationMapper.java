package com.cohort.api.mapper;

import com.cohort.api.dto.PracticeAttemptResponse;
import com.cohort.api.dto.QuestionResponse;
import com.cohort.api.dto.TopicResponse;
import com.cohort.api.entity.PracticeAttempt;
import com.cohort.api.entity.Question;
import com.cohort.api.entity.Topic;
import org.springframework.stereotype.Component;

/**
 * DTO + Mapper pattern (ARCHITECTURE_REVISION_JAVA.md §4.1), matching
 * InterviewMapper/ResumeMapper's style: hand-written, no MapStruct. JPA
 * entities never cross the controller boundary.
 */
@Component
public class PreparationMapper {

    public TopicResponse toResponse(Topic topic) {
        return new TopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getSlug(),
                topic.getCategory(),
                topic.getDescription(),
                topic.getDefaultDifficulty(),
                topic.getTaxonomyVersion(),
                topic.getCreatedAt());
    }

    public QuestionResponse toResponse(Question question, String topicName) {
        return new QuestionResponse(
                question.getId(),
                question.getTopicId(),
                topicName,
                question.getQuestionText(),
                question.getCategory(),
                question.getDifficulty(),
                question.isSystem(),
                question.getSourceQuestionId(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }

    public PracticeAttemptResponse toResponse(PracticeAttempt attempt) {
        return new PracticeAttemptResponse(
                attempt.getId(),
                attempt.getQuestionId(),
                attempt.getStatus(),
                attempt.getConfidence(),
                attempt.getTimeTakenMinutes(),
                attempt.getNotes(),
                attempt.getAttemptedAt(),
                attempt.getCreatedAt());
    }
}
