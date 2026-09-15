package com.example.jejugilmoa.domain.user.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_1", "사용자를 찾을 수 없습니다."),
    BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_2", "차단 내역을 찾을 수 없습니다."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "USER400_1", "닉네임은 50자를 초과할 수 없습니다."),
    USER_NOT_WITHDRAWN(HttpStatus.BAD_REQUEST, "USER400_2", "탈퇴한 계정이 아닙니다."),
    BLOCK_SELF(HttpStatus.BAD_REQUEST, "USER400_3", "자기 자신을 차단할 수 없습니다."),
    ALREADY_BLOCKED(HttpStatus.CONFLICT, "USER409_1", "이미 차단한 사용자입니다."),
    USER_PERMANENTLY_WITHDRAWN(HttpStatus.GONE, "USER410_1", "탈퇴 후 30일이 지나 복구할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
