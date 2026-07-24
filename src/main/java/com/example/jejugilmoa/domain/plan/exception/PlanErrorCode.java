package com.example.jejugilmoa.domain.plan.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlanErrorCode implements BaseCode {
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN404_1", "존재하지 않는 여행 계획입니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "PLAN400_1", "종료일은 시작일 이후여야 합니다."),
    INVALID_START_DATE(HttpStatus.BAD_REQUEST, "PLAN400_2", "시작일은 오늘 이후여야 합니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN404_2", "존재하지 않는 카테고리입니다."),
    DEPARTURE_REQUIRED(HttpStatus.BAD_REQUEST, "PLAN400_3", "출발지 정보를 입력해주세요."),
    DESTINATION_REQUIRED(HttpStatus.BAD_REQUEST, "PLAN400_4", "목적지 정보를 입력해주세요."),
    TRIP_DURATION_EXCEEDED(HttpStatus.BAD_REQUEST, "PLAN400_5", "여행 기간은 최대 30일을 초과할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
