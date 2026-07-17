package com.example.jejugilmoa.domain.user.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_1", "사용자를 찾을 수 없습니다."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "USER400_1", "닉네임은 50자를 초과할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
