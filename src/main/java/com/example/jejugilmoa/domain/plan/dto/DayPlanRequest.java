package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record DayPlanRequest(

        @Schema(description = "방문 날짜 (여행 시작일~종료일 범위 내)", example = "2026-08-15")
        @NotNull
        LocalDate visitDate,

        @Schema(description = "해당 날짜의 경유지 목록 (빈 배열 허용)")
        @Valid
        List<WaypointCreateRequest> waypoints

) {}
