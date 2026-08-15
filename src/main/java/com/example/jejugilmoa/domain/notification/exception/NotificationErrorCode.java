package com.example.jejugilmoa.domain.notification.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseCode {
    DEVICE_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION404_1", "등록된 디바이스 토큰을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
