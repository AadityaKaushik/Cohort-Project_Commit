package com.cohort.api.controller;

import com.cohort.api.dto.ChangePasswordRequest;
import com.cohort.api.dto.OnboardingRequest;
import com.cohort.api.dto.OnboardingResponse;
import com.cohort.api.service.OnboardingService;
import com.cohort.api.dto.DeleteAccountRequest;
import com.cohort.api.dto.UpdateProfileRequest;
import com.cohort.api.dto.UserProfileResponse;
import com.cohort.api.service.UserService;
import com.cohort.api.service.account.AccountService;
import com.cohort.api.service.account.DataExportService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final OnboardingService onboardingService;
    private final AccountService accountService;
    private final DataExportService dataExportService;

    public UserController(UserService userService, AccountService accountService, DataExportService dataExportService,
                          OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
        this.userService = userService;
        this.accountService = accountService;
        this.dataExportService = dataExportService;
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(userId(jwt));
    }

    @PatchMapping("/me")
    public UserProfileResponse updateMe(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId(jwt), request);
    }

    @PostMapping("/me/onboarding")
    public OnboardingResponse completeOnboarding(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody OnboardingRequest request) {
        return onboardingService.complete(userId(jwt), request);
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(userId(jwt), request);
    }

    @GetMapping("/me/export")
    public ResponseEntity<Map<String, Object>> exportData(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("cohort-data-export.json").build().toString())
                .body(dataExportService.export(userId(jwt)));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteAccount(userId(jwt), request);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
