package com.example.jejugilmoa.domain.auth.enums;

import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum SocialProvider {
    KAKAO("kakao", "카카오", false),
    NAVER("naver", "네이버", true),
    GOOGLE("google", "구글", true),
    APPLE("apple", "애플", false);

    private final String key;
    private final String displayName;
    private final boolean clientSecretRequired;

    public static SocialProvider from(String value) {
        return Arrays.stream(values())
            .filter(provider -> provider.key.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new GeneralException(AuthErrorCode.UNSUPPORTED_PROVIDER));
    }
}
