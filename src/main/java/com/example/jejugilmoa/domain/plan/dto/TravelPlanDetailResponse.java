package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TravelCompanion;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.user.enums.TravelStyle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TravelPlanDetailResponse(

        @Schema(description = "여행 계획 ID", example = "1")
        Long planId,

        @Schema(description = "여행 제목", example = "제주 여름 휴가")
        String title,

        @Schema(description = "시작 날짜", example = "2026-08-15")
        LocalDate startDate,

        @Schema(description = "종료 날짜", example = "2026-08-17")
        LocalDate endDate,

        @Schema(description = "박 수 (당일치기=0)", example = "2")
        int nights,

        @Schema(description = "일 수 (nights + 1)", example = "3")
        int days,

        @Schema(description = "계획 상태", example = "DRAFT")
        TravelPlanStatus status,

        @Schema(description = "여행 스타일", example = "RELAXED")
        TravelStyle travelStyle,

        @Schema(description = "동반자 유형", example = "COUPLE")
        TravelCompanion companion,

        @Schema(description = "출발지 이름", example = "제주국제공항")
        String departureLocationName,

        @Schema(description = "출발지 위도", example = "33.5072")
        BigDecimal departureLatitude,

        @Schema(description = "출발지 경도", example = "126.4929")
        BigDecimal departureLongitude,

        @Schema(description = "선호 테마 목록 (TravelTheme enum 문자열)", example = "[\"FOOD\", \"NATURE\"]")
        List<String> categories,

        @Schema(description = "날짜별 일정 목록 (계획 기간의 모든 날짜 포함, 경유지 없는 날도 포함)")
        List<DayItineraryResponse> itinerary,

        @Schema(description = "교통비 예산 (미설정 시 null)", example = "50000")
        Integer budgetTransportation,

        @Schema(description = "숙박 예산 (미설정 시 null)", example = "150000")
        Integer budgetAccommodation,

        @Schema(description = "식비 예산 (미설정 시 null)", example = "80000")
        Integer budgetFood,

        @Schema(description = "기타 예산 (미설정 시 null)", example = "30000")
        Integer budgetEtc,

        @Schema(description = "총 예산 합계 (모두 미설정 시 null)", example = "310000")
        Integer totalBudget

) {}
