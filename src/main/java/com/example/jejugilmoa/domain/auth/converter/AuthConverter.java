package com.example.jejugilmoa.domain.auth.converter;

import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.dto.OAuthUserInfo;
import com.example.jejugilmoa.domain.user.entity.User;

public final class AuthConverter {

    private AuthConverter() {
    }

    public static User toUser(OAuthUserInfo userInfo) {
        return User.builder()
            .externalProvider(userInfo.provider().getKey())
            .externalId(userInfo.externalId())
            .nickname(userInfo.nickname())
            .profileImageUrl(userInfo.profileImageUrl())
            .build();
    }

    public static OAuthLoginResponse toResponse(User user, boolean newUser) {
        return new OAuthLoginResponse(
            user.getId(),
            user.getNickname(),
            user.getProfileImageUrl(),
            user.getRole(),
            newUser
        );
    }
}
