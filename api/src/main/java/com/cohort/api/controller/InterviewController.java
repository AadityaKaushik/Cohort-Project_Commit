package com.cohort.api.controller;

import com.cohort.api.dto.CreateInterviewQuestionRequest;
import com.cohort.api.dto.CreateInterviewRequest;
import com.cohort.api.dto.InterviewDetailResponse;
import com.cohort.api.dto.InterviewFeedbackRequest;
import com.cohort.api.dto.InterviewFeedbackResponse;
import com.cohort.api.dto.InterviewQuestionResponse;
import com.cohort.api.dto.InterviewSummary;
import com.cohort.api.dto.PagedResponse;
import com.cohort.api.dto.UpdateInterviewRequest;
import com.cohort.api.entity.InterviewRoundType;
import com.cohort.api.service.InterviewService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interviews")
public class InterviewController {
    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewDetailResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateInterviewRequest request) {
        return interviewService.create(userId(jwt), request);
    }

    @GetMapping
    public PagedResponse<InterviewSummary> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "application_id", required = false) UUID applicationId,
            @RequestParam(required = false) Boolean upcoming,
            @RequestParam(name = "round_type", required = false) InterviewRoundType roundType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "scheduled_at") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return interviewService.list(userId(jwt), applicationId, upcoming, roundType, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    public InterviewDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return interviewService.getById(userId(jwt), id);
    }

    @PatchMapping("/{id}")
    public InterviewDetailResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateInterviewRequest request) {
        return interviewService.update(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        interviewService.delete(userId(jwt), id);
    }

    @GetMapping("/{id}/questions")
    public List<InterviewQuestionResponse> listQuestions(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return interviewService.listQuestions(userId(jwt), id);
    }

    @PostMapping("/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewQuestionResponse addQuestion(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody CreateInterviewQuestionRequest request) {
        return interviewService.addQuestion(userId(jwt), id, request);
    }

    @GetMapping("/{id}/feedback")
    public InterviewFeedbackResponse getFeedback(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return interviewService.getFeedback(userId(jwt), id);
    }

    @PutMapping("/{id}/feedback")
    public InterviewFeedbackResponse upsertFeedback(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody InterviewFeedbackRequest request) {
        return interviewService.upsertFeedback(userId(jwt), id, request);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
