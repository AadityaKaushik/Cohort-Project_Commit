package com.cohort.api.service;

import com.cohort.api.entity.OAuthIdentity;
import com.cohort.api.entity.User;
import com.cohort.api.exception.OAuthLinkRefusedException;
import com.cohort.api.exception.OAuthLinkRefusedException.Reason;
import com.cohort.api.repository.OAuthIdentityRepository;
import com.cohort.api.repository.UserRepository;
import com.cohort.api.security.oauth.OAuthProfile;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-93: turns a provider sign-in into one of our users. An existing account is
 * only ever linked when the provider itself marks the email verified; otherwise
 * anyone who can register that address at a provider could take the account over.
 */
@Service
public class OAuthAccountService {

    private final UserRepository userRepository;
    private final OAuthIdentityRepository identityRepository;
    private final PasswordEncoder passwordEncoder;

    public OAuthAccountService(
            UserRepository userRepository, OAuthIdentityRepository identityRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User resolveUser(OAuthProfile profile) {
        var linked = identityRepository.findByProviderAndSubject(profile.provider(), profile.subject());
        if (linked.isPresent()) {
            return userRepository.findById(linked.get().getUserId())
                    .orElseThrow(() -> new IllegalStateException("Linked identity points at a missing user"));
        }

        if (profile.email() == null || profile.email().isBlank()) {
            throw new OAuthLinkRefusedException(Reason.MISSING_EMAIL);
        }

        var existing = userRepository.findByEmail(profile.email());
        if (existing.isPresent()) {
            if (!profile.emailVerified()) {
                throw new OAuthLinkRefusedException(Reason.UNVERIFIED_EMAIL);
            }
            User user = existing.get();
            if (!user.isEmailVerified()) {
                user.markEmailVerified();
                userRepository.save(user);
            }
            link(user.getId(), profile);
            return user;
        }

        User created = new User(profile.email(), passwordEncoder.encode(UUID.randomUUID().toString()), displayNameOf(profile));
        if (profile.emailVerified()) {
            created.markEmailVerified();
        }
        created = userRepository.save(created);
        link(created.getId(), profile);
        return created;
    }

    private void link(UUID userId, OAuthProfile profile) {
        identityRepository.save(new OAuthIdentity(userId, profile.provider(), profile.subject(), profile.email()));
    }

    private String displayNameOf(OAuthProfile profile) {
        if (profile.displayName() != null && !profile.displayName().isBlank()) {
            return profile.displayName();
        }
        return profile.email().substring(0, profile.email().indexOf('@'));
    }
}
