package com.example.shopping;

import com.example.shopping.admin.entity.Admin;
import com.example.shopping.admin.repository.AdminRepository;
import com.example.shopping.member.entity.EmailVerificationToken;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.EmailVerificationTokenRepository;
import com.example.shopping.member.repository.MemberRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 端到端走一次購物主流程(真的 HTTP → Security → Service → JPA,資料庫用 H2):
 * 後台建分類與商品 → 會員註冊、驗證 Email、登入 → 加入購物車 → 結帳 → 付款 → 後台出貨 → 會員查看訂單歷程。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void fullPurchaseFlow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // ---- 後台:建立管理員、分類、商品並上架 ----
        Admin admin = new Admin();
        admin.setUsername("it-admin-" + suffix);
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setName("整合測試管理員");
        adminRepository.save(admin);

        String adminToken = call(post("/api/admin/auth/login"), null,
                Map.of("username", admin.getUsername(), "password", "admin123"), 200)
                .at("/data/token").asText();

        long categoryId = call(post("/api/admin/categories"), adminToken,
                Map.of("name", "整合測試分類-" + suffix, "sortOrder", 1), 200)
                .at("/data/id").asLong();

        JsonNode product = call(post("/api/admin/products"), adminToken, Map.of(
                "categoryId", categoryId,
                "name", "整合測試商品",
                "price", 500,
                "skus", List.of(Map.of("skuCode", "IT-" + suffix, "specName", "標準", "price", 500, "stock", 3))),
                200).at("/data");
        long productId = product.at("/id").asLong();
        long skuId = product.at("/skus/0/id").asLong();

        call(patch("/api/admin/products/" + productId + "/status"), adminToken, Map.of("status", "ON_SALE"), 200);

        // ---- 會員:註冊 → 未驗證不能登入 → 驗證 → 登入 ----
        String email = "it-" + suffix + "@example.com";
        Map<String, String> credentials = Map.of("email", email, "password", "password123");
        call(post("/api/auth/register"), null,
                Map.of("email", email, "password", "password123", "name", "整合測試會員"), 200);

        JsonNode unverified = call(post("/api/auth/login"), null, credentials, 400);
        assertThat(unverified.at("/message").asText()).contains("Email 驗證");

        Member member = memberRepository.findByEmail(email).orElseThrow();
        String verifyToken = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> t.getMemberId().equals(member.getId()))
                .map(EmailVerificationToken::getToken)
                .findFirst().orElseThrow();
        call(post("/api/auth/verify-email"), null, Map.of("token", verifyToken), 200);

        String memberToken = call(post("/api/auth/login"), null, credentials, 200).at("/data/token").asText();

        // 會員 token 不能打後台 API
        call(get("/api/admin/orders"), memberToken, null, 403);

        // ---- 購物:地址 → 購物車 → 結帳 ----
        long addressId = call(post("/api/members/addresses"), memberToken, Map.of(
                "recipientName", "王小明", "phone", "0912345678",
                "city", "台北市", "district", "大安區", "detailAddress", "復興南路一段1號"), 200)
                .at("/data/id").asLong();

        call(post("/api/cart/items"), memberToken, Map.of("skuId", skuId, "quantity", 2), 200);

        JsonNode order = call(post("/api/orders"), memberToken,
                Map.of("addressId", addressId, "paymentMethod", "CREDIT_CARD"), 200).at("/data");
        long orderId = order.at("/id").asLong();
        assertThat(order.at("/status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(order.at("/totalAmount").decimalValue()).isEqualByComparingTo("1000");
        assertThat(order.at("/paymentDeadline").isNull()).isFalse();
        assertThat(order.at("/statusLogs")).hasSize(1);

        // 下單扣庫存後剩 1 件,應出現在庫存警示中
        JsonNode lowStock = call(get("/api/admin/products/low-stock?threshold=1"), adminToken, null, 200).at("/data");
        assertThat(lowStock.findValuesAsText("skuCode")).contains("IT-" + suffix);

        // ---- 付款 → 出貨 ----
        call(post("/api/orders/" + orderId + "/pay"), memberToken, null, 200);

        JsonNode missingTracking = call(patch("/api/admin/orders/" + orderId + "/status"), adminToken,
                Map.of("status", "SHIPPING"), 400);
        assertThat(missingTracking.at("/message").asText()).contains("物流單號");

        call(patch("/api/admin/orders/" + orderId + "/status"), adminToken,
                Map.of("status", "SHIPPING", "shippingCarrier", "黑貓宅急便", "trackingNumber", "TRK-" + suffix), 200);

        // ---- 會員查看訂單:物流資訊與完整歷程 ----
        JsonNode shipped = call(get("/api/orders/" + orderId), memberToken, null, 200).at("/data");
        assertThat(shipped.at("/status").asText()).isEqualTo("SHIPPING");
        assertThat(shipped.at("/trackingNumber").asText()).isEqualTo("TRK-" + suffix);
        assertThat(shipped.at("/statusLogs").findValuesAsText("toStatus"))
                .containsExactly("PENDING_PAYMENT", "PAID", "SHIPPING");
        assertThat(shipped.at("/statusLogs").findValuesAsText("actor"))
                .containsExactly("MEMBER", "MEMBER", "ADMIN");

        // ---- 完成訂單 → 回饋 1% 購物金 → 下一筆訂單折抵 ----
        call(patch("/api/admin/orders/" + orderId + "/status"), adminToken, Map.of("status", "COMPLETED"), 200);
        JsonNode points = call(get("/api/points"), memberToken, null, 200).at("/data");
        assertThat(points.at("/balance").asInt()).isEqualTo(10);

        call(post("/api/cart/items"), memberToken, Map.of("skuId", skuId, "quantity", 1), 200);
        JsonNode secondOrder = call(post("/api/orders"), memberToken,
                Map.of("addressId", addressId, "paymentMethod", "ATM", "pointsToUse", 10), 200).at("/data");
        assertThat(secondOrder.at("/pointsUsed").asInt()).isEqualTo(10);
        assertThat(secondOrder.at("/totalAmount").decimalValue()).isEqualByComparingTo("490");
        assertThat(call(get("/api/points"), memberToken, null, 200).at("/data/balance").asInt()).isZero();

        // 取消第二筆訂單,購物金退回
        call(post("/api/orders/" + secondOrder.at("/id").asLong() + "/cancel"), memberToken, null, 200);
        JsonNode history = call(get("/api/points/transactions"), memberToken, null, 200).at("/data/content");
        assertThat(history.findValuesAsText("type")).containsExactly("REFUND", "REDEEM", "EARN");
        assertThat(history.at("/0/balanceAfter").asInt()).isEqualTo(10);

        // ---- 退貨:會員申請 → 後台核准 → 訂單退款、回庫存、收回回饋購物金 ----
        JsonNode completed = call(get("/api/orders/" + orderId), memberToken, null, 200).at("/data");
        assertThat(completed.at("/returnDeadline").isNull()).isFalse();
        JsonNode applied = call(post("/api/orders/" + orderId + "/return"), memberToken,
                Map.of("reason", "尺寸不合"), 200).at("/data/returnRequest");
        assertThat(applied.at("/status").asText()).isEqualTo("PENDING");
        call(post("/api/orders/" + orderId + "/return"), memberToken, Map.of("reason", "再申請一次"), 400);

        long returnId = applied.at("/id").asLong();
        call(post("/api/admin/returns/" + returnId + "/approve"), adminToken, Map.of(), 200);
        JsonNode refunded = call(get("/api/orders/" + orderId), memberToken, null, 200).at("/data");
        assertThat(refunded.at("/status").asText()).isEqualTo("REFUNDED");
        assertThat(refunded.at("/returnRequest/status").asText()).isEqualTo("APPROVED");
        assertThat(call(get("/api/points"), memberToken, null, 200).at("/data/balance").asInt()).isZero();
        JsonNode restocked = call(get("/api/admin/products/" + productId), adminToken, null, 200).at("/data");
        assertThat(restocked.at("/skus/0/stock").asInt()).isEqualTo(3);

        // ---- 後台:關鍵字搜尋與 CSV 匯出 ----
        JsonNode found = call(get("/api/admin/orders?keyword=" + suffix), adminToken, null, 200).at("/data");
        // 會員有兩筆訂單(第二筆已取消),Email 含 suffix 所以都搜得到
        assertThat(found.at("/totalElements").asLong()).isEqualTo(2);
        assertThat(found.at("/content").findValuesAsText("id")).contains(String.valueOf(orderId));

        var export = mockMvc.perform(get("/api/admin/orders/export?keyword=" + suffix)
                .header("Authorization", "Bearer " + adminToken)).andReturn().getResponse();
        assertThat(export.getStatus()).isEqualTo(200);
        assertThat(export.getHeader("Content-Disposition")).contains("attachment").contains(".csv");
        String csv = new String(export.getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv.lines()).hasSize(3);
        assertThat(csv).contains(shipped.at("/orderNo").asText()).contains("TRK-" + suffix);

        // ---- 商品問答:會員提問 → 後台回覆 → 前台公開可見 ----
        long questionId = call(post("/api/products/" + productId + "/questions"), memberToken,
                Map.of("content", "請問有其他顏色嗎?"), 200).at("/data/id").asLong();
        JsonNode pending = call(get("/api/admin/questions?answered=false"), adminToken, null, 200).at("/data/content");
        assertThat(pending.findValuesAsText("id")).contains(String.valueOf(questionId));
        call(put("/api/admin/questions/" + questionId + "/answer"), adminToken, Map.of("answer", "目前只有黑色"), 200);
        JsonNode publicQuestions = call(get("/api/products/" + productId + "/questions"), null, null, 200)
                .at("/data/content/0");
        assertThat(publicQuestions.at("/answer").asText()).isEqualTo("目前只有黑色");
        assertThat(publicQuestions.at("/memberName").asText()).isEqualTo("整**");
        call(post("/api/products/" + productId + "/questions"), null, Map.of("content", "未登入提問"), 401);

        // ---- 管理員操作紀錄:成功與失敗都有記,新增類操作從回傳結果取 id ----
        JsonNode orderAudit = call(get("/api/admin/audit-logs?targetType=ORDER&targetId=" + orderId), adminToken,
                null, 200).at("/data/content");
        assertThat(orderAudit.findValuesAsText("action")).contains("更新訂單狀態");
        assertThat(orderAudit.findValuesAsText("adminUsername")).containsOnly(admin.getUsername());
        assertThat(orderAudit.findValuesAsText("success")).contains("true", "false");
        assertThat(orderAudit.findValuesAsText("detail")).anyMatch(d -> d.contains("SHIPPING 黑貓宅急便 TRK-" + suffix));
        assertThat(orderAudit.findValuesAsText("errorMessage")).anyMatch(m -> m.contains("物流單號"));

        JsonNode productAudit = call(get("/api/admin/audit-logs?targetType=PRODUCT&targetId=" + productId),
                adminToken, null, 200).at("/data/content");
        assertThat(productAudit.findValuesAsText("action")).contains("新增商品", "商品上下架");

        // ---- 站內通知:訂單各階段、退貨、提問回覆都會通知,可全部標為已讀 ----
        long unread = call(get("/api/notifications/unread-count"), memberToken, null, 200).at("/data/count").asLong();
        assertThat(unread).isGreaterThanOrEqualTo(5);
        JsonNode inbox = call(get("/api/notifications?size=50"), memberToken, null, 200).at("/data/content");
        assertThat(inbox.findValuesAsText("title"))
                .contains("商品出貨通知", "退貨退款完成通知", "您的商品提問已回覆");
        assertThat(inbox.findValuesAsText("link")).contains("/orders/" + orderId, "/products/" + productId);
        call(post("/api/notifications/read-all"), memberToken, null, 200);
        assertThat(call(get("/api/notifications/unread-count"), memberToken, null, 200).at("/data/count").asLong())
                .isZero();

        // 未登入不能查看訂單
        call(get("/api/orders/" + orderId), null, null, 401);
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
