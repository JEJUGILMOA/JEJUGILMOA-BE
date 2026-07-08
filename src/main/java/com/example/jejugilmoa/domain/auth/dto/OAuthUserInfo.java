package com.example.jejugilmoa.domain.auth.dto;

import com.example.jejugilmoa.domain.auth.enums.SocialProvider;

public record OAuthUserInfo(
    SocialProvider provider,
    String externalId,
    String nickname,
    String profileImageUrl
) {
}
