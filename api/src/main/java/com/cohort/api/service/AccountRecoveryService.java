package com.cohort.api.service;

import com.cohort.api.entity.AuthToken;
import com.cohort.api.entity.AuthToken.Purpose;
import com.cohort.api.entity.User;
import com.cohort.api.exception.InvalidAuthTokenException;
import com.cohort.api.repository.AuthTokenRepository;
import com.cohort.api.repository.RefreshTokenRepository;
import com.cohort.api.repository.UserRepository;
import com.cohort.api.security.PasswordPolicyValidator;
import com.cohort.api.service.mail.MailSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-92: password reset and email verification over single-use, expiring tokens. */
@Service
public class AccountRecoveryService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final MailSender mailSender;
    private final String frontendBaseUrl;
    private final Duration resetTtl;
    private final Duration verificationTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountRecoveryService(
            UserRepository userRepository,
            AuthTokenRepository authTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            MailSender mailSender,
            @Value("${app.frontend-base-url}") String frontendBaseUrl,
            @Value("${app.auth.password-reset-ttl-minutes}") long resetTtlMinutes,
            @Value("${app.auth.email-verification-ttl-hours}") long verificationTtlHours) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.mailSender = mailSender;
        this.frontendBaseUrl = frontendBaseUrl;
        this.resetTtl = Duration.ofMinutes(resetTtlMinutes);
        this.verificationTtl = Duration.ofHours(verificationTtlHours);
    }

    /** Silent for unknown emails so the response never reveals whether an account exists. */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String raw = issue(user, Purpose.password_reset, resetTtl);
            mailSender.send(user.getEmail(), "Reset your Cohort password",
                    "Use this link to choose a new password: " + frontendBaseUrl + "/reset-password?token=" + raw);
        });
    }

    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        passwordPolicyValidator.validate(newPassword);
        AuthToken token = consume(rawToken, Purpose.password_reset);
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
    }

    @Transactional
    public void sendVerification(User user) {
        String raw = issue(user, Purpose.email_verification, verificationTtl);
        mailSender.send(user.getEmail(), "Verify your Cohort email",
                "Confirm your email address: " + frontendBaseUrl + "/verify-email?token=" + raw);
    }

    @Transactional
    public void requestVerification(String email) {
        userRepository.findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerification);
    }

    @Transactional
    public void confirmVerification(String rawToken) {
        AuthToken token = consume(rawToken, Purpose.email_verification);
        User user = token.getUser();
        user.markEmailVerified();
        userRepository.save(user);
    }

    private String issue(User user, Purpose purpose, Duration ttl) {
        Instant now = Instant.now();
        authTokenRepository.invalidateOutstanding(user.getId(), purpose, now);
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        authTokenRepository.save(new AuthToken(user, purpose, hash(raw), now.plus(ttl)));
        return raw;
    }

    private AuthToken consume(String rawToken, Purpose purpose) {
        Instant now = Instant.now();
        AuthToken token = authTokenRepository.findByTokenHashAndPurpose(hash(rawToken), purpose)
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(InvalidAuthTokenException::new);
        token.markUsed(now);
        return token;
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
