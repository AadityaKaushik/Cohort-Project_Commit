package com.cohort.api.service;

import com.cohort.api.entity.RefreshToken;
import com.cohort.api.entity.User;
import com.cohort.api.exception.InvalidRefreshTokenException;
import com.cohort.api.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Access tokens (stateless JWTs) and refresh tokens (opaque, DB-backed, and
 * therefore individually revocable — the whole reason FR-06 is possible)
 * are deliberately different mechanisms; this class is the one place that
 * knows how to mint and validate both.
 */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(
            JwtEncoder jwtEncoder,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes,
            @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenTtl = Duration.ofMinutes(accessTokenTtlMinutes);
        this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cohort-api")
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();
        // A symmetric key has no key ID to disambiguate, so the header's
        // algorithm must be stated explicitly rather than left to default
        // JWK selection (which assumes an asymmetric RS256 key).
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Transactional
    public String issueRefreshToken(User user) {
        String rawToken = generateRawToken();
        RefreshToken entity = new RefreshToken(user, hash(rawToken), Instant.now().plus(refreshTokenTtl));
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /** Validates, revokes, and returns the token in one step — refresh tokens are single-use. */
    @Transactional
    public RefreshToken consumeRefreshToken(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (token.isRevoked() || token.isExpired()) {
            throw new InvalidRefreshTokenException();
        }
        token.revoke();
        return token;
    }

    @Transactional
    public void revokeIfPresent(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available on any JVM", e);
        }
    }
}
