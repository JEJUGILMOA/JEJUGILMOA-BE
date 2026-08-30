package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DayPlanRequest(

        @Schema(description = "방문 날짜 (여행 시작일~종료일 범위 내)", example = "2026-08-15")
        @NotNull
        LocalDate visitDate,

        @Schema(description = "출발지 Place ID (null이면 departureLocationName 필수)")
        Long departurePlaceId,

        @Schema(description = "출발지 텍스트 (departurePlaceId가 null일 때 필수)", example = "제주국제공항")
        @Size(max = 200)
        String departureLocationName,

        @Schema(description = "출발지 위도 (-90 ~ 90)", example = "33.5070")
        @NotNull
        @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
        BigDecimal departureLatitude,

        @Schema(description = "출발지 경도 (-180 ~ 180)", example = "126.4927")
        @NotNull
        @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
        BigDecimal departureLongitude,

        @Schema(description = "해당 날짜의 경유지 목록 (빈 배열 허용)")
        @Valid
        List<WaypointCreateRequest> waypoints

) {}
