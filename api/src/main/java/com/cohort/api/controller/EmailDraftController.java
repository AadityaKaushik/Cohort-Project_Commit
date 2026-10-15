package com.cohort.api.controller;

import com.cohort.api.dto.EmailDraftRequest;
import com.cohort.api.dto.EmailDraftResponse;
import com.cohort.api.service.ai.EmailDraftService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/email-drafts")
public class EmailDraftController {
    private final EmailDraftService draftService;

    public EmailDraftController(EmailDraftService draftService) {
        this.draftService = draftService;
    }

    @PostMapping
    public EmailDraftResponse draft(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId, @Valid @RequestBody EmailDraftRequest request) {
        return draftService.draft(UUID.fromString(jwt.getSubject()), applicationId, request);
    }
}
