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
            assertThat(columnExists(connection, "users", "email")).isTrue();
            assertThat(columnExists(connection, "refresh_tokens", "token_hash")).isTrue();
        }
    }

    @Test
    void flywaySchemaHistoryRecordsBothMigrationsAsSuccessful() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank")) {
            var versions = new ArrayList<String>();
            while (rs.next()) {
                assertThat(rs.getBoolean("success")).isTrue();
                versions.add(rs.getString("version"));
            }
            assertThat(versions).containsExactly("1", "2");
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
