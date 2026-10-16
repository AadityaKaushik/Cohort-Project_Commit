package com.cohort.api.controller;

import com.cohort.api.dto.InterviewQuestionResponse;
import com.cohort.api.dto.QuestionResponse;
import com.cohort.api.dto.SaveToQuestionBankRequest;
import com.cohort.api.dto.UpdateInterviewQuestionRequest;
import com.cohort.api.service.InterviewService;
import com.cohort.api.service.PreparationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interview-questions")
public class InterviewQuestionController {
    private final InterviewService interviewService;
    private final PreparationService preparationService;

    public InterviewQuestionController(InterviewService interviewService, PreparationService preparationService) {
        this.interviewService = interviewService;
        this.preparationService = preparationService;
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

    @PostMapping("/{id}/save-to-bank")
    public ResponseEntity<QuestionResponse> saveToQuestionBank(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody(required = false) SaveToQuestionBankRequest request) {
        PreparationService.SavedQuestion saved =
                preparationService.saveInterviewQuestionToBank(userId(jwt), id, request == null ? null : request.topicId());
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK).body(saved.question());
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
