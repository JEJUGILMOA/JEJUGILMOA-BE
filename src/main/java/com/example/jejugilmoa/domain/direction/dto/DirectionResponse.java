package com.example.jejugilmoa.domain.direction.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 길찾기 응답.
 * path는 지도에 폴리라인을 그리기 위한 경로 좌표 목록 (위도/경도).
 * Redis 캐시(JDK 직렬화)에 저장되므로 Serializable을 구현한다.
 */
public record DirectionResponse(
    String option,
    RouteSummary summary,
    List<Coordinate> path
) implements Serializable {

    /** distance는 미터, duration은 밀리초, 요금은 원 단위. */
    public record RouteSummary(
        int distance,
        long duration,
        int tollFare,
        int taxiFare,
        int fuelPrice
    ) implements Serializable {}

    public record Coordinate(double lat, double lng) implements Serializable {}
}
