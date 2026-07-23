package com.example.jejugilmoa.domain.place.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlaceErrorCode implements BaseCode {
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE404_1", "존재하지 않는 관광지입니다."),
    INVALID_COORDINATE(HttpStatus.BAD_REQUEST, "PLACE400_1", "유효하지 않은 좌표값입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
