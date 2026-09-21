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

/**
 * US-51..56,77: every recommendations endpoint delegates straight to
 * {@link RecommendationFacade} -- thin by design
 * (ARCHITECTURE_REVISION_JAVA.md §4.2), the same way {@code PreparationController}
 * and every other controller in this codebase is.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationFacade facade;

    public RecommendationController(RecommendationFacade facade) {
        this.facade = facade;
    }

    /** {@code federated}: US-65's toggle -- see RecommendationFacade#generateRecommendations(UUID, boolean) for what it does and how P53's circuit breaker keeps it from ever turning into a 500. */
    @GetMapping
    public List<RecommendationResponse> list(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "false") boolean federated) {
        return facade.generateRecommendations(userId(jwt), federated);
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
