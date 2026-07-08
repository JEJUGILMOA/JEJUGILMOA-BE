package com.example.jejugilmoa.domain.auth.client;

import com.example.jejugilmoa.domain.auth.dto.OAuthLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthUserInfo;
import com.example.jejugilmoa.domain.auth.enums.SocialProvider;

public interface SocialOAuthClient {
    OAuthUserInfo fetchUserInfo(SocialProvider provider, OAuthLoginRequest request);
}
