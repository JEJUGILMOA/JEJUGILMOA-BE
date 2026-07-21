package com.example.jejugilmoa.global.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.api")
public record ExternalApiProperties(ApiConfig tourApi, ApiConfig visitorData) {

    public record ApiConfig(String serviceKey, String baseUrl) {}
}
