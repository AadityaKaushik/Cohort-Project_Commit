package com.cohort.api.service;

import com.cohort.api.entity.AuthToken;
import com.cohort.api.entity.AuthToken.Purpose;
import com.cohort.api.entity.User;
import com.cohort.api.exception.InvalidAuthTokenException;
import com.cohort.api.repository.AuthTokenRepository;
import com.cohort.api.repository.RefreshTokenRepository;
import com.cohort.api.repository.UserRepository;
import com.cohort.api.security.LoginRateLimiter;
import com.cohort.api.security.PasswordPolicyValidator;
import com.cohort.api.service.mail.MailSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Six-digit emailed codes for confirming an email address and for resetting a password.
 * Codes are stored salted and hashed, expire, work once, and stop working after a few wrong guesses.
 */
@Service
public class AccountRecoveryService {
    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final LoginRateLimiter codeRateLimiter;
    private final MailSender mailSender;
    private final Duration codeTtl;
    private final int maxAttempts;
    private final Duration resendCooldown;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountRecoveryService(
            UserRepository userRepository,
            AuthTokenRepository authTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            LoginRateLimiter codeRateLimiter,
            MailSender mailSender,
            @Value("${app.auth.code.ttl-minutes}") long codeTtlMinutes,
            @Value("${app.auth.code.max-attempts}") int maxAttempts,
            @Value("${app.auth.code.resend-cooldown-seconds}") long resendCooldownSeconds) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.codeRateLimiter = codeRateLimiter;
        this.mailSender = mailSender;
        this.codeTtl = Duration.ofMinutes(codeTtlMinutes);
        this.maxAttempts = maxAttempts;
        this.resendCooldown = Duration.ofSeconds(resendCooldownSeconds);
    }

    // Silent for unknown emails so the response never reveals whether an account exists.
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> sendCode(user, Purpose.password_reset, false));
    }

    @Transactional(noRollbackFor = InvalidAuthTokenException.class)
    public void confirmPasswordReset(String email, String code, String newPassword) {
        passwordPolicyValidator.validate(newPassword);
        User user = consume(email, code, Purpose.password_reset);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Receiving the code proves the user owns the address.
        if (!user.isEmailVerified()) {
            user.markEmailVerified();
        }
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
    }

    @Transactional
    public void sendVerification(User user) {
        sendCode(user, Purpose.email_verification, false);
    }

    /** After an email change; the cooldown is for the old address, so it does not apply. */
    @Transactional
    public void sendVerificationToNewAddress(User user) {
        sendCode(user, Purpose.email_verification, true);
    }

    @Transactional
    public void requestVerification(String email) {
        userRepository.findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerification);
    }

    @Transactional(noRollbackFor = InvalidAuthTokenException.class)
    public User confirmVerification(String email, String code) {
        User user = consume(email, code, Purpose.email_verification);
        user.markEmailVerified();
        return userRepository.save(user);
    }

    private void sendCode(User user, Purpose purpose, boolean ignoreCooldown) {
        Instant now = Instant.now();
        boolean sentRecently = !ignoreCooldown && authTokenRepository
                .findFirstByUser_IdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose)
                .map(last -> last.getCreatedAt() != null && last.getCreatedAt().isAfter(now.minus(resendCooldown)))
                .orElse(false);
        if (sentRecently) {
            return;
        }
        authTokenRepository.invalidateOutstanding(user.getId(), purpose, now);
        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        authTokenRepository.save(new AuthToken(user, purpose, saltedHash(code), now.plus(codeTtl)));
        mailSender.send(user.getEmail(), subject(purpose), body(purpose, code));
    }

    private User consume(String email, String code, Purpose purpose) {
        String limiterKey = purpose + ":" + email;
        codeRateLimiter.checkAllowed(limiterKey);
        Instant now = Instant.now();
        User user = userRepository.findByEmail(email).orElse(null);
        AuthToken token = user == null ? null : authTokenRepository
                .findFirstByUser_IdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .filter(candidate -> candidate.isUsable(now))
                .orElse(null);
        if (token == null || !matches(token.getTokenHash(), code)) {
            if (token != null) {
                token.recordFailedAttempt(maxAttempts, now);
                authTokenRepository.save(token);
            }
            codeRateLimiter.recordFailure(limiterKey);
            throw new InvalidAuthTokenException();
        }
        token.markUsed(now);
        codeRateLimiter.recordSuccess(limiterKey);
        return user;
    }

    private static String subject(Purpose purpose) {
        return purpose == Purpose.password_reset ? "Your Cohort password reset code" : "Your Cohort sign-up code";
    }

    private String body(Purpose purpose, String code) {
        String action = purpose == Purpose.password_reset ? "reset your password" : "confirm your email";
        return """
                Your code to %s is %s

                It works once and expires in %d minutes. If you did not ask for it, you can ignore this email.

                Cohort""".formatted(action, code, codeTtl.toMinutes());
    }

    // Stored as salt$hash, so equal codes never collide and a leaked table does not list codes.
    private String saltedHash(String code) {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        String saltText = Base64.getUrlEncoder().withoutPadding().encodeToString(salt);
        return saltText + "$" + sha256(saltText + ":" + code);
    }

    private boolean matches(String stored, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        int separator = stored.indexOf('$');
        if (separator < 0) {
            return false;
        }
        String expected = stored.substring(separator + 1);
        String actual = sha256(stored.substring(0, separator) + ":" + code);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available on any JVM", e);
        }
    }
}
