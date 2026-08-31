package com.example.jejugilmoa.global.external.tats;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "external.api.tats-cnctr-rate")
public record TatsCnctrRateProperties(
        String serviceKey,
        String baseUrl,
        String areaCd,
        List<String> signguCds
) {}
