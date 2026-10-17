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
import com.cohort.api.exception.EmailNotVerifiedException;
import com.cohort.api.exception.InvalidAuthTokenException;
import com.cohort.api.exception.InvalidCredentialsException;
import com.cohort.api.repository.UserRepository;
import com.cohort.api.security.LoginRateLimiter;
import com.cohort.api.security.PasswordPolicyValidator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
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
    private final AccountRecoveryService accountRecoveryService;
    private final boolean emailVerificationRequired;

    // Fixed hash so an unknown email costs the same Argon2 check as a real one.
    private final String dummyHashForTimingSafety;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        PasswordPolicyValidator passwordPolicyValidator, TokenService tokenService,
                        LoginRateLimiter loginRateLimiter,
                        AccountRecoveryService accountRecoveryService,
                        @Value("${app.auth.email-verification-required}") boolean emailVerificationRequired) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.tokenService = tokenService;
        this.loginRateLimiter = loginRateLimiter;
        this.accountRecoveryService = accountRecoveryService;
        this.emailVerificationRequired = emailVerificationRequired;
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
        accountRecoveryService.sendVerification(user);
        if (emailVerificationRequired) {
            return AuthResponse.awaitingVerification(UserSummary.from(user));
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
        User user = maybeUser.get();
        // Checked after the password, so this never reveals whether an address is registered.
        if (emailVerificationRequired && !user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
        if (passwordEncoder.upgradeEncoding(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            userRepository.save(user);
        }
        return issueAuthResponse(user);
    }

    @Transactional(noRollbackFor = InvalidAuthTokenException.class)
    public AuthResponse confirmEmail(String email, String code) {
        return issueAuthResponse(accountRecoveryService.confirmVerification(email, code));
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
        return AuthResponse.signedIn(UserSummary.from(user), accessToken, refreshToken);
    }
}
