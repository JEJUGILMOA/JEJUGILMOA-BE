package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SharedPlanResponse(
        @Schema(description = "여행 계획 ID", example = "1") Long planId,
        @Schema(description = "여행 제목", example = "제주 여름 휴가") String title,
        @Schema(description = "여행 시작일", example = "2026-08-15") LocalDate startDate,
        @Schema(description = "여행 종료일", example = "2026-08-17") LocalDate endDate,
        @Schema(description = "출발지", example = "제주국제공항") String departure,
        @Schema(description = "방문 날짜와 순서로 정렬된 경유지") List<SharedWaypointResponse> waypoints,
        @Schema(description = "교통비 예산") Integer budgetTransportation,
        @Schema(description = "숙박 예산") Integer budgetAccommodation,
        @Schema(description = "식비 예산") Integer budgetFood,
        @Schema(description = "기타 예산") Integer budgetEtc,
        @Schema(description = "총 예산") Integer totalBudget
) {
    public record SharedWaypointResponse(
            @Schema(description = "방문 날짜", example = "2026-08-15") LocalDate visitDate,
            @Schema(description = "해당 날짜 내 방문 순서", example = "1") int sequenceOrder,
            @Schema(description = "장소 이름", example = "협재해수욕장") String placeName,
            @Schema(description = "장소 주소") String address,
            @Schema(description = "장소 위도") BigDecimal latitude,
            @Schema(description = "장소 경도") BigDecimal longitude,
            @Schema(description = "장소 대표 이미지") String imageUrl
    ) {}
}
