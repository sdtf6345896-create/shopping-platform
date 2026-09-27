package com.example.shopping;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 啟動完整 Spring context(H2 + Hibernate 建表),
 * 確保 bean 設定與 entity 對應沒有衝突(單元測試全 mock,抓不到這類問題)。
 */
@SpringBootTest
@ActiveProfiles("test")
class ShoppingPlatformApplicationTests {

    @Test
    void contextLoads() {
    }
}
