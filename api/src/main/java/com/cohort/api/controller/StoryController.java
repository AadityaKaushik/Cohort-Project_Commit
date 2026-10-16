package com.cohort.api.controller;

import com.cohort.api.dto.CreateStoryRequest;
import com.cohort.api.dto.StoryResponse;
import com.cohort.api.dto.UpdateStoryRequest;
import com.cohort.api.service.StoryService;
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

@RestController
@RequestMapping("/api/v1/stories")
public class StoryController {

    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @GetMapping
    public List<StoryResponse> list(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String competency) {
        return storyService.list(userId(jwt), competency);
    }

    @GetMapping("/{id}")
    public StoryResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return storyService.get(userId(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoryResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateStoryRequest request) {
        return storyService.create(userId(jwt), request);
    }

    @PatchMapping("/{id}")
    public StoryResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateStoryRequest request) {
        return storyService.update(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        storyService.delete(userId(jwt), id);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
