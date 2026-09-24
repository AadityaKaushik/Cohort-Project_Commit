package com.cohort.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.AbstractIntegrationTest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Proves the migrations in db/migration apply cleanly to a genuinely empty
 * Postgres 16 instance and produce the expected tables — not just that the
 * SQL parses.
 */
@SpringBootTest
@Import(AbstractIntegrationTest.ContainerConfig.class)
class FlywayMigrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void migrationsCreateExpectedTablesAndColumns() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(tableExists(connection, "users")).isTrue();
            assertThat(tableExists(connection, "refresh_tokens")).isTrue();
            assertThat(tableExists(connection, "companies")).isTrue();
            assertThat(tableExists(connection, "applications")).isTrue();
            assertThat(tableExists(connection, "application_status_history")).isTrue();
            assertThat(tableExists(connection, "tags")).isTrue();
            assertThat(tableExists(connection, "application_tags")).isTrue();
            assertThat(tableExists(connection, "resumes")).isTrue();
            assertThat(tableExists(connection, "resume_sections")).isTrue();
            assertThat(tableExists(connection, "interviews")).isTrue();
            assertThat(tableExists(connection, "interview_questions")).isTrue();
            assertThat(tableExists(connection, "interview_feedback")).isTrue();
            assertThat(tableExists(connection, "topics")).isTrue();
            assertThat(tableExists(connection, "topic_prerequisites")).isTrue();
            assertThat(tableExists(connection, "questions")).isTrue();
            assertThat(tableExists(connection, "practice_attempts")).isTrue();
            assertThat(tableExists(connection, "preparation_tasks")).isTrue();
            assertThat(tableExists(connection, "recommendations")).isTrue();
            assertThat(tableExists(connection, "recommendation_evidence")).isTrue();
            assertThat(tableExists(connection, "notifications")).isTrue();
            assertThat(columnExists(connection, "users", "email")).isTrue();
            assertThat(columnExists(connection, "refresh_tokens", "token_hash")).isTrue();
            assertThat(columnExists(connection, "companies", "normalized_name")).isTrue();
            assertThat(columnExists(connection, "applications", "current_stage")).isTrue();
            assertThat(columnExists(connection, "applications", "stage_entered_at")).isTrue();
            assertThat(columnExists(connection, "application_status_history", "to_status")).isTrue();
            assertThat(columnExists(connection, "tags", "name")).isTrue();
            assertThat(columnExists(connection, "resumes", "parent_resume_id")).isTrue();
            assertThat(columnExists(connection, "resumes", "is_default")).isTrue();
            assertThat(columnExists(connection, "resume_sections", "section_type")).isTrue();
            assertThat(columnExists(connection, "resume_sections", "metadata")).isTrue();
            assertThat(columnExists(connection, "interviews", "round_number")).isTrue();
            assertThat(columnExists(connection, "interviews", "round_type")).isTrue();
            assertThat(columnExists(connection, "interview_questions", "question_text")).isTrue();
            assertThat(columnExists(connection, "interview_questions", "topic_id")).isTrue();
            assertThat(columnExists(connection, "interview_feedback", "is_draft")).isTrue();
            assertThat(columnExists(connection, "topics", "default_difficulty")).isTrue();
            assertThat(columnExists(connection, "topic_prerequisites", "prerequisite_topic_id")).isTrue();
            assertThat(columnExists(connection, "questions", "is_system")).isTrue();
            assertThat(columnExists(connection, "practice_attempts", "attempted_at")).isTrue();
            assertThat(columnExists(connection, "preparation_tasks", "estimated_effort_minutes")).isTrue();
            assertThat(columnExists(connection, "recommendations", "evidence_tier")).isTrue();
            assertThat(columnExists(connection, "recommendations", "status")).isTrue();
            assertThat(columnExists(connection, "recommendation_evidence", "label")).isTrue();
            assertThat(columnExists(connection, "notifications", "related_entity_id")).isTrue();
            assertThat(columnExists(connection, "notifications", "dedupe_key")).isTrue();
            assertThat(columnExists(connection, "notifications", "is_read")).isTrue();
        }
    }

    @Test
    void flywaySchemaHistoryRecordsEveryMigrationAsSuccessful() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank")) {
            var versions = new ArrayList<String>();
            while (rs.next()) {
                assertThat(rs.getBoolean("success")).isTrue();
                versions.add(rs.getString("version"));
            }
            assertThat(versions)
                    .containsExactly(
                            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
                            "17", "18", "19", "20");
        }
    }

    /**
     * V20 (FR-99..101): the partial unique index on (user_id, dedupe_key) is
     * what makes repeated detection runs idempotent -- a second row for the
     * same occurrence must be rejected, while rows without a dedupe key (and
     * the same key for a different user) are unaffected.
     */
    @Test
    void notificationsDedupeKeyIsUniquePerUser() throws Exception {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO users (email, password_hash) VALUES ('v20-a@example.com', 'x'), ('v20-b@example.com', 'x')");
            String insert = "INSERT INTO notifications (user_id, type, title, body, dedupe_key) "
                    + "SELECT id, 'stalled_application', 't', 'b', %s FROM users WHERE email = '%s' ON CONFLICT DO NOTHING";

            assertThat(statement.executeUpdate(insert.formatted("'stall:1'", "v20-a@example.com"))).isEqualTo(1);
            assertThat(statement.executeUpdate(insert.formatted("'stall:1'", "v20-a@example.com"))).isZero();
            assertThat(statement.executeUpdate(insert.formatted("'stall:1'", "v20-b@example.com"))).isEqualTo(1);
            assertThat(statement.executeUpdate(insert.formatted("NULL", "v20-a@example.com"))).isEqualTo(1);
            assertThat(statement.executeUpdate(insert.formatted("NULL", "v20-a@example.com"))).isEqualTo(1);

            statement.execute("DELETE FROM users WHERE email IN ('v20-a@example.com', 'v20-b@example.com')");
            try (ResultSet rs = statement.executeQuery("SELECT count(*) FROM notifications WHERE title = 't'")) {
                rs.next();
                assertThat(rs.getLong(1)).as("notifications cascade with their owner").isZero();
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getTables(null, "public", tableName, null)) {
            return rs.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, "public", tableName, columnName)) {
            return rs.next();
        }
    }
}
