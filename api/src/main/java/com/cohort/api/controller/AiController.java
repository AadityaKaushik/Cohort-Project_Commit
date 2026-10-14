package com.cohort.api.controller;

import com.cohort.api.service.ai.AiService;
import com.cohort.api.service.ai.AiStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/status")
    public AiStatus status() {
        return aiService.status();
    }
}
