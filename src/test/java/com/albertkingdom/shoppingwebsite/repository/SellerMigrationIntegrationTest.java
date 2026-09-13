package com.albertkingdom.shoppingwebsite.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SellerMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v5CreatesSellerRoleAndProductOwnershipConstraint() {
        Integer sellerRoleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE name = 'ROLE_SELLER'", Integer.class);
        Integer sellerIndexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'product' AND column_name = 'seller_id'",
                Integer.class);
        Integer sellerForeignKeyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.key_column_usage "
                        + "WHERE constraint_schema = DATABASE() AND table_name = 'product' "
                        + "AND column_name = 'seller_id' AND referenced_table_name = 'users'",
                Integer.class);
        Integer orderItemSellerIndexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'order_item' AND column_name = 'seller_id'",
                Integer.class);
        Integer orderItemSellerForeignKeyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.key_column_usage "
                        + "WHERE constraint_schema = DATABASE() AND table_name = 'order_item' "
                        + "AND column_name = 'seller_id' AND referenced_table_name = 'users'",
                Integer.class);

        assertEquals(1, sellerRoleCount);
        assertEquals(1, sellerIndexCount);
        assertEquals(1, sellerForeignKeyCount);
        assertEquals(1, orderItemSellerIndexCount);
        assertEquals(1, orderItemSellerForeignKeyCount);
    }

    @Test
    void v6InitializesOrderCreatedAtAndAddsDatabaseDefault() {
        Integer nullable = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'orders' "
                        + "AND column_name = 'created_at' AND is_nullable = 'NO'", Integer.class);
        String defaultValue = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'orders' "
                        + "AND column_name = 'created_at'", String.class);

        assertEquals(1, nullable);
        assertNotNull(defaultValue);
    }
}
