package com.example.jejugilmoa.domain.notification.dto;

import com.example.jejugilmoa.domain.notification.enums.NotificationCategory;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationCategory type,
        String title,
        String body,
        Instant createdAt,
        boolean isRead,
        String deepLink
) {}
