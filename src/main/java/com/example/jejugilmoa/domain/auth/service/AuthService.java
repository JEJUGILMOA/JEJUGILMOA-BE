package com.example.jejugilmoa.domain.auth.service;

import com.example.jejugilmoa.domain.auth.client.SocialOAuthClient;
import com.example.jejugilmoa.domain.auth.converter.AuthConverter;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.dto.OAuthUserInfo;
import com.example.jejugilmoa.domain.auth.enums.SocialProvider;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final SocialOAuthClient socialOAuthClient;
    private final UserRepository userRepository;

    @Transactional
    public OAuthLoginResponse login(String providerValue, OAuthLoginRequest request) {
        SocialProvider provider = SocialProvider.from(providerValue);
        OAuthUserInfo userInfo = socialOAuthClient.fetchUserInfo(provider, request);

        return userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNull(
                userInfo.provider().getKey(),
                userInfo.externalId()
            )
            .map(user -> AuthConverter.toResponse(user, false))
            .orElseGet(() -> createUser(userInfo));
    }

    private OAuthLoginResponse createUser(OAuthUserInfo userInfo) {
        User user = userRepository.save(AuthConverter.toUser(userInfo));
        return AuthConverter.toResponse(user, true);
    }
}
