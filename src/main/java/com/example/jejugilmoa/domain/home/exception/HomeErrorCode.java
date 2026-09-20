package com.example.jejugilmoa.domain.home.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum HomeErrorCode implements BaseCode {
    BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "HOME404_1", "배너 이미지를 조회할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
