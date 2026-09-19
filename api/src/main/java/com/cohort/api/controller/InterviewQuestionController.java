package com.cohort.api.controller;

import com.cohort.api.dto.InterviewQuestionResponse;
import com.cohort.api.dto.UpdateInterviewQuestionRequest;
import com.cohort.api.service.InterviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Split from {@link InterviewController} purely because its path
 * (PROJECT_PLAN.md §18.5: {@code PATCH|DELETE /interview-questions/{id}})
 * addresses a question directly rather than nesting under its interview —
 * both controllers delegate to the same {@link InterviewService}.
 */
@RestController
@RequestMapping("/api/v1/interview-questions")
public class InterviewQuestionController {

    private final InterviewService interviewService;

    public InterviewQuestionController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PatchMapping("/{id}")
    public InterviewQuestionResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateInterviewQuestionRequest request) {
        return interviewService.updateQuestion(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        interviewService.deleteQuestion(userId(jwt), id);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
