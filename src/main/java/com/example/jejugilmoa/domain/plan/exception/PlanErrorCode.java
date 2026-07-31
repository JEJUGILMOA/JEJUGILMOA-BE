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
    TRIP_DURATION_EXCEEDED(HttpStatus.BAD_REQUEST, "PLAN400_5", "여행 기간은 최대 30일을 초과할 수 없습니다."),
    PLACE_ALREADY_ADDED(HttpStatus.BAD_REQUEST, "PLAN400_6", "이미 추가된 장소입니다."),
    PLAN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PLAN403_1", "해당 여행 계획에 접근할 권한이 없습니다."),
    WAYPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN404_3", "존재하지 않는 경유지입니다."),
    INVALID_WAYPOINT_ORDER(HttpStatus.BAD_REQUEST, "PLAN400_7", "경유지 순서 목록이 올바르지 않습니다."),
    TRIP_NOT_STARTABLE(HttpStatus.BAD_REQUEST, "PLAN400_8", "계획중 상태의 여행만 시작할 수 있습니다."),
    TRIP_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "PLAN400_9", "진행중인 여행이 아닙니다."),
    WAYPOINT_ALREADY_VISITED(HttpStatus.BAD_REQUEST, "PLAN400_10", "이미 방문 체크된 경유지입니다."),
    WAYPOINT_LOCATION_MISMATCH(HttpStatus.BAD_REQUEST, "PLAN400_11", "방문할 경유지와 거리가 멉니다."),
    WAYPOINT_OUT_OF_ORDER(HttpStatus.BAD_REQUEST, "PLAN400_12", "이전 경유지를 먼저 방문 체크해야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
