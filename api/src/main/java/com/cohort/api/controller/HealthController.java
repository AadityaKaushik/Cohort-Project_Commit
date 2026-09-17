package com.cohort.api.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unversioned liveness probe, deliberately outside /api/v1 — infra checks
 * (load balancers, container orchestrators, docker-compose healthchecks)
 * shouldn't depend on the API's own versioning scheme.
 *
 * This currently reports process liveness only. FR-97 (reporting database
 * and FL-service reachability) is extended here once those dependencies
 * exist, starting with the database in Sprint 1 / P04-P05.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
