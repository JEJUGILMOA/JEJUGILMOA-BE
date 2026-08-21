package com.example.jejugilmoa.domain.notification.enums;

public enum NotificationType {
    PLAN_START,
    RECORD_WRITING,
    BADGE_ACQUIRED,
    NEXT_PLACE,
    PLACE_ARRIVAL,
    MARKETING;

    public NotificationCategory category() {
        return switch (this) {
            case PLAN_START, NEXT_PLACE, PLACE_ARRIVAL -> NotificationCategory.PLAN;
            case BADGE_ACQUIRED -> NotificationCategory.BADGE;
            case RECORD_WRITING, MARKETING -> NotificationCategory.SYSTEM;
        };
    }
}
