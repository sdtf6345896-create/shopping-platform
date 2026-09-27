package com.example.shopping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在真的 MySQL 上跑完整啟動流程:全部 Flyway migration → Hibernate schema validate → dev 示範資料初始化。
 * 需要設定環境變數 MIGRATION_TEST_DB_URL(CI 會用 MySQL service container 提供),本機沒設定時自動略過。
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "MIGRATION_TEST_DB_URL", matches = ".+")
class MysqlMigrationIntegrationTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("MIGRATION_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> env("MIGRATION_TEST_DB_USERNAME", "root"));
        registry.add("spring.datasource.password", () -> env("MIGRATION_TEST_DB_PASSWORD", "root"));
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationsApplyAndDemoDataSeeds() {
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = FALSE", Integer.class);
        assertThat(failed).isZero();

        Integer orders = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
        Integer ordersWithoutLog = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders o WHERE NOT EXISTS "
                        + "(SELECT 1 FROM order_status_log l WHERE l.order_id = o.id)", Integer.class);
        assertThat(orders).isPositive();
        assertThat(ordersWithoutLog).isZero();
    }
}
