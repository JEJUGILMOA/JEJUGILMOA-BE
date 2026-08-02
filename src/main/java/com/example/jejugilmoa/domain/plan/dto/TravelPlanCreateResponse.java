package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TransportMode;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.enums.TravelRegion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record TravelPlanCreateResponse(
        @Schema(description = "여행 계획 ID", example = "1") Long planId,
        @Schema(description = "여행 제목", example = "제주 여름 휴가") String title,
        @Schema(description = "시작일", example = "2026-08-15") LocalDate startDate,
        @Schema(description = "종료일", example = "2026-08-17") LocalDate endDate,
        @Schema(description = "여행 지역") TravelRegion region,
        @Schema(description = "이동 수단") TransportMode transportMode,
        @Schema(description = "계획 상태", example = "DRAFT") TravelPlanStatus status,
        @Schema(description = "선호 카테고리 이름 목록") List<String> categories,
        @Schema(description = "출발지 이름", example = "제주국제공항") String departureLocationName,
        @Schema(description = "목적지 이름", example = "성산일출봉") String destinationLocationName
) {}
