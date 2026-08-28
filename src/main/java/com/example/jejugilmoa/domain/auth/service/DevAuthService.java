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

        // 탈퇴 계정만 행 잠금으로 조회해 익명화 스케줄러와 직렬화한다. 잠금 대기 중 익명화가
        // 커밋되면 provider/externalId가 비워지고, 다른 요청이 먼저 복구하면 deletedAt이 NULL이
        // 되어 조회에서 제외된다 — 후자는 신규 저장 시 유니크 제약 위반으로 드러난다(dev 전용 허용).
        return userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNotNullForUpdate(DEV_PROVIDER, request.email())
                .map(user -> {
                    user.restore();
                    return new DevAuthResponse(user.getId(), user.getEmail(), user.getNickname(), user.getRole(), false, null);
                })
                .orElseGet(() -> {
                    User user = userRepository.save(User.builder()
                            .externalProvider(DEV_PROVIDER)
                            .externalId(request.email())
                            .email(request.email())
                            .nickname(request.nickname())
                            .build());
                    return new DevAuthResponse(user.getId(), user.getEmail(), user.getNickname(), user.getRole(), true, null);
                });
    }

    @Transactional(readOnly = true)
    public DevAuthResponse login(DevLoginRequest request) {
        User user = userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNull(DEV_PROVIDER, request.email())
                .orElseThrow(() -> new GeneralException(AuthErrorCode.DEV_USER_NOT_FOUND));

        return new DevAuthResponse(user.getId(), user.getEmail(), user.getNickname(), user.getRole(), false, null);
    }
}
