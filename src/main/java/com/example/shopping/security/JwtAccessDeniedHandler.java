package com.example.shopping.security;

import com.example.shopping.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 直接寫回 JSON,不透過 response.sendError() 觸發 /error 轉發 —
 * 否則 /error 這個內部轉發請求會重新跑一次 filter chain,
 * 而 JwtAuthenticationFilter 預設不處理 ERROR dispatch,
 * 導致該請求被視為匿名,原本的 403 反而被 JwtAuthenticationEntryPoint 蓋成 401。
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error("權限不足,無法執行此操作")));
    }
}
