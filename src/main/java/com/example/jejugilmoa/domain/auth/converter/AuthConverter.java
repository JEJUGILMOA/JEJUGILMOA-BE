package com.example.jejugilmoa.domain.auth.converter;

import com.example.jejugilmoa.domain.auth.dto.AppleIdentityClaims;
import com.example.jejugilmoa.domain.auth.enums.SocialProvider;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.dto.OAuthUserInfo;
import com.example.jejugilmoa.domain.user.entity.User;

public final class AuthConverter {

    private AuthConverter() {
    }

    public static OAuthUserInfo toUserInfo(AppleIdentityClaims identity) {
        return new OAuthUserInfo(SocialProvider.APPLE,
                identity.subject(), "애플 사용자", null, identity.email());
    }

    public static User toUser(OAuthUserInfo userInfo) {
        return User.builder()
            .externalProvider(userInfo.provider().getKey())
            .externalId(userInfo.externalId())
            .nickname(userInfo.nickname())
            .profileImageUrl(userInfo.profileImageUrl())
            .email(userInfo.email())
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
