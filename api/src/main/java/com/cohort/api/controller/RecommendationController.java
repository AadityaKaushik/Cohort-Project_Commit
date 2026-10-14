package com.cohort.api.controller;

import com.cohort.api.dto.DismissRecommendationRequest;
import com.cohort.api.dto.RecommendationResponse;
import com.cohort.api.service.recommendation.RecommendationFacade;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {
    private final RecommendationFacade facade;

    public RecommendationController(RecommendationFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public List<RecommendationResponse> list(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "false") boolean federated) {
        return facade.generateRecommendations(userId(jwt), federated);
    }

    @GetMapping("/handled")
    public List<RecommendationResponse> handled(@AuthenticationPrincipal Jwt jwt) {
        return facade.listHandled(userId(jwt));
    }

    @PostMapping("/{id}/restore")
    public RecommendationResponse restore(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return facade.restore(userId(jwt), id);
    }

    @PostMapping("/{id}/accept")
    public RecommendationResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return facade.accept(userId(jwt), id);
    }

    @PostMapping("/{id}/snooze")
    public RecommendationResponse snooze(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return facade.snooze(userId(jwt), id);
    }

    @PostMapping("/{id}/dismiss")
    public RecommendationResponse dismiss(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody DismissRecommendationRequest request) {
        return facade.dismiss(userId(jwt), id, request.reason());
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
