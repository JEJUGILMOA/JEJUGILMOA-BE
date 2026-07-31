package com.example.jejugilmoa.domain.map.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MapErrorCode implements BaseCode {
    INVALID_BOUNDS(HttpStatus.BAD_REQUEST, "MAP400_1", "유효하지 않은 지도 영역입니다."),
    INVALID_GRID_SIZE(HttpStatus.BAD_REQUEST, "MAP400_2", "격자 크기는 1 이상 20 이하이어야 합니다."),
    INVALID_LIMIT(HttpStatus.BAD_REQUEST, "MAP400_3", "조회 한도는 1 이상 500 이하이어야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
