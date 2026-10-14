package com.cohort.api.service.ai;

import com.cohort.api.exception.AiRateLimitExceededException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiRateLimiter {
    private final int maxRequests;
    private final Duration window;
    private final ConcurrentHashMap<UUID, Deque<Instant>> requestsByUser = new ConcurrentHashMap<>();

    public AiRateLimiter(
            @Value("${app.ai.rate-limit.max-requests}") int maxRequests,
            @Value("${app.ai.rate-limit.window-minutes}") long windowMinutes) {
        this.maxRequests = maxRequests;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    public void acquire(UUID userId) {
        Deque<Instant> requests = requestsByUser.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (requests) {
            Instant cutoff = Instant.now().minus(window);
            while (!requests.isEmpty() && requests.peekFirst().isBefore(cutoff)) {
                requests.pollFirst();
            }
            if (requests.size() >= maxRequests) {
                throw new AiRateLimitExceededException();
            }
            requests.addLast(Instant.now());
        }
    }
}
