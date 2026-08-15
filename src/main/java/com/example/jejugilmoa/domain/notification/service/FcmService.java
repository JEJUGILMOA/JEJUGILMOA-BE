package com.example.jejugilmoa.domain.notification.service;

import com.example.jejugilmoa.domain.notification.entity.DeviceToken;
import com.example.jejugilmoa.domain.notification.repository.DeviceTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FcmService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase가 초기화되지 않아 푸시 알림을 전송하지 않습니다. userId={}", userId);
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(userId);
        for (DeviceToken deviceToken : tokens) {
            sendToToken(deviceToken, title, body, data);
        }
    }

    private void sendToToken(DeviceToken deviceToken, String title, String body, Map<String, String> data) {
        Message.Builder builder = Message.builder()
                .setToken(deviceToken.getToken())
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (data != null) {
            builder.putAllData(data);
        }

        try {
            FirebaseMessaging.getInstance().send(builder.build());
        } catch (FirebaseMessagingException e) {
            if (MessagingErrorCode.UNREGISTERED.equals(e.getMessagingErrorCode())) {
                log.info("만료된 FCM 토큰 삭제. deviceId={}", deviceToken.getDeviceId());
                deviceTokenRepository.delete(deviceToken);
            } else {
                log.error("FCM 전송 실패. deviceId={}, error={}", deviceToken.getDeviceId(), e.getMessage());
            }
        }
    }
}
