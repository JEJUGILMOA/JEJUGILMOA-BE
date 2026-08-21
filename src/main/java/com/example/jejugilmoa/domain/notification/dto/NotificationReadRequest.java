package com.example.jejugilmoa.domain.notification.dto;

import java.util.List;

public record NotificationReadRequest(
        List<Long> notificationIds,
        Boolean all
) {}
