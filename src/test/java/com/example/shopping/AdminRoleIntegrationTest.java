package com.example.shopping;

import com.example.shopping.admin.entity.Admin;
import com.example.shopping.admin.repository.AdminRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 後台角色:客服(STAFF)只能用被允許的功能;停用或調整角色立即生效(不必等 token 過期);不能調整自己的權限。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminRoleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void staffPermissionsAndImmediateRevocation() throws Exception {
        Admin boss = new Admin();
        boss.setUsername("boss-" + suffix);
        boss.setPassword(passwordEncoder.encode("boss12345"));
        boss.setName("老闆");
        adminRepository.save(boss);
        String bossToken = login(boss.getUsername(), "boss12345").at("/token").asText();

        JsonNode staff = call(post("/api/admin/accounts"), bossToken, Map.of(
                "username", "cs-" + suffix, "name", "客服小美", "password", "staff12345", "role", "STAFF"), 200)
                .at("/data");
        long staffId = staff.at("/id").asLong();
        JsonNode staffLogin = login("cs-" + suffix, "staff12345");
        assertThat(staffLogin.at("/role").asText()).isEqualTo("STAFF");
        String staffToken = staffLogin.at("/token").asText();

        // 客服可以處理訂單、查看商品,看得到自己的帳號資訊
        call(get("/api/admin/orders"), staffToken, null, 200);
        call(get("/api/admin/products"), staffToken, null, 200);
        call(get("/api/admin/reports/dashboard"), staffToken, null, 200);
        assertThat(call(get("/api/admin/account/me"), staffToken, null, 200).at("/data/role").asText())
                .isEqualTo("STAFF");
        // 不能改商品、管理行銷、看報表或管理帳號
        call(patch("/api/admin/products/status"), staffToken, Map.of("ids", new long[]{1}, "status", "OFF_SHELF"), 403);
        call(get("/api/admin/coupons"), staffToken, null, 403);
        call(get("/api/admin/reports/summary"), staffToken, null, 403);
        call(get("/api/admin/accounts"), staffToken, null, 403);

        // 升為 ADMIN 後,同一個 token 立刻有完整權限
        call(put("/api/admin/accounts/" + staffId), bossToken,
                Map.of("name", "客服小美", "role", "ADMIN", "status", "ACTIVE"), 200);
        call(get("/api/admin/coupons"), staffToken, null, 200);

        // 停用後,手上的 token 立刻失效
        call(put("/api/admin/accounts/" + staffId), bossToken,
                Map.of("name", "客服小美", "role", "STAFF", "status", "DISABLED"), 200);
        call(get("/api/admin/orders"), staffToken, null, 401);

        // 不能調整自己的角色 / 狀態
        call(put("/api/admin/accounts/" + boss.getId()), bossToken,
                Map.of("name", "老闆", "role", "STAFF", "status", "ACTIVE"), 400);
        // 帳號重複
        call(post("/api/admin/accounts"), bossToken, Map.of(
                "username", "cs-" + suffix, "name", "重複", "password", "staff12345", "role", "STAFF"), 400);
    }

    /** 公開路徑只開放 GET;其他方法一律要登入(先前 requestMatchers("GET", ...) 會把 "GET" 當成路徑,開放了所有方法) */
    @Test
    void publicPathsOnlyAllowAnonymousGet() throws Exception {
        call(get("/api/products"), null, null, 200);
        // 由 security 層擋下(訊息來自 JwtAuthenticationEntryPoint),而不是靠 controller 裡的 SecurityUtils 才擋
        assertThat(call(put("/api/products/1/reviews/me"), null, Map.of("rating", 5), 401).at("/message").asText())
                .isEqualTo("請先登入或登入已逾期");
        assertThat(call(post("/api/products/1/questions"), null, Map.of("content", "?"), 401).at("/message").asText())
                .isEqualTo("請先登入或登入已逾期");
    }

    private JsonNode login(String username, String password) throws Exception {
        return call(post("/api/admin/auth/login"), null, Map.of("username", username, "password", password), 200)
                .at("/data");
    }

    private JsonNode call(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus)
            throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        var result = mockMvc.perform(request).andReturn();
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(result.getResponse().getStatus())
                .as("%s %s → %s", result.getRequest().getMethod(), result.getRequest().getRequestURI(), content)
                .isEqualTo(expectedStatus);
        return content.isEmpty() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }
}
