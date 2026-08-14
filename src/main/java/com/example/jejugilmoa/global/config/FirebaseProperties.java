package com.example.jejugilmoa.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(Credentials credentials) {

    public record Credentials(String encoded) {}
}
