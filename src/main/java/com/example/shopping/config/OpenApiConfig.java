package com.example.shopping.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 設定:加上 JWT Bearer 驗證(右上角 Authorize 貼上登入取得的 token 即可測試需登入的 API),
 * 並把前台與後台 API 分成兩組。
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI shoppingPlatformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("購物平台 API")
                        .version("v1")
                        .description("""
                                仿 momo 風格購物網站的後端 API。
                                會員 API 用 `POST /api/auth/login` 取得 token,後台 API 用 `POST /api/admin/auth/login`;
                                點右上角 Authorize 貼上 token(不用加 Bearer 前綴)即可呼叫需要登入的 API。"""))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    public GroupedOpenApi storefrontApi() {
        return GroupedOpenApi.builder()
                .group("1-storefront")
                .displayName("前台 API")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("2-admin")
                .displayName("後台 API")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}
