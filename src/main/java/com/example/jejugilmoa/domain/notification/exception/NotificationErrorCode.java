package com.example.jejugilmoa.domain.notification.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseCode {
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION404_1", "알림을 찾을 수 없습니다."),
    INVALID_READ_REQUEST(HttpStatus.BAD_REQUEST, "NOTIFICATION400_1", "notificationIds 또는 all 중 하나는 필요합니다."),
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "NOTIFICATION400_2", "page는 0 이상이어야 합니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "NOTIFICATION400_3", "size는 1~100 사이여야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
