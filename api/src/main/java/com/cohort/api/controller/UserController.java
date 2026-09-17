package com.cohort.api.controller;

import com.cohort.api.dto.UpdateProfileRequest;
import com.cohort.api.dto.UserProfileResponse;
import com.cohort.api.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
