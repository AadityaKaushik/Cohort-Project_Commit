package com.cohort.api.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cohort.api.exception.WeakPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyValidatorTest {

    private PasswordPolicyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordPolicyValidator();
        validator.loadBlocklist();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1", "a", "1234567", ""})
    void rejectsPasswordsUnderTenCharacters(String password) {
        assertThatThrownBy(() -> validator.validate(password))
                .isInstanceOf(WeakPasswordException.class)
                .hasMessageContaining("10 characters");
    }

    @Test
    void rejectsBlocklistedPasswordRegardlessOfCase() {
        assertThatThrownBy(() -> validator.validate("Password123"))
                .isInstanceOf(WeakPasswordException.class)
                .hasMessageContaining("too common");
    }

    @Test
    void acceptsALongUnlistedPassword() {
        assertThatCode(() -> validator.validate("a-genuinely-unusual-passphrase-42"))
                .doesNotThrowAnyException();
    }
}
