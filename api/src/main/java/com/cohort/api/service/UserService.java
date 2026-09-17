package com.cohort.api.service;

import com.cohort.api.dto.UpdateProfileRequest;
import com.cohort.api.dto.UserProfileResponse;
import com.cohort.api.entity.User;
import com.cohort.api.exception.EmailAlreadyInUseException;
import com.cohort.api.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return UserProfileResponse.from(requireUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = requireUser(userId);

        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new EmailAlreadyInUseException();
            }
            user.setEmail(request.email());
        }
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.targetRole() != null) {
            user.setTargetRole(request.targetRole());
        }
        if (request.graduationYear() != null) {
            user.setGraduationYear(request.graduationYear());
        }
        if (request.timezone() != null) {
            user.setTimezone(request.timezone());
        }

        return UserProfileResponse.from(user);
    }

    private User requireUser(UUID userId) {
        // The caller's identity comes from a just-validated JWT subject, so
        // a missing row here means the account was deleted after the token
        // was issued — not a client error worth its own exception type.
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user " + userId + " no longer exists"));
    }
}
