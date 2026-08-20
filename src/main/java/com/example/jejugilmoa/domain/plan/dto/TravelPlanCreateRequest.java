package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TravelCompanion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TravelPlanCreateRequest(

        @Schema(description = "여행 제목", example = "제주 여름 휴가")
        @NotBlank @Size(max = 200)
        String title,

        @Schema(description = "여행 시작일", example = "2026-08-15")
        @NotNull
        LocalDate startDate,

        @Schema(description = "여행 종료일 (시작일과 같거나 이후)", example = "2026-08-17")
        @NotNull
        LocalDate endDate,

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

        @Schema(description = "동반자 유형", example = "COUPLE")
        TravelCompanion companion,

        @Schema(description = "선호 카테고리 ID 목록 (1개 이상)", example = "[1, 2]")
        @NotNull @Size(min = 1)
        List<@NotNull @Positive Long> categoryIds,

        @Schema(description = "날짜별 경유지 목록 (계획 생성 시 초기 경유지, 빈 배열 허용)")
        @Valid
        List<DayPlanRequest> days,

        @Schema(description = "예산 (생략 가능)")
        BudgetCreateRequest budget

) {}
