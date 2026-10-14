package com.cohort.api.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserDataExportRepository {
    private static final String OWNED_APPLICATIONS = "(SELECT id FROM applications WHERE user_id = ?)";
    private static final String OWNED_RESUMES = "(SELECT id FROM resumes WHERE user_id = ?)";
    private static final String OWNED_RECOMMENDATIONS = "(SELECT id FROM recommendations WHERE user_id = ?)";

    private static final Map<String, String> QUERIES = queries();

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public UserDataExportRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, List<JsonNode>> findAllOwnedBy(UUID userId) {
        Map<String, List<JsonNode>> result = new LinkedHashMap<>();
        QUERIES.forEach((name, sql) -> result.put(name, jdbcTemplate.query(sql, (rs, row) -> parse(rs.getString(1)), userId)));
        return result;
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read exported row", ex);
        }
    }

    private static Map<String, String> queries() {
        Map<String, String> queries = new LinkedHashMap<>();
        queries.put("companies", "SELECT to_jsonb(t)::text FROM companies t WHERE created_by_user_id = ?");
        queries.put("applications", "SELECT to_jsonb(t)::text FROM applications t WHERE user_id = ?");
        queries.put("application_status_history",
                "SELECT to_jsonb(t)::text FROM application_status_history t WHERE application_id IN " + OWNED_APPLICATIONS);
        queries.put("tags", "SELECT to_jsonb(t)::text FROM tags t WHERE user_id = ?");
        queries.put("application_tags",
                "SELECT to_jsonb(t)::text FROM application_tags t WHERE application_id IN " + OWNED_APPLICATIONS);
        queries.put("resumes", "SELECT (to_jsonb(t) - 'file_path')::text FROM resumes t WHERE user_id = ?");
        queries.put("resume_sections",
                "SELECT to_jsonb(t)::text FROM resume_sections t WHERE resume_id IN " + OWNED_RESUMES);
        queries.put("interviews", "SELECT to_jsonb(t)::text FROM interviews t WHERE user_id = ?");
        queries.put("interview_feedback", "SELECT to_jsonb(t)::text FROM interview_feedback t WHERE user_id = ?");
        queries.put("interview_questions", "SELECT to_jsonb(t)::text FROM interview_questions t WHERE user_id = ?");
        queries.put("questions", "SELECT to_jsonb(t)::text FROM questions t WHERE user_id = ?");
        queries.put("practice_attempts", "SELECT to_jsonb(t)::text FROM practice_attempts t WHERE user_id = ?");
        queries.put("preparation_tasks", "SELECT to_jsonb(t)::text FROM preparation_tasks t WHERE user_id = ?");
        queries.put("recommendations", "SELECT to_jsonb(t)::text FROM recommendations t WHERE user_id = ?");
        queries.put("recommendation_evidence",
                "SELECT to_jsonb(t)::text FROM recommendation_evidence t WHERE recommendation_id IN " + OWNED_RECOMMENDATIONS);
        queries.put("notifications", "SELECT to_jsonb(t)::text FROM notifications t WHERE user_id = ?");
        queries.put("report_exports", "SELECT (to_jsonb(t) - 'file_path')::text FROM report_exports t WHERE user_id = ?");
        queries.put("privacy_ledger_entries", "SELECT to_jsonb(t)::text FROM fl.privacy_ledger_entries t WHERE user_id = ?");
        return queries;
    }
}
