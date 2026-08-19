package com.example.jejugilmoa.domain.notification.service;

import com.example.jejugilmoa.domain.notification.dto.DeviceTokenRegisterRequest;
import com.example.jejugilmoa.domain.notification.entity.DeviceToken;
import com.example.jejugilmoa.domain.notification.repository.DeviceTokenRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate txTemplate;

    public void registerOrUpdateToken(Long userId, DeviceTokenRegisterRequest request) {
        try {
            txTemplate.executeWithoutResult(status -> evictAndUpsert(userId, request));
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 uk_device_token_token 또는 uk_device_token_user_device 충돌 발생.
            // 새 트랜잭션에서 충돌 행을 재조회한 뒤 소유권을 현재 요청자로 갱신한다.
            txTemplate.executeWithoutResult(status -> recoverFromConflict(userId, request));
        }
    }

    private void evictAndUpsert(Long userId, DeviceTokenRegisterRequest request) {
        deviceTokenRepository.evictTokenExcluding(request.token(), userId, request.deviceId());
        deviceTokenRepository.findByUserIdAndDeviceId(userId, request.deviceId())
                .ifPresentOrElse(
                        existing -> existing.updateToken(request.token()),
                        () -> saveNew(userId, request)
                );
    }

    private void saveNew(Long userId, DeviceTokenRegisterRequest request) {
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

    private void recoverFromConflict(Long userId, DeviceTokenRegisterRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
        deviceTokenRepository.findByToken(request.token())
                .ifPresentOrElse(
                        existing -> existing.updateOwner(user, request.deviceId(), request.platform()),
                        // 다른 요청이 이미 충돌 행을 제거한 경우 재삽입
                        () -> evictAndUpsert(userId, request)
                );
    }
}
