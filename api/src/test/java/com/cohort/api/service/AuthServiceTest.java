package com.cohort.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cohort.api.dto.LoginRequest;
import com.cohort.api.dto.RegisterRequest;
import com.cohort.api.entity.User;
import com.cohort.api.exception.EmailAlreadyRegisteredException;
import com.cohort.api.exception.InvalidCredentialsException;
import com.cohort.api.exception.WeakPasswordException;
import com.cohort.api.repository.UserRepository;
import com.cohort.api.security.LoginRateLimiter;
import com.cohort.api.security.PasswordPolicyValidator;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private PasswordPolicyValidator passwordPolicyValidator;
    private TokenService tokenService;
    private LoginRateLimiter loginRateLimiter;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        passwordPolicyValidator = mock(PasswordPolicyValidator.class);
        tokenService = mock(TokenService.class);
        loginRateLimiter = mock(LoginRateLimiter.class);

        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(tokenService.issueAccessToken(any())).thenReturn("access-token");
        when(tokenService.issueRefreshToken(any())).thenReturn("refresh-token");

        authService = new AuthService(
                userRepository, passwordEncoder, passwordPolicyValidator, tokenService, loginRateLimiter);
    }

    @Test
    void registerRejectsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("taken@example.com", "a-fine-password", "Someone")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void registerRejectsWeakPasswordBeforeTouchingTheDatabase() {
        var request = new RegisterRequest("new@example.com", "short", "Someone");
        org.mockito.Mockito.doThrow(new WeakPasswordException("too short"))
                .when(passwordPolicyValidator).validate("short");

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(WeakPasswordException.class);
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void registerSucceedsAndIssuesTokens() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(new RegisterRequest("new@example.com", "a-fine-password", "New User"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("new@example.com");
    }

    @Test
    void loginWithUnknownEmailThrowsSameExceptionAsWrongPassword() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        InvalidCredentialsException unknownEmailException = catchInvalidCredentials(
                () -> authService.login(new LoginRequest("ghost@example.com", "whatever12")));

        User existingUser = new User("real@example.com", "stored-hash", "Real User");
        when(userRepository.findByEmail("real@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        InvalidCredentialsException wrongPasswordException = catchInvalidCredentials(
                () -> authService.login(new LoginRequest("real@example.com", "wrong-password")));

        // Same exception type and same message for both — a client cannot
        // tell "no such account" apart from "wrong password" (FR-02).
        assertThat(unknownEmailException.getMessage()).isEqualTo(wrongPasswordException.getMessage());
    }

    @Test
    void loginStillCallsPasswordEncoderWhenEmailIsUnknown() {
        // The dummy-hash comparison must still run so an unknown-email
        // login pays the same Argon2 cost as a real one.
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "whatever12")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder).matches(anyString(), anyString());
    }

    @Test
    void successfulLoginClearsRateLimitState() {
        User user = new User("real@example.com", "stored-hash", "Real User");
        when(userRepository.findByEmail("real@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "stored-hash")).thenReturn(true);

        authService.login(new LoginRequest("real@example.com", "correct-password"));

        verify(loginRateLimiter).recordSuccess("real@example.com");
        verify(loginRateLimiter, never()).recordFailure(anyString());
    }

    private InvalidCredentialsException catchInvalidCredentials(Runnable action) {
        try {
            action.run();
        } catch (InvalidCredentialsException e) {
            return e;
        }
        throw new AssertionError("Expected InvalidCredentialsException to be thrown");
    }
}
