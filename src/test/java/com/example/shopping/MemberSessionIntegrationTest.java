package com.example.shopping;

import com.example.shopping.member.entity.Member;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 兩台裝置登入 → 列出兩個工作階段;換發 token 沿用同一工作階段;登出其他裝置 / 登出單一裝置後無法再換發 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberSessionIntegrationTest {

    private static final String PHONE_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Mobile Safari/604.1";
    private static final String DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/126.0 Safari/537.36";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void manageLoggedInDevices() throws Exception {
        Member member = new Member();
        member.setEmail("sessions-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        member.setPassword(passwordEncoder.encode("password123"));
        member.setName("多裝置會員");
        member.setEmailVerified(true);
        memberRepository.save(member);
        Map<String, String> credentials = Map.of("email", member.getEmail(), "password", "password123");

        JsonNode desktop = call(post("/api/auth/login").header("User-Agent", DESKTOP_UA), null, credentials, 200).at("/data");
        JsonNode phone = call(post("/api/auth/login").header("User-Agent", PHONE_UA)
                .header("X-Forwarded-For", "203.0.113.7, 10.0.0.1"), null, credentials, 200).at("/data");
        String desktopSession = desktop.at("/sessionId").asText();
        assertThat(desktopSession).isNotBlank().isNotEqualTo(phone.at("/sessionId").asText());

        // 換發 token:同一個工作階段,裝置資訊沿用登入時的
        JsonNode refreshed = call(post("/api/auth/refresh"), null,
                Map.of("refreshToken", desktop.at("/refreshToken").asText()), 200).at("/data");
        assertThat(refreshed.at("/sessionId").asText()).isEqualTo(desktopSession);
        String token = refreshed.at("/token").asText();

        JsonNode sessions = call(get("/api/members/me/sessions"), token, null, 200).at("/data");
        assertThat(sessions).hasSize(2);
        assertThat(sessions.findValues("sessionId")).extracting(JsonNode::asText).contains(desktopSession);
        assertThat(sessions.toString()).contains("203.0.113.7").contains("iPhone").contains("Windows NT");

        // 登出其他裝置:手機的 refresh token 失效,電腦的照常
        call(post("/api/members/me/sessions/revoke-others"), token, Map.of("currentSessionId", desktopSession), 200);
        call(post("/api/auth/refresh"), null, Map.of("refreshToken", phone.at("/refreshToken").asText()), 401);
        assertThat(call(get("/api/members/me/sessions"), token, null, 200).at("/data")).hasSize(1);

        // 登出電腦本身
        call(delete("/api/members/me/sessions/" + desktopSession), token, null, 200);
        call(post("/api/auth/refresh"), null, Map.of("refreshToken", refreshed.at("/refreshToken").asText()), 401);
        call(delete("/api/members/me/sessions/" + desktopSession), token, null, 404);
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
