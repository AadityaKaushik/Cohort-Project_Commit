package com.cohort.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** P109 (US-73): every endpoint in the OpenAPI document, checked for 401 without a token and 404 on other users' ids. */
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class EndpointSecuritySuiteIntegrationTest extends AbstractIntegrationTest {
    private static final List<String> PUBLIC_PREFIXES = List.of("/health", "/api/v1/auth/", "/v3/api-docs");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    record Endpoint(HttpMethod method, String path) {
        boolean hasPathVariable() {
            return path.contains("{");
        }

        boolean sendsBody() {
            return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
        }

        String withIds(String id) {
            return path.replaceAll("\\{[^}]+}", id);
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    @BeforeEach
    void signIn() throws Exception {
        token = registerAndGetAccessToken("suite-" + UUID.randomUUID() + "@example.com");
    }

    @Test
    void documentCoversEverySprint() throws Exception {
        List<String> paths = endpoints().stream().map(Endpoint::path).toList();

        assertThat(paths).contains(
                "/api/v1/applications/{id}",
                "/api/v1/resumes/{id}",
                "/api/v1/interviews/{id}",
                "/api/v1/notifications",
                "/api/v1/contacts/{id}",
                "/api/v1/offers/{id}",
                "/api/v1/goals",
                "/api/v1/search",
                "/api/v1/directory/contributions");
    }

    @Test
    void everyProtectedEndpointRejectsAnonymousCallers() throws Exception {
        List<String> failures = new ArrayList<>();
        for (Endpoint endpoint : endpoints()) {
            if (isPublic(endpoint)) {
                continue;
            }
            int status = statusOf(endpoint, endpoint.withIds(UUID.randomUUID().toString()), null);
            if (status != 401) {
                failures.add(endpoint + " -> " + status);
            }
        }
        assertThat(failures).as("endpoints that did not return 401 without a token").isEmpty();
    }

    @Test
    void everyIdEndpointAnswersUnknownIdsWith404() throws Exception {
        List<String> failures = new ArrayList<>();
        for (Endpoint endpoint : endpoints()) {
            if (isPublic(endpoint) || !endpoint.hasPathVariable()) {
                continue;
            }
            int status = statusOf(endpoint, endpoint.withIds(UUID.randomUUID().toString()), token);
            if (!isHidden(endpoint, status)) {
                failures.add(endpoint + " -> " + status);
            }
        }
        assertThat(failures).as("endpoints that leaked something other than 404 for an unknown id").isEmpty();
    }

    @Test
    void everyIdEndpointAnswersAnotherUsersIdsWith404() throws Exception {
        Map<String, String> ownerIds = createRecordsForAnotherUser();
        List<String> failures = new ArrayList<>();
        for (Endpoint endpoint : endpoints()) {
            if (isPublic(endpoint) || !endpoint.hasPathVariable()) {
                continue;
            }
            String ownerId = ownerIdFor(endpoint, ownerIds);
            if (ownerId == null) {
                continue;
            }
            int status = statusOf(endpoint, endpoint.withIds(ownerId), token);
            if (!isHidden(endpoint, status)) {
                failures.add(endpoint + " -> " + status);
            }
        }
        assertThat(failures).as("endpoints that exposed another user's record").isEmpty();
    }

    // A write with an empty body can fail validation before the lookup; that reveals nothing about the record.
    private static boolean isHidden(Endpoint endpoint, int status) {
        return status == 404 || (endpoint.sendsBody() && status == 400);
    }

    private static String ownerIdFor(Endpoint endpoint, Map<String, String> ownerIds) {
        String path = endpoint.path();
        for (Map.Entry<String, String> entry : ownerIds.entrySet()) {
            if (path.startsWith(entry.getKey() + "/{")) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, String> createRecordsForAnotherUser() throws Exception {
        String ownerToken = registerAndGetAccessToken("owner-" + UUID.randomUUID() + "@example.com");
        String applicationId = create(ownerToken, "/api/v1/applications", """
                {"company_name": "Acme Corp", "job_title": "Backend Engineer", "date_applied": "2026-09-01", "status": "APPLIED"}
                """);
        String interviewId = create(ownerToken, "/api/v1/interviews", """
                {"application_id": "%s", "round_number": 1, "round_type": "TECHNICAL"}
                """.formatted(applicationId));
        String resumeId = create(ownerToken, "/api/v1/resumes", """
                {"name": "Backend resume"}
                """);
        String contactId = create(ownerToken, "/api/v1/contacts", """
                {"name": "Priya Sharma"}
                """);
        String offerId = create(ownerToken, "/api/v1/offers", """
                {"application_id": "%s", "base_salary": 1200000, "currency": "INR"}
                """.formatted(applicationId));
        String storyId = create(ownerToken, "/api/v1/stories", """
                {"title": "Migrated the billing service"}
                """);
        return Map.of(
                "/api/v1/stories", storyId,
                "/api/v1/applications", applicationId,
                "/api/v1/interviews", interviewId,
                "/api/v1/resumes", resumeId,
                "/api/v1/contacts", contactId,
                "/api/v1/offers", offerId);
    }

    private String create(String ownerToken, String path, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        assertThat(result.getResponse().getStatus()).as("creating " + path + ": " + result.getResponse().getContentAsString())
                .isEqualTo(201);
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // An exception that escapes the handlers would be a 500 in a running server.
    private int statusOf(Endpoint endpoint, String path, String bearer) throws Exception {
        MockHttpServletRequestBuilder builder = request(endpoint.method(), path);
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        if (endpoint.sendsBody()) {
            builder.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        try {
            return mockMvc.perform(builder).andReturn().getResponse().getStatus();
        } catch (ServletException ex) {
            return 500;
        }
    }

    private List<Endpoint> endpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode paths = objectMapper.readTree(result.getResponse().getContentAsString()).get("paths");
        List<Endpoint> endpoints = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> pathEntries = paths.fields();
        while (pathEntries.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathEntries.next();
            Iterator<String> methods = pathEntry.getValue().fieldNames();
            while (methods.hasNext()) {
                endpoints.add(new Endpoint(HttpMethod.valueOf(methods.next().toUpperCase(Locale.ROOT)), pathEntry.getKey()));
            }
        }
        assertThat(endpoints).isNotEmpty();
        return endpoints;
    }

    private static boolean isPublic(Endpoint endpoint) {
        return PUBLIC_PREFIXES.stream().anyMatch(endpoint.path()::startsWith);
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Suite User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
