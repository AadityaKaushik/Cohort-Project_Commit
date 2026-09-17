package com.cohort.api.service;

import com.cohort.api.dto.AuthResponse;
import com.cohort.api.dto.LoginRequest;
import com.cohort.api.dto.RefreshRequest;
import com.cohort.api.dto.RegisterRequest;
import com.cohort.api.dto.TokenPairResponse;
import com.cohort.api.dto.UserSummary;
import com.cohort.api.entity.RefreshToken;
import com.cohort.api.entity.User;
import com.cohort.api.exception.EmailAlreadyRegisteredException;
import com.cohort.api.exception.InvalidCredentialsException;
import com.cohort.api.repository.UserRepository;
import com.cohort.api.security.LoginRateLimiter;
import com.cohort.api.security.PasswordPolicyValidator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final TokenService tokenService;
    private final LoginRateLimiter loginRateLimiter;

    /**
     * Computed once at startup so a login attempt against a non-existent
     * email still pays the same Argon2 verification cost as a real one —
     * otherwise the response time itself would reveal whether the account
     * exists (FR-02).
     */
    private final String dummyHashForTimingSafety;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        PasswordPolicyValidator passwordPolicyValidator, TokenService tokenService,
                        LoginRateLimiter loginRateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.tokenService = tokenService;
        this.loginRateLimiter = loginRateLimiter;
        this.dummyHashForTimingSafety = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        passwordPolicyValidator.validate(request.password());
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException();
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user;
        try {
            user = userRepository.save(new User(request.email(), passwordHash, request.displayName()));
        } catch (DataIntegrityViolationException raceOnUniqueEmail) {
            throw new EmailAlreadyRegisteredException();
        }
        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        loginRateLimiter.checkAllowed(request.email());

        Optional<User> maybeUser = userRepository.findByEmail(request.email());
        boolean passwordMatches = maybeUser
                .map(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .orElseGet(() -> {
                    passwordEncoder.matches(request.password(), dummyHashForTimingSafety);
                    return false;
                });

        if (maybeUser.isEmpty() || !passwordMatches) {
            loginRateLimiter.recordFailure(request.email());
            throw new InvalidCredentialsException();
        }

        loginRateLimiter.recordSuccess(request.email());
        return issueAuthResponse(maybeUser.get());
    }

    @Transactional
    public TokenPairResponse refresh(RefreshRequest request) {
        RefreshToken consumed = tokenService.consumeRefreshToken(request.refreshToken());
        User user = consumed.getUser();
        return new TokenPairResponse(tokenService.issueAccessToken(user), tokenService.issueRefreshToken(user));
    }

    @Transactional
    public void logout(String refreshToken) {
        tokenService.revokeIfPresent(refreshToken);
    }

    private AuthResponse issueAuthResponse(User user) {
        String accessToken = tokenService.issueAccessToken(user);
        String refreshToken = tokenService.issueRefreshToken(user);
        return new AuthResponse(UserSummary.from(user), accessToken, refreshToken);
    }
}
