package com.cohort.api.service.fl;

import com.cohort.api.entity.User;
import com.cohort.api.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlConsentService {
    private final UserRepository userRepository;

    public FlConsentService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isOptedIn(UUID userId) {
        return findUser(userId).isFlOptIn();
    }

    @Transactional
    public boolean optIn(UUID userId) {
        User user = findUser(userId);
        user.setFlOptIn(true);
        return user.isFlOptIn();
    }

    @Transactional
    public boolean optOut(UUID userId) {
        User user = findUser(userId);
        user.setFlOptIn(false);
        return user.isFlOptIn();
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user " + userId + " no longer exists"));
    }
}
