package com.cohort.api.controller;

import com.cohort.api.dto.AiDraftResponse;
import com.cohort.api.dto.ResumeMatchResponse;
import com.cohort.api.service.ai.ResumeMatchService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}")
public class ResumeMatchController {
    private final ResumeMatchService matchService;

    public ResumeMatchController(ResumeMatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/match")
    public ResumeMatchResponse match(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId) {
        return matchService.match(UUID.fromString(jwt.getSubject()), applicationId);
    }

    @PostMapping("/cover-letter-draft")
    public AiDraftResponse coverLetter(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId) {
        return matchService.coverLetter(UUID.fromString(jwt.getSubject()), applicationId);
    }
}
