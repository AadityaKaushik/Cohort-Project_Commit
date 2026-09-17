package com.cohort.api.security;

import com.cohort.api.exception.WeakPasswordException;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** FR-03: minimum length plus a common-password blocklist. */
@Component
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 10;

    private final Set<String> commonPasswords = new HashSet<>();

    @PostConstruct
    void loadBlocklist() {
        ClassPathResource resource = new ClassPathResource("common-passwords.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(String::toLowerCase)
                    .forEach(commonPasswords::add);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load common-passwords.txt", e);
        }
    }

    public void validate(String password) {
        if (password.length() < MIN_LENGTH) {
            throw new WeakPasswordException("Password must be at least " + MIN_LENGTH + " characters long.");
        }
        if (commonPasswords.contains(password.toLowerCase())) {
            throw new WeakPasswordException("Password is too common. Choose something less predictable.");
        }
    }
}
