package com.cohort.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.AbstractIntegrationTest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@Import(AbstractIntegrationTest.ContainerConfig.class)
class QuestionBankContentTest extends AbstractIntegrationTest {
    private static final String TEMPLATE_PATTERN =
            "^(Explain the core idea behind|Describe a mistake candidates commonly make with"
                    + "|Walk through how you would approach a classic|Compare two different approaches to a"
                    + "|What are the time and space complexity trade-offs)";

    @Autowired
    private DataSource dataSource;

    @Test
    void everyTopicHasSixSystemQuestionsCoveringEachDifficulty() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("""
                        SELECT t.slug,
                               COUNT(q.id) AS total,
                               COUNT(DISTINCT q.difficulty) AS difficulties
                        FROM topics t
                        LEFT JOIN questions q ON q.topic_id = t.id AND q.is_system
                        GROUP BY t.slug
                        """)) {
            int topics = 0;
            while (rs.next()) {
                topics++;
                assertThat(rs.getInt("total")).as(rs.getString("slug")).isEqualTo(6);
                assertThat(rs.getInt("difficulties")).as(rs.getString("slug")).isEqualTo(3);
            }
            assertThat(topics).isEqualTo(48);
        }
    }

    @Test
    void noTemplatedPromptTextRemains() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "SELECT COUNT(*) FROM questions WHERE is_system AND (content_key IS NULL OR question_text ~ '"
                                + TEMPLATE_PATTERN + "')")) {
            rs.next();
            assertThat(rs.getInt(1)).isZero();
        }
    }

    @Test
    void everyTopicHasARealDescription() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "SELECT COUNT(*) FROM topics WHERE description IS NULL OR description LIKE '% part of the % category%'")) {
            rs.next();
            assertThat(rs.getInt(1)).isZero();
        }
    }

    @Test
    void practiceAttemptsOnOldPlaceholderRowsStillResolve() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            postgres.start();

            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .placeholders(Map.of("flPassword", "test"))
                    .target("100")
                    .repeatableSqlMigrationPrefix("SKIP_")
                    .load()
                    .migrate();

            UUID userId = UUID.randomUUID();
            UUID attemptId = UUID.randomUUID();
            String oldQuestionId;
            try (Connection connection = DriverManager.getConnection(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                    Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO users (id, email, password_hash) VALUES ('" + userId
                        + "', 'old-attempt@example.com', 'x')");
                try (ResultSet rs = statement.executeQuery("""
                        SELECT q.id FROM questions q JOIN topics t ON t.id = q.topic_id
                        WHERE t.slug = 'arrays' AND q.question_text LIKE 'Explain the core idea behind %'
                        """)) {
                    rs.next();
                    oldQuestionId = rs.getString(1);
                }
                statement.execute("INSERT INTO practice_attempts (id, user_id, question_id, status) VALUES ('"
                        + attemptId + "', '" + userId + "', '" + oldQuestionId + "', 'confident')");
            }

            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .placeholders(Map.of("flPassword", "test"))
                    .load()
                    .migrate();

            try (Connection connection = DriverManager.getConnection(
                            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                    Statement statement = connection.createStatement();
                    ResultSet rs = statement.executeQuery("""
                            SELECT q.id, q.content_key, q.question_text
                            FROM practice_attempts a JOIN questions q ON q.id = a.question_id
                            WHERE a.id = '%s'
                            """.formatted(attemptId))) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("id")).isEqualTo(oldQuestionId);
                assertThat(rs.getString("content_key")).isEqualTo("arrays-1");
                assertThat(rs.getString("question_text")).doesNotStartWith("Explain the core idea behind");
            }
        }
    }
}
