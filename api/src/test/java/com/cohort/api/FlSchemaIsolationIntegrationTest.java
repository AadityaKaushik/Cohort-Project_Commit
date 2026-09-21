package com.cohort.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * P50 (US-61: "the server holds no user-schema database credentials,
 * verified in CI"): proves against a real, freshly-migrated Postgres
 * (Testcontainers, not a mock) that the {@code cohort_fl} role V19 creates
 * has zero privileges on any user-content (public schema) table, and is
 * granted exactly the two {@code fl} schema bookkeeping tables it needs.
 *
 * <p>This is the part of P50's verify column that can actually run in this
 * sandbox (no live docker-compose fl-service container is available here);
 * {@code scripts/verify-fl-db-isolation.sh} + the {@code fl-db-isolation}
 * CI job cover the equivalent check against a real running compose stack.
 */
@SpringBootTest
@Import(AbstractIntegrationTest.ContainerConfig.class)
class FlSchemaIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void cohortFlRoleHasNoPrivilegeOnAnyPublicSchemaTable() throws Exception {
        List<String> leakedTables = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        WITH public_tables AS MATERIALIZED (
                            SELECT table_name FROM information_schema.tables
                            WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                        )
                        SELECT table_name FROM public_tables
                        WHERE has_table_privilege('cohort_fl', 'public.' || quote_ident(table_name), 'SELECT')
                           OR has_table_privilege('cohort_fl', 'public.' || quote_ident(table_name), 'INSERT')
                           OR has_table_privilege('cohort_fl', 'public.' || quote_ident(table_name), 'UPDATE')
                           OR has_table_privilege('cohort_fl', 'public.' || quote_ident(table_name), 'DELETE')
                        """);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                leakedTables.add(resultSet.getString("table_name"));
            }
        }
        assertThat(leakedTables)
                .as("cohort_fl must have zero privileges on any user-content (public schema) table")
                .isEmpty();
    }

    @Test
    void cohortFlRoleIsGrantedExactlyTheTwoFlBookkeepingTables() throws Exception {
        List<String> granted = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT DISTINCT table_schema || '.' || table_name AS qualified
                        FROM information_schema.role_table_grants
                        WHERE grantee = 'cohort_fl'
                        ORDER BY 1
                        """);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                granted.add(resultSet.getString("qualified"));
            }
        }
        assertThat(granted).containsExactlyInAnyOrder("fl.fl_rounds", "fl.privacy_ledger_entries");
    }

    @Test
    void cohortFlRoleHasUsageOnTheFlSchemaOnly() throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement usageOnFl = connection.prepareStatement(
                        "SELECT has_schema_privilege('cohort_fl', 'fl', 'USAGE')");
                ResultSet usageOnFlResult = usageOnFl.executeQuery()) {
            usageOnFlResult.next();
            assertThat(usageOnFlResult.getBoolean(1)).isTrue();
        }
        try (Connection connection = dataSource.getConnection();
                PreparedStatement createOnPublic = connection.prepareStatement(
                        "SELECT has_schema_privilege('cohort_fl', 'public', 'CREATE')");
                ResultSet createOnPublicResult = createOnPublic.executeQuery()) {
            createOnPublicResult.next();
            assertThat(createOnPublicResult.getBoolean(1)).isFalse();
        }
    }
}
