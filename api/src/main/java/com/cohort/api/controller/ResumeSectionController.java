package com.cohort.api.controller;

import com.cohort.api.dto.ReorderResumeSectionsRequest;
import com.cohort.api.dto.ResumeSectionRequest;
import com.cohort.api.dto.ResumeSectionResponse;
import com.cohort.api.service.ResumeSectionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumes/{resumeId}/sections")
public class ResumeSectionController {
    private final ResumeSectionService sectionService;

    public ResumeSectionController(ResumeSectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping
    public List<ResumeSectionResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID resumeId) {
        return sectionService.list(userId(jwt), resumeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeSectionResponse add(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID resumeId, @Valid @RequestBody ResumeSectionRequest request) {
        return sectionService.add(userId(jwt), resumeId, request);
    }

    @PutMapping("/{sectionId}")
    public ResumeSectionResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID resumeId,
            @PathVariable UUID sectionId,
            @Valid @RequestBody ResumeSectionRequest request) {
        return sectionService.update(userId(jwt), resumeId, sectionId, request);
    }

    @DeleteMapping("/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID resumeId, @PathVariable UUID sectionId) {
        sectionService.delete(userId(jwt), resumeId, sectionId);
    }

    @PutMapping("/order")
    public List<ResumeSectionResponse> reorder(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID resumeId, @Valid @RequestBody ReorderResumeSectionsRequest request) {
        return sectionService.reorder(userId(jwt), resumeId, request);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
