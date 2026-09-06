package com.example.jejugilmoa.domain.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Set;

@ConfigurationProperties(prefix = "app.apple")
public record AppleAuthProperties(String issuer, URI jwksUri, Set<String> allowedAudiences) {
}
