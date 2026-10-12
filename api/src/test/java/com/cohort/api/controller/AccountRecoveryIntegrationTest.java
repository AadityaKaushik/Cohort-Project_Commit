package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.service.mail.MailSender;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import({AbstractIntegrationTest.ContainerConfig.class, AccountRecoveryIntegrationTest.MailConfig.class})
class AccountRecoveryIntegrationTest extends AbstractIntegrationTest {

    record Sent(String to, String subject, String body) {
    }

    static final List<Sent> OUTBOX = new ArrayList<>();

    @TestConfiguration(proxyBeanMethods = false)
    static class MailConfig {

        @Bean
        @Primary
        MailSender recordingMailSender() {
            return (to, subject, body) -> OUTBOX.add(new Sent(to, subject, body));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clearOutbox() {
        OUTBOX.clear();
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void register(String email, String password) throws Exception {
        postJson("/api/v1/auth/register", """
                {"email": "%s", "password": "%s", "display_name": "Recover"}
                """.formatted(email, password)).andExpect(status().isCreated());
    }

    private String lastTokenFor(String email) {
        Sent mail = OUTBOX.stream().filter(m -> m.to().equals(email)).reduce((first, second) -> second)
                .orElseThrow();
        Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(mail.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @Test
    void resetTokenChangesPasswordAndCannotBeReused() throws Exception {
        String email = "reset-flow@example.com";
        register(email, "original-password-1");
        OUTBOX.clear();

        postJson("/api/v1/auth/password-reset/request", "{\"email\": \"%s\"}".formatted(email))
                .andExpect(status().isAccepted());
        String token = lastTokenFor(email);

        postJson("/api/v1/auth/password-reset/confirm", """
                {"token": "%s", "new_password": "brand-new-password-9"}
                """.formatted(token)).andExpect(status().isNoContent());

        postJson("/api/v1/auth/login", "{\"email\": \"%s\", \"password\": \"brand-new-password-9\"}".formatted(email))
                .andExpect(status().isOk());
        postJson("/api/v1/auth/login", "{\"email\": \"%s\", \"password\": \"original-password-1\"}".formatted(email))
                .andExpect(status().isUnauthorized());

        postJson("/api/v1/auth/password-reset/confirm", """
                {"token": "%s", "new_password": "another-password-77"}
                """.formatted(token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void expiredResetTokenIsRejected() throws Exception {
        String email = "reset-expired@example.com";
        register(email, "original-password-1");
        OUTBOX.clear();
        postJson("/api/v1/auth/password-reset/request", "{\"email\": \"%s\"}".formatted(email))
                .andExpect(status().isAccepted());
        String token = lastTokenFor(email);
        jdbc.update("update auth_tokens set expires_at = now() - interval '1 minute' "
                + "where user_id = (select id from users where email = ?)", email);

        postJson("/api/v1/auth/password-reset/confirm", """
                {"token": "%s", "new_password": "brand-new-password-9"}
                """.formatted(token)).andExpect(status().isBadRequest());
    }

    @Test
    void resetRequestForUnknownEmailLooksIdenticalAndSendsNothing() throws Exception {
        postJson("/api/v1/auth/password-reset/request", "{\"email\": \"nobody-here@example.com\"}")
                .andExpect(status().isAccepted());

        assertThat(OUTBOX).isEmpty();
    }

    @Test
    void newerResetRequestInvalidatesThePreviousToken() throws Exception {
        String email = "reset-twice@example.com";
        register(email, "original-password-1");
        OUTBOX.clear();
        postJson("/api/v1/auth/password-reset/request", "{\"email\": \"%s\"}".formatted(email));
        String first = lastTokenFor(email);
        postJson("/api/v1/auth/password-reset/request", "{\"email\": \"%s\"}".formatted(email));

        postJson("/api/v1/auth/password-reset/confirm", """
                {"token": "%s", "new_password": "brand-new-password-9"}
                """.formatted(first)).andExpect(status().isBadRequest());
    }

    @Test
    void registrationSendsVerificationLinkThatWorksOnceAndSetsTheFlag() throws Exception {
        String email = "verify-me@example.com";
        register(email, "original-password-1");
        String token = lastTokenFor(email);
        assertThat(jdbc.queryForObject("select email_verified_at is null from users where email = ?",
                Boolean.class, email)).isTrue();

        postJson("/api/v1/auth/email-verification/confirm", "{\"token\": \"%s\"}".formatted(token))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select email_verified_at is not null from users where email = ?",
                Boolean.class, email)).isTrue();

        postJson("/api/v1/auth/email-verification/confirm", "{\"token\": \"%s\"}".formatted(token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verificationRequestForUnknownEmailSendsNothingAndAnswersLikeAKnownOne() throws Exception {
        postJson("/api/v1/auth/email-verification/request", "{\"email\": \"ghost@example.com\"}")
                .andExpect(status().isAccepted());

        assertThat(OUTBOX).isEmpty();
    }
}
