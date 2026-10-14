package com.cohort.api.service.ai;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cohort.api.exception.AiRateLimitExceededException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiRateLimiterTest {
    @Test
    void blocksAUserOverTheLimitWithoutAffectingOthers() {
        AiRateLimiter limiter = new AiRateLimiter(2, 60);
        UUID busy = UUID.randomUUID();
        UUID quiet = UUID.randomUUID();

        limiter.acquire(busy);
        limiter.acquire(busy);

        assertThatThrownBy(() -> limiter.acquire(busy)).isInstanceOf(AiRateLimitExceededException.class);
        assertThatCode(() -> limiter.acquire(quiet)).doesNotThrowAnyException();
    }
}
