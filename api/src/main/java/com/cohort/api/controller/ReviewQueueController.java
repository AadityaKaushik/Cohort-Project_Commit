package com.cohort.api.controller;

import com.cohort.api.dto.ReviewQueueResponse;
import com.cohort.api.service.preparation.review.ReviewQueueService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prep")
public class ReviewQueueController {
    private final ReviewQueueService reviewQueueService;

    public ReviewQueueController(ReviewQueueService reviewQueueService) {
        this.reviewQueueService = reviewQueueService;
    }

    @GetMapping("/review-queue")
    public ReviewQueueResponse reviewQueue(@AuthenticationPrincipal Jwt jwt) {
        return reviewQueueService.queue(UUID.fromString(jwt.getSubject()));
    }
}
