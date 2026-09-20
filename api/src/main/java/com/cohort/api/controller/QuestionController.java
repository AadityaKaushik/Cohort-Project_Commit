package com.cohort.api.controller;

import com.cohort.api.dto.CreatePracticeAttemptRequest;
import com.cohort.api.dto.CreateQuestionRequest;
import com.cohort.api.dto.PagedResponse;
import com.cohort.api.dto.PracticeAttemptResponse;
import com.cohort.api.dto.QuestionResponse;
import com.cohort.api.dto.UpdateQuestionRequest;
import com.cohort.api.entity.Difficulty;
import com.cohort.api.entity.Question;
import com.cohort.api.service.PreparationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** US-35..37: question bank browsing/authoring plus practice attempt recording. Thin by design (ARCHITECTURE_REVISION_JAVA.md §4.2). */
@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final PreparationService preparationService;

    public QuestionController(PreparationService preparationService) {
        this.preparationService = preparationService;
    }

    @GetMapping
    public PagedResponse<QuestionResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Question.Category category,
            @RequestParam(name = "topic_id", required = false) UUID topicId,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(name = "is_system", required = false) Boolean isSystem,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return preparationService.listQuestions(userId(jwt), category, topicId, difficulty, isSystem, q, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateQuestionRequest request) {
        return preparationService.createQuestion(userId(jwt), request);
    }

    @GetMapping("/{id}")
    public QuestionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return preparationService.getVisibleQuestion(userId(jwt), id);
    }

    @PatchMapping("/{id}")
    public QuestionResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody UpdateQuestionRequest request) {
        return preparationService.updateQuestion(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        preparationService.deleteQuestion(userId(jwt), id);
    }

    @PostMapping("/{id}/fork")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse fork(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return preparationService.forkQuestion(userId(jwt), id);
    }

    @PostMapping("/{id}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public PracticeAttemptResponse recordAttempt(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody CreatePracticeAttemptRequest request) {
        return preparationService.recordAttempt(userId(jwt), id, request);
    }

    @GetMapping("/{id}/attempts")
    public List<PracticeAttemptResponse> listAttempts(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return preparationService.listAttempts(userId(jwt), id);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
