package com.example.jejugilmoa.domain.notification.service;

import com.example.jejugilmoa.domain.notification.entity.Notification;
import com.example.jejugilmoa.domain.notification.enums.NotificationType;
import com.example.jejugilmoa.domain.notification.repository.NotificationRepository;
import com.example.jejugilmoa.domain.user.entity.NotificationSetting;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.repository.NotificationSettingRepository;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Transactional
    public void send(Long userId, NotificationType type, String title, String body, Map<String, String> data) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        if (!isNotificationEnabled(userId, type)) {
            log.debug("알림 설정 꺼짐. userId={}, type={}", userId, type);
            return;
        }

        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .data(serializeData(data))
                .build());

        fcmService.sendToUser(userId, title, body, data);
    }

    private boolean isNotificationEnabled(Long userId, NotificationType type) {
        return notificationSettingRepository.findByUserIdAndUserDeletedAtIsNull(userId)
                .map(setting -> isTypeEnabled(setting, type))
                .orElse(true);
    }

    private boolean isTypeEnabled(NotificationSetting setting, NotificationType type) {
        Boolean enabled = switch (type) {
            case PLAN_START -> setting.getNotifyPlanStart();
            case RECORD_WRITING -> setting.getNotifyRecordWriting();
            case BADGE_ACQUIRED -> setting.getNotifyBadgeAcquired();
            case NEXT_PLACE -> setting.getNotifyNextPlace();
            case PLACE_ARRIVAL -> setting.getNotifyPlaceArrival();
            case MARKETING -> setting.getNotifyMarketing();
        };
        return Boolean.TRUE.equals(enabled);
    }

    private String serializeData(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("알림 data 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
