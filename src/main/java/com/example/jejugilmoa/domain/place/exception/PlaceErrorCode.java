package com.example.jejugilmoa.domain.place.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlaceErrorCode implements BaseCode {
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE404_1", "존재하지 않는 관광지입니다."),
    INVALID_COORDINATE(HttpStatus.BAD_REQUEST, "PLACE400_1", "유효하지 않은 좌표값입니다."),
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "PLACE400_2", "페이지 번호는 0 이상이어야 합니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "PLACE400_3", "페이지 크기는 1 이상 100 이하이어야 합니다."),
    INVALID_LIMIT(HttpStatus.BAD_REQUEST, "PLACE400_4", "조회 한도는 1 이상 100 이하이어야 합니다."),
    SYNC_ALL_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "PLACE503_1", "모든 시군구 동기화에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
