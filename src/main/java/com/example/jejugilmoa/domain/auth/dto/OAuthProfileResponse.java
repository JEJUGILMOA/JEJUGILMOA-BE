package com.example.jejugilmoa.domain.auth.dto;

public record OAuthProfileResponse(
    String externalId,
    String nickname,
    String profileImageUrl
) {
}
