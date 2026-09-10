package com.albertkingdom.shoppingwebsite.repository;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SellerMigrationUpgradeIntegrationTest {

    private final String url = requiredEnvironment("MIGRATION_TEST_DATASOURCE_URL");
    private final String username = requiredEnvironment("MIGRATION_TEST_DATASOURCE_USERNAME");
    private final String password = requiredEnvironment("MIGRATION_TEST_DATASOURCE_PASSWORD");

    @AfterEach
    void cleanMigrationTestSchema() {
        flyway(null).clean();
    }

    @Test
    void v5UpgradePreservesPreExistingSellerRoleWithoutCreatingDuplicate() {
        flyway("4").migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        jdbcTemplate.update("INSERT INTO roles (name) VALUES ('ROLE_SELLER')");

        flyway("5").migrate();

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE name = 'ROLE_SELLER'", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'product' AND column_name = 'seller_id'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.key_column_usage "
                        + "WHERE constraint_schema = DATABASE() AND table_name = 'product' "
                        + "AND column_name = 'seller_id' AND referenced_table_name = 'users'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'order_item' AND column_name = 'seller_id'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.key_column_usage "
                        + "WHERE constraint_schema = DATABASE() AND table_name = 'order_item' "
                        + "AND column_name = 'seller_id' AND referenced_table_name = 'users'",
                Integer.class));
    }

    private Flyway flyway(String targetVersion) {
        var configuration = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                .cleanDisabled(false);
        if (targetVersion != null) {
            configuration.target(MigrationVersion.fromVersion(targetVersion));
        }
        return configuration.load();
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured for MySQL migration integration tests");
        }
        return value;
    }
}
