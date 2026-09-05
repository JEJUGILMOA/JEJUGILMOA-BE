package com.example.jejugilmoa.domain.direction.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DirectionErrorCode implements BaseCode {
    INVALID_COORDINATE(HttpStatus.BAD_REQUEST, "DIRECTION400_1", "유효하지 않은 좌표값입니다."),
    INVALID_OPTION(HttpStatus.BAD_REQUEST, "DIRECTION400_2", "지원하지 않는 경로 탐색 옵션입니다."),
    TOO_MANY_WAYPOINTS(HttpStatus.BAD_REQUEST, "DIRECTION400_3", "경유지는 최대 5개까지 입력할 수 있습니다."),
    INVALID_WAYPOINT_FORMAT(HttpStatus.BAD_REQUEST, "DIRECTION400_4", "경유지 형식이 올바르지 않습니다. (위도,경도|위도,경도)"),
    SAME_START_GOAL(HttpStatus.BAD_REQUEST, "DIRECTION400_5", "출발지와 목적지가 동일합니다."),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "DIRECTION404_1", "경로를 찾을 수 없습니다."),
    NAVER_API_ERROR(HttpStatus.BAD_GATEWAY, "DIRECTION502_1", "네이버 지도 API 호출에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
