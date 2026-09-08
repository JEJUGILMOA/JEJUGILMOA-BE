package com.example.jejugilmoa.domain.recommendation.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecommendationErrorCode implements BaseCode {

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE404_1", "존재하지 않는 추천 코스입니다."),
    SAVED_COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE404_2", "담은 코스를 찾을 수 없습니다."),
    COURSE_ALREADY_SAVED(HttpStatus.CONFLICT, "COURSE409_1", "이미 담은 코스입니다."),
    RECORD_COURSE_NOT_ACCESSIBLE(HttpStatus.FORBIDDEN, "COURSE403_1", "접근할 수 없는 기록 코스입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
