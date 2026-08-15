package com.example.jejugilmoa.domain.notification.service;

import com.example.jejugilmoa.domain.notification.dto.DeviceTokenRegisterRequest;
import com.example.jejugilmoa.domain.notification.entity.DeviceToken;
import com.example.jejugilmoa.domain.notification.repository.DeviceTokenRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerOrUpdateToken(Long userId, DeviceTokenRegisterRequest request) {
        // 동일 FCM 토큰이 다른 사용자 또는 다른 기기에 등록되어 있으면 먼저 제거
        deviceTokenRepository.evictTokenExcluding(request.token(), userId, request.deviceId());

        deviceTokenRepository.findByUserIdAndDeviceId(userId, request.deviceId())
                .ifPresentOrElse(
                        existing -> existing.updateToken(request.token()),
                        () -> {
                            User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                                    .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
                            deviceTokenRepository.save(DeviceToken.builder()
                                    .user(user)
                                    .token(request.token())
                                    .platform(request.platform())
                                    .deviceId(request.deviceId())
                                    .lastUsedAt(Instant.now())
                                    .build());
                        }
                );
    }
}
