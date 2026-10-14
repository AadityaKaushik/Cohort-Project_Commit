package com.cohort.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class GoalIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void settingTheSameMetricTwiceKeepsOneGoal() throws Exception {
        Session owner = register("goal-upsert@example.com");
        setGoal(owner.token(), "applications", 5);
        setGoal(owner.token(), "applications", 8);

        mockMvc.perform(get("/api/v1/goals").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].weekly_target", is(8)))
                .andExpect(jsonPath("$[0].progress", is(0)))
                .andExpect(jsonPath("$[0].achieved", is(false)));
    }

    @Test
    void progressCountsThisWeeksActivity() throws Exception {
        Session owner = register("goal-progress@example.com");
        createApplication(owner.token(), "Acme");
        createApplication(owner.token(), "Globex");
        setGoal(owner.token(), "applications", 2);

        mockMvc.perform(get("/api/v1/goals").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$[0].progress", is(2)))
                .andExpect(jsonPath("$[0].achieved", is(true)));
    }

    @Test
    void activityHasStreakAndOneEntryPerDay() throws Exception {
        Session owner = register("goal-activity@example.com");
        createApplication(owner.token(), "Acme");
        jdbcTemplate.update("UPDATE applications SET date_applied = ? WHERE user_id = ?",
                LocalDate.now(ZoneOffset.UTC).minusDays(1), owner.userId());
        createApplication(owner.token(), "Globex");

        JsonNode activity = getJson(owner.token(), "/api/v1/goals/activity?weeks=2");
        assertThat(activity.get("current_streak").asInt()).isEqualTo(2);
        assertThat(activity.get("longest_streak").asInt()).isEqualTo(2);
        JsonNode days = activity.get("days");
        assertThat(days.get(days.size() - 1).get("date").asText()).isEqualTo(LocalDate.now(ZoneOffset.UTC).toString());
        assertThat(days.get(days.size() - 1).get("count").asInt()).isEqualTo(1);
    }

    @Test
    void targetMustBeBetweenOneAndAThousand() throws Exception {
        Session owner = register("goal-validation@example.com");
        mockMvc.perform(put("/api/v1/goals")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metric\": \"applications\", \"weekly_target\": 0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherUserCannotSeeOrDeleteSomeoneElsesGoal() throws Exception {
        Session owner = register("goal-iso-owner@example.com");
        Session intruder = register("goal-iso-intruder@example.com");
        String goalId = setGoal(owner.token(), "interviews", 3);
        createApplication(owner.token(), "Initech");

        mockMvc.perform(get("/api/v1/goals").header("Authorization", bearer(intruder)))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(delete("/api/v1/goals/" + goalId).header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("GOAL_NOT_FOUND")));
        JsonNode activity = getJson(intruder.token(), "/api/v1/goals/activity");
        assertThat(activity.get("current_streak").asInt()).isZero();
        assertThat(activity.get("longest_streak").asInt()).isZero();

        mockMvc.perform(delete("/api/v1/goals/" + goalId).header("Authorization", bearer(owner)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/goals")).andExpect(status().isUnauthorized());
    }

    private record Session(UUID userId, String token) {
    }

    private String bearer(Session session) {
        return "Bearer " + session.token();
    }

    private String setGoal(String token, String metric, int target) throws Exception {
        String body = mockMvc.perform(put("/api/v1/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metric\": \"%s\", \"weekly_target\": %d}".formatted(metric, target)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private JsonNode getJson(String token, String path) throws Exception {
        String body = mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String createApplication(String token, String companyName) throws Exception {
        String body = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "%s", "job_title": "Engineer", "date_applied": "%s", "status": "APPLIED"}
                                """.formatted(companyName, LocalDate.now(ZoneOffset.UTC))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
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
}
