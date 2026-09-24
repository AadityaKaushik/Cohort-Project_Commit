package com.cohort.api.service.fl;

import com.cohort.api.entity.User;
import com.cohort.api.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-57 (opt in, default off) / US-64 (withdraw, takes effect from the
 * next round): the whole consent contract lives in one boolean,
 * {@code users.fl_opt_in} (V1 migration already declares it
 * {@code NOT NULL DEFAULT false}, so "default off" is enforced at the
 * schema level, not just in application code). {@link FlFeatureExportJob}
 * re-reads {@link UserRepository#findAllByFlOptInTrue()} on every run
 * rather than caching a snapshot, which is what makes withdrawal actually
 * take effect on the very next export cycle instead of requiring extra
 * bookkeeping here.
 */
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
        // Same reasoning as UserService#requireUser: the caller's identity
        // comes from a just-validated JWT subject, so a missing row means
        // the account was deleted after the token was issued.
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user " + userId + " no longer exists"));
    }
}
