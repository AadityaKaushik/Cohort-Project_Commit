package com.cohort.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderConfigTest {

    private final PasswordEncoder encoder = new PasswordEncoderConfig().passwordEncoder();

    @Test
    void hashesUseArgon2idWithAtLeast64MiBAndThreeIterations() {
        String hash = encoder.encode("a-fine-password");

        assertThat(hash).startsWith("$argon2id$");
        assertThat(hash).contains("m=65536,t=3,p=1");
    }

    @Test
    void hashFromPreviousParametersStillVerifiesAndIsFlaggedForUpgrade() {
        PasswordEncoder legacy = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        String legacyHash = legacy.encode("a-fine-password");

        assertThat(encoder.matches("a-fine-password", legacyHash)).isTrue();
        assertThat(encoder.upgradeEncoding(legacyHash)).isTrue();
    }

    @Test
    void currentHashIsNotFlaggedForUpgrade() {
        assertThat(encoder.upgradeEncoding(encoder.encode("a-fine-password"))).isFalse();
    }
}
