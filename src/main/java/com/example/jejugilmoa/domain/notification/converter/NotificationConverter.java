package com.example.jejugilmoa.domain.notification.converter;

import com.example.jejugilmoa.domain.notification.dto.NotificationResponse;
import com.example.jejugilmoa.domain.notification.entity.Notification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class NotificationConverter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public NotificationResponse toResponse(Notification notification, boolean isRead) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().category(),
                notification.getTitle(),
                notification.getBody(),
                notification.getCreatedAt(),
                isRead,
                extractDeepLink(notification.getData())
        );
    }

    private String extractDeepLink(String data) {
        if (data == null) {
            return null;
        }
        try {
            Map<String, String> payload = OBJECT_MAPPER.readValue(data, new TypeReference<Map<String, String>>() {});
            return payload.get("deepLink");
        } catch (JsonProcessingException e) {
            log.warn("알림 data 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}
