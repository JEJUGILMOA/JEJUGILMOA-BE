package com.example.jejugilmoa.domain.auth.jwt;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}
