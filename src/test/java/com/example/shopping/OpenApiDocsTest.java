package com.example.shopping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** API 文件可公開存取,且帶有 JWT 驗證設定、前後台分組正確 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode docs(String group) throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs/" + group)).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body);
    }

    @Test
    void storefrontGroup_hasBearerSchemeAndNoAdminPaths() throws Exception {
        JsonNode docs = docs("1-storefront");

        assertThat(docs.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(docs.at("/paths").has("/api/orders")).isTrue();
        docs.at("/paths").fieldNames().forEachRemaining(path -> assertThat(path).doesNotStartWith("/api/admin"));
    }

    @Test
    void adminGroup_onlyHasAdminPaths() throws Exception {
        JsonNode docs = docs("2-admin");

        assertThat(docs.at("/paths").has("/api/admin/orders")).isTrue();
        docs.at("/paths").fieldNames().forEachRemaining(path -> assertThat(path).startsWith("/api/admin"));
    }
}
