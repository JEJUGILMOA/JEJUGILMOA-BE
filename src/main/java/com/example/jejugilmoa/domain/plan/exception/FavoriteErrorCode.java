package com.example.jejugilmoa.domain.plan.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FavoriteErrorCode implements BaseCode {
    FAVORITE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "FAVORITE400_1", "이미 즐겨찾기한 장소입니다."),
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "FAVORITE404_1", "즐겨찾기를 찾을 수 없습니다."),
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "FAVORITE400_2", "페이지 번호는 0 이상이어야 합니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "FAVORITE400_3", "페이지 크기는 1 이상 100 이하이어야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
