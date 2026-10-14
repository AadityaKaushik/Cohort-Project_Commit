package com.cohort.api.controller;

import com.cohort.api.dto.ActivityResponse;
import com.cohort.api.dto.GoalResponse;
import com.cohort.api.dto.SetGoalRequest;
import com.cohort.api.service.GoalService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public List<GoalResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return goalService.list(userId(jwt));
    }

    @PutMapping
    public GoalResponse set(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SetGoalRequest request) {
        return goalService.set(userId(jwt), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        goalService.delete(userId(jwt), id);
    }

    @GetMapping("/activity")
    public ActivityResponse activity(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "26") int weeks) {
        return goalService.activity(userId(jwt), weeks);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
