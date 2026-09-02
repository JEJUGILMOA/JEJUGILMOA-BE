package com.example.jejugilmoa.domain.recommendation.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecommendationErrorCode implements BaseCode {

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE404_1", "존재하지 않는 추천 코스입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
