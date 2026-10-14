package com.cohort.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.AbstractIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

@SpringBootTest
@Import(AbstractIntegrationTest.ContainerConfig.class)
class SeedDataIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private DataSource dataSource;

    @Test
    void seedScriptLoadsIdenticallyWhenAppliedTwice() throws Exception {
        String sql = readSeedScript();

        try (Connection connection = dataSource.getConnection()) {
            executeScript(connection, sql);
            SeedCounts firstRun = countSeedRows(connection);

            executeScript(connection, sql);
            SeedCounts secondRun = countSeedRows(connection);

            assertThat(secondRun).usingRecursiveComparison().isEqualTo(firstRun);

            assertThat(firstRun.users()).isEqualTo(1);
            assertThat(firstRun.companies()).isEqualTo(8);
            assertThat(firstRun.resumes()).isEqualTo(3);
            assertThat(firstRun.applications()).isEqualTo(40);
            assertThat(firstRun.statusHistory()).isEqualTo(40);
            assertThat(firstRun.interviews()).isEqualTo(15);
            assertThat(firstRun.interviewQuestions()).isEqualTo(30);
            assertThat(firstRun.interviewFeedback()).isEqualTo(9);
        }
    }

    @Test
    void theSeededUserIsClearlyMarkedAsSynthetic() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            executeScript(connection, readSeedScript());

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT email, display_name FROM users WHERE id = 'deadbeef-0001-4000-8000-000000000001'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("email")).endsWith("@example.com");
                assertThat(rs.getString("display_name")).contains("[SEED]");
            }
        }
    }

    private String readSeedScript() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/seed/R__seed_dev_data.sql");
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private void executeScript(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private SeedCounts countSeedRows(Connection connection) throws Exception {
        return new SeedCounts(
                count(connection, "users", "email = 'seed.demo.user@example.com'"),
                count(connection, "companies", "created_by_user_id = 'deadbeef-0001-4000-8000-000000000001'"),
                count(connection, "resumes", "user_id = 'deadbeef-0001-4000-8000-000000000001'"),
                count(connection, "applications", "user_id = 'deadbeef-0001-4000-8000-000000000001'"),
                count(connection, "application_status_history",
                        "application_id IN (SELECT id FROM applications WHERE user_id = 'deadbeef-0001-4000-8000-000000000001')"),
                count(connection, "interviews", "user_id = 'deadbeef-0001-4000-8000-000000000001'"),
                count(connection, "interview_questions", "user_id = 'deadbeef-0001-4000-8000-000000000001'"),
                count(connection, "interview_feedback", "user_id = 'deadbeef-0001-4000-8000-000000000001'"));
    }

    private int count(Connection connection, String table, String whereClause) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT count(*) FROM " + table + " WHERE " + whereClause)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private record SeedCounts(
            int users,
            int companies,
            int resumes,
            int applications,
            int statusHistory,
            int interviews,
            int interviewQuestions,
            int interviewFeedback) {
    }
}
