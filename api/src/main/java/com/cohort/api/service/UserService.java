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
    private final AccountRecoveryService accountRecoveryService;

    public UserService(UserRepository userRepository, AccountRecoveryService accountRecoveryService) {
        this.userRepository = userRepository;
        this.accountRecoveryService = accountRecoveryService;
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
            accountRecoveryService.sendVerificationToNewAddress(user);
        }
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.targetRole() != null) {
            user.setTargetRole(request.targetRole());
        }
        if (request.targetDate() != null) {
            user.setTargetDate(request.targetDate());
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
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user " + userId + " no longer exists"));
    }
}
