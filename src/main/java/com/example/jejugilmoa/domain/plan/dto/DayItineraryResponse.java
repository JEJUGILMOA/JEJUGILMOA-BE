package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DayItineraryResponse(

        @Schema(description = "날짜", example = "2026-08-15")
        LocalDate date,

        @Schema(description = "여행 순번 (1일차, 2일차 ...)", example = "1")
        int dayNumber,

        @Schema(description = "출발지 이름 (null이면 미설정)", example = "제주국제공항")
        String departureLocationName,

        @Schema(description = "출발지 위도 (null이면 미설정)", example = "33.5070")
        BigDecimal departureLatitude,

        @Schema(description = "출발지 경도 (null이면 미설정)", example = "126.4927")
        BigDecimal departureLongitude,

        @Schema(description = "해당 날짜의 경유지 목록 (방문 순서 오름차순)")
        List<WaypointResponse> waypoints

) {}
