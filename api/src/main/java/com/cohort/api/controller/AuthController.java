package com.cohort.api.controller;

import com.cohort.api.dto.AuthResponse;
import com.cohort.api.dto.EmailVerificationConfirmRequest;
import com.cohort.api.dto.EmailVerificationRequest;
import com.cohort.api.dto.LoginRequest;
import com.cohort.api.dto.PasswordResetConfirmRequest;
import com.cohort.api.dto.PasswordResetRequest;
import com.cohort.api.dto.RefreshRequest;
import com.cohort.api.dto.RegisterRequest;
import com.cohort.api.dto.TokenPairResponse;
import com.cohort.api.service.AccountRecoveryService;
import com.cohort.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountRecoveryService accountRecoveryService;

    public AuthController(AuthService authService, AccountRecoveryService accountRecoveryService) {
        this.authService = authService;
        this.accountRecoveryService = accountRecoveryService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        accountRecoveryService.requestPasswordReset(request.email());
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        accountRecoveryService.confirmPasswordReset(request.email(), request.code(), request.newPassword());
    }

    @PostMapping("/email-verification/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestEmailVerification(@Valid @RequestBody EmailVerificationRequest request) {
        accountRecoveryService.requestVerification(request.email());
    }

    /** A correct code confirms the address and signs the user in. */
    @PostMapping("/email-verification/confirm")
    public AuthResponse confirmEmailVerification(@Valid @RequestBody EmailVerificationConfirmRequest request) {
        return authService.confirmEmail(request.email(), request.code());
    }
}
