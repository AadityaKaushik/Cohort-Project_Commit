package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.exception.AiRateLimitExceededException;
import com.cohort.api.exception.AiUnavailableException;
import com.cohort.api.service.ai.AiMessage;
import com.cohort.api.service.ai.AiPrompt;
import com.cohort.api.service.ai.AiService;
import com.cohort.api.service.ai.AiTestConfig;
import com.cohort.api.service.ai.StubAiProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({AbstractIntegrationTest.ContainerConfig.class, AiTestConfig.class})
@TestPropertySource(properties = {"app.ai.rate-limit.max-requests=3"})
class AiProviderIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AiService aiService;

    @Autowired
    private StubAiProvider provider;

    private final AiPrompt prompt = AiPrompt.text("system", List.of(AiMessage.user("my secret salary is 90000")));

    @BeforeEach
    void resetStub() {
        provider.reset();
    }

    @Test
    void statusReportsTheActiveProvider() throws Exception {
        UUID userId = register("ai-status@example.com").userId();
        String token = tokenFor("ai-status@example.com");
        mockMvc.perform(get("/api/v1/ai/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider", is("ollama")))
                .andExpect(jsonPath("$.hosted", is(false)));

        provider.setHosted(true);
        mockMvc.perform(get("/api/v1/ai/status").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.provider", is("gemini")))
                .andExpect(jsonPath("$.hosted", is(true)));
        assertThat(userId).isNotNull();
    }

    @Test
    void usageLogKeepsSizesButNeverPromptText() throws Exception {
        UUID userId = register("ai-usage@example.com").userId();
        provider.replyWith("a reply");

        aiService.complete(userId, "test_feature", prompt);

        List<String> rows = jdbcTemplate.queryForList(
                "SELECT feature || '|' || provider || '|' || succeeded || '|' || response_chars FROM ai_usage_log WHERE user_id = ?",
                String.class, userId);
        assertThat(rows).containsExactly("test_feature|ollama|true|7");
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'ai_usage_log'", String.class);
        assertThat(columns).doesNotContain("prompt", "response", "content", "text");
    }

    @Test
    void providerFailureIsLoggedAndSurfacesAsUnavailable() throws Exception {
        UUID userId = register("ai-down@example.com").userId();
        provider.goDown();

        try {
            aiService.complete(userId, "test_feature", prompt);
        } catch (AiUnavailableException expected) {
            assertThat(expected.getMessage()).isEqualTo("AI unavailable");
        }

        Boolean succeeded = jdbcTemplate.queryForObject(
                "SELECT succeeded FROM ai_usage_log WHERE user_id = ?", Boolean.class, userId);
        assertThat(succeeded).isFalse();
    }

    @Test
    void rateLimitIsPerUser() throws Exception {
        UUID busy = register("ai-busy@example.com").userId();
        UUID other = register("ai-other@example.com").userId();
        provider.replyWith("ok");

        for (int i = 0; i < 3; i++) {
            aiService.complete(busy, "test_feature", prompt);
        }
        try {
            aiService.complete(busy, "test_feature", prompt);
            throw new AssertionError("expected the fourth call to be limited");
        } catch (AiRateLimitExceededException expected) {
            assertThat(aiService.complete(other, "test_feature", prompt)).isEqualTo("ok");
        }
    }

    record Session(UUID userId, String token) {
    }

    private Session register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return new Session(UUID.fromString(json.get("user").get("id").asText()), json.get("access_token").asText());
    }

    private String tokenFor(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"a-genuinely-fine-password\"}".formatted(email)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("access_token").asText();
    }
}
