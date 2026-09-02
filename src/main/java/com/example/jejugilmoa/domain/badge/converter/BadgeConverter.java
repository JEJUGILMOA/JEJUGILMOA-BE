package com.example.jejugilmoa.domain.badge.converter;

import com.example.jejugilmoa.domain.badge.dto.BadgeEarnedResponse;
import com.example.jejugilmoa.domain.badge.dto.BadgeItemResponse;
import com.example.jejugilmoa.domain.badge.entity.Badge;
import com.example.jejugilmoa.domain.badge.enums.BadgeType;
import com.example.jejugilmoa.domain.user.entity.UserBadge;

import java.time.LocalDateTime;

public class BadgeConverter {

    private static final String HIDDEN_NAME = "???";
    private static final String HIDDEN_DESCRIPTION = "조건을 달성하면 공개되는 히든 배지입니다.";

    private BadgeConverter() {
    }

    public static BadgeItemResponse toItemResponse(Badge badge, boolean acquired, LocalDateTime acquiredAt,
                                                    Integer currentProgress, Integer targetProgress) {
        boolean maskHidden = badge.getBadgeType() == BadgeType.HIDDEN && !acquired;

        return new BadgeItemResponse(
                badge.getId(),
                maskHidden ? HIDDEN_NAME : badge.getName(),
                maskHidden ? HIDDEN_DESCRIPTION : badge.getDescription(),
                maskHidden ? null : badge.getImageUrl(),
                acquired,
                acquiredAt,
                maskHidden ? null : currentProgress,
                maskHidden ? null : targetProgress
        );
    }

    public static BadgeEarnedResponse toEarnedResponse(UserBadge userBadge) {
        Badge badge = userBadge.getBadge();
        return new BadgeEarnedResponse(
                badge.getId(),
                badge.getName(),
                badge.getDescription(),
                badge.getImageUrl(),
                userBadge.getAcquiredAt()
        );
    }
}
