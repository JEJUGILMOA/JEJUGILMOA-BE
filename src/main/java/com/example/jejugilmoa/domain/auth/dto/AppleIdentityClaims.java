package com.example.jejugilmoa.domain.auth.dto;

public record AppleIdentityClaims(
    String subject,
    String email
) {
}
