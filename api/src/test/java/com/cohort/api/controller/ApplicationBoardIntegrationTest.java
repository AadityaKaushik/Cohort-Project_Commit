package com.cohort.api.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class ApplicationBoardIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void summariesAndDetailExposeTheLegalNextAndCorrectionStatuses() throws Exception {
        String token = register("board-legal@example.com");
        String id = create(token, "OA");

        mockMvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].allowed_next_statuses",
                        containsInAnyOrder("INTERVIEW", "FINAL_ROUND", "OFFER", "REJECTED", "WITHDRAWN")))
                .andExpect(jsonPath("$.items[0].correction_statuses", containsInAnyOrder("SAVED", "APPLIED")));

        mockMvc.perform(get("/api/v1/applications/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.allowed_next_statuses", hasSize(5)));
    }

    @Test
    void legalMoveIsPersistedWithHistoryAndIllegalMoveIsRefusedWithAReadableReason() throws Exception {
        String token = register("board-move@example.com");
        String id = create(token, "APPLIED");

        move(token, id, "{\"to_status\": \"OA\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OA")));

        move(token, id, "{\"to_status\": \"ACCEPTED\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("INVALID_STATUS_TRANSITION")))
                .andExpect(jsonPath("$.error.message", is("An application cannot move from OA to Accepted.")))
                .andExpect(jsonPath("$.error.message", not(containsString("_"))));

        mockMvc.perform(get("/api/v1/applications/" + id + "/history").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].to_status", is("OA")));
    }

    @Test
    void backwardMoveWithoutANoteExplainsThatANoteIsNeeded() throws Exception {
        String token = register("board-back@example.com");
        String id = create(token, "INTERVIEW");

        move(token, id, "{\"to_status\": \"APPLIED\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message", containsString("Add a note")));

        move(token, id, "{\"to_status\": \"APPLIED\", \"note\": \"logged too early\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPLIED")));
    }

    @Test
    void anotherUsersApplicationIsNotFoundWhenMoved() throws Exception {
        String owner = register("board-owner@example.com");
        String other = register("board-other@example.com");
        String id = create(owner, "APPLIED");

        move(other, id, "{\"to_status\": \"OA\"}").andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions move(String token, String id, String body)
            throws Exception {
        return mockMvc.perform(post("/api/v1/applications/" + id + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String create(String token, String status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "Board Co", "job_title": "Engineer",
                                 "date_applied": "2026-01-10", "status": "%s"}
                                """.formatted(status)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Board"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
