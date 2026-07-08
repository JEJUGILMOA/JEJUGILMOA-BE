package com.example.jejugilmoa.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "JWT";

    @Bean
    public OpenAPI swagger() {
        Info info = new Info()
                .title("제주길모아 API 명세서")
                .description("제주길모아 백엔드 API 명세서입니다")
                .version("0.0.1");

        // Swagger UI 우측 상단 Authorize 버튼으로 Bearer 토큰 입력 가능
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(info)
                .addServersItem(new Server().url("/"))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
