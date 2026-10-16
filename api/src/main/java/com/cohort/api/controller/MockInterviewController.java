package com.cohort.api.controller;

import com.cohort.api.dto.MockInterviewResponse;
import com.cohort.api.service.preparation.mock.MockInterviewService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prep")
public class MockInterviewController {
    private final MockInterviewService mockInterviewService;

    public MockInterviewController(MockInterviewService mockInterviewService) {
        this.mockInterviewService = mockInterviewService;
    }

    @GetMapping("/mock-interview")
    public MockInterviewResponse mockInterview(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "5") int count) {
        return mockInterviewService.build(UUID.fromString(jwt.getSubject()), count);
    }
}
