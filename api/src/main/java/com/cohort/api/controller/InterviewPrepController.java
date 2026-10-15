package com.cohort.api.controller;

import com.cohort.api.dto.AiDraftResponse;
import com.cohort.api.dto.PrepPackResponse;
import com.cohort.api.service.ai.InterviewPrepService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/prep-pack")
public class InterviewPrepController {
    private final InterviewPrepService prepService;

    public InterviewPrepController(InterviewPrepService prepService) {
        this.prepService = prepService;
    }

    @GetMapping
    public PrepPackResponse pack(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID interviewId) {
        return prepService.pack(UUID.fromString(jwt.getSubject()), interviewId);
    }

    @PostMapping("/summary")
    public AiDraftResponse summary(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID interviewId) {
        return prepService.summary(UUID.fromString(jwt.getSubject()), interviewId);
    }
}
