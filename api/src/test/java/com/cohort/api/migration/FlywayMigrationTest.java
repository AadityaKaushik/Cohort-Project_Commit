package com.cohort.api.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            assertThat(tableExists(connection, "report_exports")).isTrue();
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
            assertThat(columnExists(connection, "report_exports", "file_path")).isTrue();
            assertThat(columnExists(connection, "report_exports", "date_range_from")).isTrue();
            assertThat(columnExists(connection, "report_exports", "expires_at")).isTrue();
            assertThat(columnExists(connection, "auth_tokens", "token_hash")).isTrue();
            assertThat(columnExists(connection, "users", "email_verified_at")).isTrue();
            assertThat(tableExists(connection, "contacts")).isTrue();
            assertThat(tableExists(connection, "company_notes")).isTrue();
            assertThat(tableExists(connection, "offers")).isTrue();
            assertThat(tableExists(connection, "offer_ratings")).isTrue();
            assertThat(tableExists(connection, "offer_factor_weights")).isTrue();
            assertThat(tableExists(connection, "goals")).isTrue();
            assertThat(columnExists(connection, "contacts", "referral_status")).isTrue();
            assertThat(columnExists(connection, "offers", "response_deadline")).isTrue();
            assertThat(columnExists(connection, "goals", "weekly_target")).isTrue();
        }
    }

    @Test
    void oauthIdentitiesAcceptLinkedinButNoUnknownProvider() throws Exception {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO users (id, email, password_hash) VALUES ('6c1b0f1e-0000-4000-8000-000000000103', 'linkedin-check@example.com', 'x')");
            statement.execute("INSERT INTO oauth_identities (user_id, provider, subject, email) VALUES "
                    + "('6c1b0f1e-0000-4000-8000-000000000103', 'linkedin', 'li-check', 'linkedin-check@example.com')");
            assertThatThrownBy(() -> statement.execute("INSERT INTO oauth_identities (user_id, provider, subject, email) VALUES "
                    + "('6c1b0f1e-0000-4000-8000-000000000103', 'myspace', 'ms-check', 'linkedin-check@example.com')"))
                    .isInstanceOf(java.sql.SQLException.class);
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
                if (rs.getString("version") != null) {
                    versions.add(rs.getString("version"));
                }
            }
            assertThat(versions)
                    .containsExactly(
                            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
                            "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29",
                            "70", "71", "72", "73", "74", "75", "76", "77", "78", "90", "100", "101", "102", "103", "104", "105");
        }
    }

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

    // V21 (FR-102..104): a progress report is PDF-only, a date range can't be inverted, and exports cascade with their owner (US-08).
    @Test
    void reportExportsConstraintsHold() throws Exception {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO users (email, password_hash) VALUES ('v21@example.com', 'x')");
            String insert = "INSERT INTO report_exports (user_id, report_type, format, date_range_from, date_range_to, "
                    + "file_path, file_name, content_type, file_size_bytes, expires_at) "
                    + "SELECT id, '%s', '%s', %s, %s, 'k', 'f', 'application/pdf', 10, now() + interval '1 day' "
                    + "FROM users WHERE email = 'v21@example.com'";

            assertThat(statement.executeUpdate(insert.formatted("applications", "csv", "'2026-01-01'", "'2026-02-01'"))).isEqualTo(1);
            assertThat(statement.executeUpdate(insert.formatted("progress_report", "pdf", "NULL", "NULL"))).isEqualTo(1);
            assertViolates(statement, insert.formatted("progress_report", "csv", "NULL", "NULL"));
            assertViolates(statement, insert.formatted("applications", "pdf", "'2026-03-01'", "'2026-02-01'"));
            assertViolates(statement, insert.formatted("applications", "docx", "NULL", "NULL"));

            statement.execute("DELETE FROM users WHERE email = 'v21@example.com'");
            try (ResultSet rs = statement.executeQuery("SELECT count(*) FROM report_exports WHERE file_path = 'k'")) {
                rs.next();
                assertThat(rs.getLong(1)).as("report exports cascade with their owner").isZero();
            }
        }
    }

    @Test
    void sprintSevenTableConstraintsHold() throws Exception {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO users (email, password_hash) VALUES ('v25@example.com', 'x')");
            String uid = "(SELECT id FROM users WHERE email = 'v25@example.com')";

            assertThat(statement.executeUpdate("INSERT INTO contacts (user_id, name, referral_status) VALUES (" + uid + ", 'A', 'requested')")).isEqualTo(1);
            assertViolates(statement, "INSERT INTO contacts (user_id, name, referral_status) VALUES (" + uid + ", 'B', 'maybe')");

            assertThat(statement.executeUpdate("INSERT INTO goals (user_id, metric, weekly_target) VALUES (" + uid + ", 'applications', 5)")).isEqualTo(1);
            assertViolates(statement, "INSERT INTO goals (user_id, metric, weekly_target) VALUES (" + uid + ", 'interviews', 0)");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate(
                            "INSERT INTO goals (user_id, metric, weekly_target) VALUES (" + uid + ", 'applications', 7)"))
                    .isInstanceOf(java.sql.SQLException.class);

            assertThat(statement.executeUpdate("INSERT INTO notifications (user_id, type, title, body, related_entity_type, related_entity_id) "
                    + "VALUES (" + uid + ", 'cold_contact', 't25', 'b', 'contact', gen_random_uuid())")).isEqualTo(1);

            statement.execute("DELETE FROM users WHERE email = 'v25@example.com'");
            try (ResultSet rs = statement.executeQuery("SELECT count(*) FROM contacts WHERE name = 'A'")) {
                rs.next();
                assertThat(rs.getLong(1)).as("contacts cascade with their owner").isZero();
            }
        }
    }

    private void assertViolates(java.sql.Statement statement, String sql) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> statement.executeUpdate(sql))
                .isInstanceOf(java.sql.SQLException.class)
                .hasMessageContaining("violates check constraint");
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
