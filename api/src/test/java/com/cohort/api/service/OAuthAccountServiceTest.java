package com.cohort.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cohort.api.entity.OAuthIdentity;
import com.cohort.api.entity.User;
import com.cohort.api.exception.OAuthLinkRefusedException;
import com.cohort.api.exception.OAuthLinkRefusedException.Reason;
import com.cohort.api.repository.OAuthIdentityRepository;
import com.cohort.api.repository.UserRepository;
import com.cohort.api.security.oauth.OAuthProfile;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class OAuthAccountServiceTest {

    private UserRepository userRepository;
    private OAuthIdentityRepository identityRepository;
    private OAuthAccountService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        identityRepository = mock(OAuthIdentityRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("unusable-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(identityRepository.findByProviderAndSubject(any(), any())).thenReturn(Optional.empty());
        service = new OAuthAccountService(userRepository, identityRepository, encoder);
    }

    @Test
    void unverifiedEmailOfAnExistingAccountIsRefusedAndNothingIsLinked() {
        when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(new User("owner@example.com", "hash", "Owner")));
        OAuthProfile profile = new OAuthProfile("github", "42", "owner@example.com", false, "Attacker");

        assertThatThrownBy(() -> service.resolveUser(profile))
                .isInstanceOf(OAuthLinkRefusedException.class)
                .extracting(e -> ((OAuthLinkRefusedException) e).getReason())
                .isEqualTo(Reason.UNVERIFIED_EMAIL);

        verify(identityRepository, never()).save(any());
    }

    @Test
    void verifiedEmailLinksTheExistingAccount() {
        User existing = new User("owner@example.com", "hash", "Owner");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(existing));

        User resolved = service.resolveUser(new OAuthProfile("google", "g-1", "owner@example.com", true, "Owner"));

        assertThat(resolved).isSameAs(existing);
        assertThat(resolved.isEmailVerified()).isTrue();
        ArgumentCaptor<OAuthIdentity> captor = ArgumentCaptor.forClass(OAuthIdentity.class);
        verify(identityRepository).save(captor.capture());
        assertThat(captor.getValue().getProvider()).isEqualTo("google");
        assertThat(captor.getValue().getSubject()).isEqualTo("g-1");
    }

    @Test
    void unknownEmailCreatesANewUserWithAnUnusablePassword() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        User created = service.resolveUser(new OAuthProfile("google", "g-2", "new@example.com", true, "New Person"));

        assertThat(created.getEmail()).isEqualTo("new@example.com");
        assertThat(created.getDisplayName()).isEqualTo("New Person");
        assertThat(created.getPasswordHash()).isEqualTo("unusable-hash");
        assertThat(created.isEmailVerified()).isTrue();
        verify(identityRepository).save(any(OAuthIdentity.class));
    }

    @Test
    void anAlreadyLinkedIdentitySignsInWithoutLookingAtTheEmail() {
        User linked = new User("old@example.com", "hash", "Linked");
        java.util.UUID userId = java.util.UUID.randomUUID();
        when(identityRepository.findByProviderAndSubject("github", "7"))
                .thenReturn(Optional.of(new OAuthIdentity(userId, "github", "7", "old@example.com")));
        when(userRepository.findById(userId)).thenReturn(Optional.of(linked));

        User resolved = service.resolveUser(new OAuthProfile("github", "7", "changed@example.com", false, "Linked"));

        assertThat(resolved).isSameAs(linked);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void aProviderWithoutAnEmailIsRefused() {
        assertThatThrownBy(() -> service.resolveUser(new OAuthProfile("github", "9", null, false, "No Mail")))
                .isInstanceOf(OAuthLinkRefusedException.class)
                .extracting(e -> ((OAuthLinkRefusedException) e).getReason())
                .isEqualTo(Reason.MISSING_EMAIL);
    }
}
