package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record DayItineraryResponse(

        @Schema(description = "날짜", example = "2026-08-15")
        LocalDate date,

        @Schema(description = "여행 순번 (1일차, 2일차 ...)", example = "1")
        int dayNumber,

        @Schema(description = "해당 날짜의 경유지 목록 (방문 순서 오름차순)")
        List<WaypointResponse> waypoints

) {}
