package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.service.mail.MailSender;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import({AbstractIntegrationTest.ContainerConfig.class, EmailCodeIntegrationTest.MailConfig.class})
@TestPropertySource(properties = "app.auth.email-verification-required=true")
class EmailCodeIntegrationTest extends AbstractIntegrationTest {

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

    private ResultActions register(String email, String password) throws Exception {
        return postJson("/api/v1/auth/register", """
                {"email": "%s", "password": "%s", "display_name": "Coder"}
                """.formatted(email, password));
    }

    private ResultActions login(String email, String password) throws Exception {
        return postJson("/api/v1/auth/login", "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password));
    }

    private ResultActions confirm(String email, String code) throws Exception {
        return postJson("/api/v1/auth/email-verification/confirm",
                "{\"email\": \"%s\", \"code\": \"%s\"}".formatted(email, code));
    }

    private ResultActions reset(String email, String code, String newPassword) throws Exception {
        return postJson("/api/v1/auth/password-reset/confirm", """
                {"email": "%s", "code": "%s", "new_password": "%s"}
                """.formatted(email, code, newPassword));
    }

    private String lastCodeFor(String email) {
        Sent mail = OUTBOX.stream().filter(m -> m.to().equals(email)).reduce((first, second) -> second)
                .orElseThrow();
        Matcher matcher = Pattern.compile("\\b(\\d{6})\\b").matcher(mail.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static String wrong(String code) {
        return "%06d".formatted((Integer.parseInt(code) + 1) % 1_000_000);
    }

    private void allowResend(String email) {
        jdbc.update("update auth_tokens set created_at = created_at - interval '5 minutes' "
                + "where user_id = (select id from users where email = ?)", email);
    }

    @Test
    void signUpNeedsTheEmailedCodeBeforeSigningIn() throws Exception {
        String email = "code-signup@example.com";
        register(email, "original-password-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verification_required").value(true))
                .andExpect(jsonPath("$.access_token").doesNotExist());
        String code = lastCodeFor(email);
        assertThat(OUTBOX.getLast().body()).doesNotContain("token=");

        login(email, "original-password-1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("EMAIL_NOT_VERIFIED"));

        confirm(email, wrong(code)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
        confirm(email, code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.verification_required").value(false));

        login(email, "original-password-1").andExpect(status().isOk());
        confirm(email, code).andExpect(status().isBadRequest());
    }

    @Test
    void wrongPasswordOnAnUnconfirmedAccountLooksLikeAnyWrongPassword() throws Exception {
        String email = "code-wrong-password@example.com";
        register(email, "original-password-1").andExpect(status().isCreated());

        login(email, "not-the-password-1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void fiveWrongGuessesUseUpTheCode() throws Exception {
        String email = "code-guesses@example.com";
        register(email, "original-password-1");
        String code = lastCodeFor(email);

        for (int i = 0; i < 5; i++) {
            confirm(email, wrong(code)).andExpect(status().isBadRequest());
        }

        confirm(email, code).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select email_verified_at is null from users where email = ?",
                Boolean.class, email)).isTrue();
    }

    @Test
    void expiredCodeIsRejected() throws Exception {
        String email = "code-expired@example.com";
        register(email, "original-password-1");
        String code = lastCodeFor(email);
        jdbc.update("update auth_tokens set expires_at = now() - interval '1 minute' "
                + "where user_id = (select id from users where email = ?)", email);

        confirm(email, code).andExpect(status().isBadRequest());
    }

    @Test
    void codesAreStoredHashedNotAsTheCodeItself() throws Exception {
        String email = "code-hashed@example.com";
        register(email, "original-password-1");
        String code = lastCodeFor(email);

        String stored = jdbc.queryForObject("select token_hash from auth_tokens "
                + "where user_id = (select id from users where email = ?)", String.class, email);
        assertThat(stored).doesNotContain(code).contains("$");
    }

    @Test
    void resendWaitsForTheCooldownThenReplacesTheOldCode() throws Exception {
        String email = "code-resend@example.com";
        register(email, "original-password-1");
        String first = lastCodeFor(email);

        postJson("/api/v1/auth/email-verification/request", "{\"email\": \"%s\"}".formatted(email))
                .andExpect(status().isAccepted());
        assertThat(OUTBOX).hasSize(1);

        allowResend(email);
        postJson("/api/v1/auth/email-verification/request", "{\"email\": \"%s\"}".formatted(email))
                .andExpect(status().isAccepted());
        assertThat(OUTBOX).hasSize(2);
        String second = lastCodeFor(email);
        assertThat(jdbc.queryForObject("select count(*) from auth_tokens where used_at is null "
                + "and user_id = (select id from users where email = ?)", Integer.class, email)).isEqualTo(1);

        if (!first.equals(second)) {
            confirm(email, first).andExpect(status().isBadRequest());
        }
        confirm(email, second).andExpect(status().isOk());
    }

    @Test
    void resetCodeChangesPasswordAndCannotBeReused() throws Exception {
        String email = "code-reset@example.com";
        register(email, "original-password-1");
        confirm(email, lastCodeFor(email)).andExpect(status().isOk());
        OUTBOX.clear();

        postJson("/api/v1/auth/password-reset/request", "{\"email\": \"%s\"}".formatted(email))
                .andExpect(status().isAccepted());
        String code = lastCodeFor(email);
        assertThat(OUTBOX.getLast().subject()).containsIgnoringCase("password");

        reset(email, wrong(code), "brand-new-password-9").andExpect(status().isBadRequest());
        reset(email, code, "brand-new-password-9").andExpect(status().isNoContent());

        login(email, "brand-new-password-9").andExpect(status().isOk());
        login(email, "original-password-1").andExpect(status().isUnauthorized());
        reset(email, code, "another-password-77")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void resettingThePasswordAlsoConfirmsTheEmail() throws Exception {
        String email = "code-reset-unconfirmed@example.com";
        register(email, "original-password-1");
        OUTBOX.clear();

        postJson("/api/v1/auth/password-reset/request", "{\"email\": \"%s\"}".formatted(email));
        reset(email, lastCodeFor(email), "brand-new-password-9").andExpect(status().isNoContent());

        login(email, "brand-new-password-9").andExpect(status().isOk());
    }

    @Test
    void unknownEmailsGetTheSameAnswersAndNoMail() throws Exception {
        postJson("/api/v1/auth/password-reset/request", "{\"email\": \"nobody-here@example.com\"}")
                .andExpect(status().isAccepted());
        postJson("/api/v1/auth/email-verification/request", "{\"email\": \"ghost@example.com\"}")
                .andExpect(status().isAccepted());
        confirm("ghost@example.com", "123456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
        reset("ghost@example.com", "123456", "brand-new-password-9")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));

        assertThat(OUTBOX).isEmpty();
    }

    @Test
    void changingTheEmailSendsACodeToTheNewAddress() throws Exception {
        String email = "code-change-old@example.com";
        register(email, "original-password-1");
        String access = JsonPath.read(
                confirm(email, lastCodeFor(email)).andReturn().getResponse().getContentAsString(), "$.access_token");
        OUTBOX.clear();

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"code-change-new@example.com\"}"))
                .andExpect(status().isOk());

        assertThat(OUTBOX).extracting(Sent::to).containsExactly("code-change-new@example.com");
        login("code-change-new@example.com", "original-password-1").andExpect(status().isForbidden());
        confirm("code-change-new@example.com", lastCodeFor("code-change-new@example.com")).andExpect(status().isOk());
    }
}
