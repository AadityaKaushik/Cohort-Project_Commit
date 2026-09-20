package com.cohort.api.controller;

import com.cohort.api.dto.CreatePreparationTaskRequest;
import com.cohort.api.dto.PreparationProgressResponse;
import com.cohort.api.dto.PreparationTaskResponse;
import com.cohort.api.dto.UpdatePreparationTaskRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** US-40/41: preparation tasks and the mastery-driven progress view. Thin by design (ARCHITECTURE_REVISION_JAVA.md §4.2). */
@RestController
@RequestMapping("/api/v1/preparation")
public class PreparationController {

    private final PreparationService preparationService;

    public PreparationController(PreparationService preparationService) {
        this.preparationService = preparationService;
    }

    @GetMapping("/tasks")
    public List<PreparationTaskResponse> listTasks(@AuthenticationPrincipal Jwt jwt) {
        return preparationService.listTasks(userId(jwt));
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public PreparationTaskResponse createTask(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreatePreparationTaskRequest request) {
        return preparationService.createTask(userId(jwt), request);
    }

    @PatchMapping("/tasks/{id}")
    public PreparationTaskResponse updateTask(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody UpdatePreparationTaskRequest request) {
        return preparationService.updateTask(userId(jwt), id, request);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        preparationService.deleteTask(userId(jwt), id);
    }

    @GetMapping("/progress")
    public PreparationProgressResponse progress(@AuthenticationPrincipal Jwt jwt) {
        return preparationService.progress(userId(jwt));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
