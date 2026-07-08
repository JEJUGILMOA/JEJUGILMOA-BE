package com.example.jejugilmoa.domain.auth.config;

import com.example.jejugilmoa.domain.auth.enums.SocialProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth")
public record OAuthProperties(
        ProviderProperties kakao,
        ProviderProperties naver,
        ProviderProperties google
) {

    public ProviderProperties get(SocialProvider provider) {
        return switch (provider) {
            case KAKAO -> kakao;
            case NAVER -> naver;
            case GOOGLE -> google;
        };
    }

    public record ProviderProperties(
            String clientId,
            String clientSecret,
            String redirectUri,
            String tokenUri,
            String userInfoUri
    ) {
    }
}
