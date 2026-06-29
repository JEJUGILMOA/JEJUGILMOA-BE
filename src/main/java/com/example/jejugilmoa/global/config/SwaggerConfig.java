package com.example.jejugilmoa.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI swagger() {
        Info info = new Info()
                .title("제주길모아 API 명세서")
                .description("제주길모아 백엔드 API 명세서입니다")
                .version("0.0.1");


        return new OpenAPI()
                .info(info)
                .addServersItem(new Server().url("/"));
    }
}
