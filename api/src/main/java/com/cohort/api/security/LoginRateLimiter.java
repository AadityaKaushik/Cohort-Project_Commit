package com.cohort.api.security;

import com.cohort.api.exception.RateLimitExceededException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory sliding-window limiter, keyed by email, for failed login
 * attempts. Single-instance only — acceptable for this project's scope;
 * a multi-instance deployment would need a shared store (e.g. Redis)
 * instead.
 */
@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final ConcurrentHashMap<String, Deque<Instant>> failuresByEmail = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${app.auth.login-rate-limit.max-attempts}") int maxAttempts,
            @Value("${app.auth.login-rate-limit.window-minutes}") long windowMinutes) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    public void checkAllowed(String email) {
        Deque<Instant> failures = failuresByEmail.get(normalize(email));
        if (failures == null) {
            return;
        }
        synchronized (failures) {
            evictExpired(failures);
            if (failures.size() >= maxAttempts) {
                throw new RateLimitExceededException();
            }
        }
    }

    public void recordFailure(String email) {
        Deque<Instant> failures = failuresByEmail.computeIfAbsent(normalize(email), key -> new ArrayDeque<>());
        synchronized (failures) {
            evictExpired(failures);
            failures.addLast(Instant.now());
        }
    }

    public void recordSuccess(String email) {
        failuresByEmail.remove(normalize(email));
    }

    private void evictExpired(Deque<Instant> failures) {
        Instant cutoff = Instant.now().minus(window);
        while (!failures.isEmpty() && failures.peekFirst().isBefore(cutoff)) {
            failures.pollFirst();
        }
    }

    private String normalize(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
