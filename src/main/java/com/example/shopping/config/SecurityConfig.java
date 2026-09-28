package com.example.shopping.config;

import com.example.shopping.security.JwtAccessDeniedHandler;
import com.example.shopping.security.JwtAuthenticationEntryPoint;
import com.example.shopping.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                           JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                           JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 公開:登入/註冊、商品與分類瀏覽、API 文件
                        .requestMatchers("/api/auth/**", "/api/admin/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/banners/**",
                                "/api/shipping/policy", "/api/promotions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**", "/sitemap.xml", "/robots.txt").permitAll()
                        // 後台:客服(STAFF)可處理訂單、退貨、問答、評價、留言與備註,商品 / 會員 / 分類只能查看
                        .requestMatchers("/api/admin/orders/**", "/api/admin/order-messages/**",
                                "/api/admin/returns/**", "/api/admin/questions/**", "/api/admin/reviews/**",
                                "/api/admin/notes/**", "/api/admin/reports/dashboard", "/api/admin/account/**")
                                .hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/admin/products/**", "/api/admin/members/**",
                                "/api/admin/categories/**").hasAnyRole("ADMIN", "STAFF")
                        // 其餘後台功能(商品編輯、行銷、報表、帳號管理…)只有 ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 其餘(購物車、訂單、會員中心)需登入
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
