package com.cohort.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.cohort.api.entity.RefreshToken;
import com.cohort.api.entity.User;
import com.cohort.api.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class TokenServiceTest {

    private final JwtEncoder jwtEncoder = mock(JwtEncoder.class);
    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);

    private Instant issuedExpiry(long configuredDays) {
        TokenService service = new TokenService(jwtEncoder, repository, 15, configuredDays);
        service.issueRefreshToken(new User("u@example.com", "hash", "U"));
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        return captor.getValue().getExpiresAt();
    }

    @Test
    void refreshTokenLifetimeIsCappedAtSevenDays() {
        Instant expiry = issuedExpiry(30);

        assertThat(Duration.between(Instant.now(), expiry)).isLessThanOrEqualTo(Duration.ofDays(7));
    }

    @Test
    void shorterConfiguredLifetimeIsHonoured() {
        Instant expiry = issuedExpiry(2);

        assertThat(Duration.between(Instant.now(), expiry)).isLessThanOrEqualTo(Duration.ofDays(2));
    }
}
