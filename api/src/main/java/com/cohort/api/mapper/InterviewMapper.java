package com.cohort.api.mapper;

import com.cohort.api.dto.CompanySummary;
import com.cohort.api.dto.InterviewDetailResponse;
import com.cohort.api.dto.InterviewFeedbackResponse;
import com.cohort.api.dto.InterviewQuestionResponse;
import com.cohort.api.dto.InterviewSummary;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewFeedback;
import com.cohort.api.entity.InterviewQuestion;
import org.springframework.stereotype.Component;

/**
 * DTO + Mapper pattern (ARCHITECTURE_REVISION_JAVA.md §4.1), matching
 * ApplicationMapper/ResumeMapper's style: hand-written, no MapStruct. JPA
 * entities never cross the controller boundary.
 */
@Component
public class InterviewMapper {

    public InterviewSummary toSummary(Interview interview, Company company, String jobTitle, boolean hasFeedback) {
        return new InterviewSummary(
                interview.getId(),
                interview.getApplicationId(),
                company.getName(),
                jobTitle,
                interview.getRoundNumber(),
                interview.getRoundType(),
                interview.getScheduledAt(),
                interview.getCompletedAt(),
                interview.getMode(),
                interview.getOutcome(),
                hasFeedback,
                interview.getCreatedAt());
    }

    public InterviewDetailResponse toDetail(Interview interview, Company company, String jobTitle, boolean hasFeedback) {
        return new InterviewDetailResponse(
                interview.getId(),
                interview.getApplicationId(),
                CompanySummary.from(company),
                jobTitle,
                interview.getScheduledAt(),
                interview.getCompletedAt(),
                interview.getRoundNumber(),
                interview.getRoundType(),
                interview.getDurationMinutes(),
                interview.getMode(),
                interview.getInterviewerName(),
                interview.getInterviewerRole(),
                interview.getOutcome(),
                interview.getPerceivedDifficulty(),
                interview.getNotes(),
                hasFeedback,
                interview.getCreatedAt(),
                interview.getUpdatedAt());
    }

    public InterviewQuestionResponse toQuestionResponse(InterviewQuestion question) {
        return new InterviewQuestionResponse(
                question.getId(),
                question.getInterviewId(),
                question.getQuestionText(),
                question.getTopicId(),
                question.getCategory(),
                question.getDifficulty(),
                question.getSelfRating(),
                question.getTimeTakenMinutes(),
                question.getLinkedQuestionId(),
                question.getNotes(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }

    public InterviewFeedbackResponse toFeedbackResponse(InterviewFeedback feedback) {
        return new InterviewFeedbackResponse(
                feedback.getId(),
                feedback.getInterviewId(),
                feedback.getSelfRating(),
                feedback.getWhatWentWell(),
                feedback.getWhatWentPoorly(),
                feedback.getImprovementAreas(),
                feedback.getExternalFeedback(),
                feedback.isDraft(),
                feedback.getSubmittedAt(),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt());
    }
}
