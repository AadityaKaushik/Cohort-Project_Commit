package com.cohort.api.controller;

import com.cohort.api.dto.ApplicationDetailResponse;
import com.cohort.api.dto.ApplicationSearchCriteria;
import com.cohort.api.dto.ApplicationStatusHistoryResponse;
import com.cohort.api.dto.ApplicationSummary;
import com.cohort.api.dto.AttachResumeRequest;
import com.cohort.api.dto.ChangeStatusRequest;
import com.cohort.api.dto.CreateApplicationRequest;
import com.cohort.api.dto.PagedResponse;
import com.cohort.api.dto.StalledApplicationResponse;
import com.cohort.api.dto.UpdateApplicationRequest;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.ApplicationService;
import com.cohort.api.service.application.StallDetectionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
@RequestMapping("/api/v1/applications")
public class ApplicationController {
    private final ApplicationService applicationService;
    private final StallDetectionService stallDetectionService;

    public ApplicationController(ApplicationService applicationService, StallDetectionService stallDetectionService) {
        this.applicationService = applicationService;
        this.stallDetectionService = stallDetectionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDetailResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateApplicationRequest request) {
        return applicationService.create(userId(jwt), request);
    }

    @GetMapping
    public PagedResponse<ApplicationSummary> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "date_applied") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) ApplicationStatus stage,
            @RequestParam(required = false) Application.Priority priority,
            @RequestParam(name = "work_mode", required = false) Application.WorkMode workMode,
            @RequestParam(required = false) String tag,
            @RequestParam(name = "date_from", required = false) LocalDate dateFrom,
            @RequestParam(name = "date_to", required = false) LocalDate dateTo,
            @RequestParam(required = false) String q) {
        ApplicationSearchCriteria criteria =
                new ApplicationSearchCriteria(status, stage, priority, workMode, tag, dateFrom, dateTo, q);
        return applicationService.list(userId(jwt), criteria, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    public ApplicationDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return applicationService.getById(userId(jwt), id);
    }

    @PatchMapping("/{id}")
    public ApplicationDetailResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateApplicationRequest request) {
        return applicationService.update(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        applicationService.delete(userId(jwt), id);
    }

    @PostMapping("/{id}/status")
    public ApplicationDetailResponse changeStatus(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        return applicationService.changeStatus(userId(jwt), id, request);
    }

    @PostMapping("/{id}/resume")
    public ApplicationDetailResponse attachResume(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody AttachResumeRequest request) {
        return applicationService.attachResume(userId(jwt), id, request.resumeId());
    }

    @GetMapping("/{id}/history")
    public List<ApplicationStatusHistoryResponse> history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return applicationService.history(userId(jwt), id);
    }

    @GetMapping("/stalled")
    public List<StalledApplicationResponse> stalled(@AuthenticationPrincipal Jwt jwt) {
        return stallDetectionService.detectStalled(userId(jwt)).stream()
                .map(StalledApplicationResponse::from)
                .toList();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
