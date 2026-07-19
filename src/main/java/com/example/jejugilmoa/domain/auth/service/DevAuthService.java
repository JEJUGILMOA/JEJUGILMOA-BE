package com.example.jejugilmoa.domain.auth.service;

import com.example.jejugilmoa.domain.auth.dto.DevAuthResponse;
import com.example.jejugilmoa.domain.auth.dto.DevLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.DevSignUpRequest;
import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile("!prod")
@Service
@RequiredArgsConstructor
public class DevAuthService {

    static final String DEV_PROVIDER = "dev";

    private final UserRepository userRepository;

    @Transactional
    public DevAuthResponse signUp(DevSignUpRequest request) {
        userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNull(DEV_PROVIDER, request.email())
                .ifPresent(u -> { throw new GeneralException(AuthErrorCode.DEV_EMAIL_ALREADY_EXISTS); });

        User user = userRepository.save(User.builder()
                .externalProvider(DEV_PROVIDER)
                .externalId(request.email())
                .email(request.email())
                .nickname(request.nickname())
                .build());

        return new DevAuthResponse(user.getId(), user.getEmail(), user.getNickname(), user.getRole(), true, null);
    }

    @Transactional(readOnly = true)
    public DevAuthResponse login(DevLoginRequest request) {
        User user = userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNull(DEV_PROVIDER, request.email())
                .orElseThrow(() -> new GeneralException(AuthErrorCode.DEV_USER_NOT_FOUND));

        return new DevAuthResponse(user.getId(), user.getEmail(), user.getNickname(), user.getRole(), false, null);
    }
}
