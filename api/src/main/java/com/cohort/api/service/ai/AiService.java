package com.cohort.api.service.ai;

import com.cohort.api.entity.AiUsage;
import com.cohort.api.exception.AiUnavailableException;
import com.cohort.api.repository.AiUsageRepository;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class AiService {
    private final AiProvider provider;
    private final AiRateLimiter rateLimiter;
    private final AiUsageRepository usageRepository;

    public AiService(AiProvider provider, AiRateLimiter rateLimiter, AiUsageRepository usageRepository) {
        this.provider = provider;
        this.rateLimiter = rateLimiter;
        this.usageRepository = usageRepository;
    }

    public AiStatus status() {
        return new AiStatus(provider.id(), provider.hosted());
    }

    public String complete(UUID userId, String feature, AiPrompt prompt) {
        rateLimiter.acquire(userId);
        long started = System.nanoTime();
        try {
            String reply = provider.complete(prompt);
            record(userId, feature, prompt, true, reply.length(), started);
            return reply;
        } catch (AiUnavailableException ex) {
            record(userId, feature, prompt, false, 0, started);
            throw ex;
        }
    }

    public void stream(UUID userId, String feature, AiPrompt prompt, Consumer<String> onToken) {
        rateLimiter.acquire(userId);
        long started = System.nanoTime();
        int[] produced = {0};
        try {
            provider.stream(prompt, token -> {
                produced[0] += token.length();
                onToken.accept(token);
            });
            record(userId, feature, prompt, true, produced[0], started);
        } catch (AiUnavailableException ex) {
            record(userId, feature, prompt, false, produced[0], started);
            throw ex;
        }
    }

    private void record(UUID userId, String feature, AiPrompt prompt, boolean succeeded, int responseChars, long started) {
        int elapsedMs = (int) ((System.nanoTime() - started) / 1_000_000);
        usageRepository.save(new AiUsage(
                userId, feature, provider.id(), succeeded, prompt.characterCount(), responseChars, elapsedMs));
    }
}
