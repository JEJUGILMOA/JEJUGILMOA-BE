package com.example.jejugilmoa.domain.badge.dto;

import java.time.LocalDateTime;

public record BadgeItemResponse(
        Long badgeId,
        String name,
        String description,
        String imageUrl,
        boolean acquired,
        LocalDateTime acquiredAt,
        Integer currentProgress,
        Integer targetProgress
) {}
