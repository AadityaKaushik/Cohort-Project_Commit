package com.cohort.api.controller;

import com.cohort.api.dto.TopicGraphResponse;
import com.cohort.api.dto.TopicResponse;
import com.cohort.api.service.PreparationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-38: read-only access to the shared, system-owned topic taxonomy — no
 * {@code @AuthenticationPrincipal} needed beyond the global auth filter,
 * since every authenticated user sees the same topics.
 */
@RestController
@RequestMapping("/api/v1/topics")
public class TopicController {

    private final PreparationService preparationService;

    public TopicController(PreparationService preparationService) {
        this.preparationService = preparationService;
    }

    @GetMapping
    public List<TopicResponse> list() {
        return preparationService.listTopics();
    }

    @GetMapping("/graph")
    public TopicGraphResponse graph() {
        return preparationService.topicGraph();
    }
}
