package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record TravelPlanCreateRequest(

        @Schema(description = "여행 제목", example = "제주 여름 휴가")
        @NotBlank @Size(max = 200)
        String title,

        @Schema(description = "여행 시작일 (오늘 이후)", example = "2026-08-15")
        @NotNull
        LocalDate startDate,

        @Schema(description = "여행 종료일 (시작일과 같거나 이후)", example = "2026-08-17")
        @NotNull
        LocalDate endDate,

        @Schema(description = "출발지 Place ID (null이면 departureLocationName 필수)", example = "null")
        Long departurePlaceId,

        @Schema(description = "출발지 직접 입력 텍스트 (departurePlaceId가 null일 때 필수)", example = "제주국제공항")
        @Size(max = 200)
        String departureLocationName,

        @Schema(description = "목적지 Place ID (null이면 destinationLocationName 필수)", example = "null")
        Long destinationPlaceId,

        @Schema(description = "목적지 직접 입력 텍스트 (destinationPlaceId가 null일 때 필수)", example = "성산일출봉")
        @Size(max = 200)
        String destinationLocationName,

        @Schema(description = "선호 카테고리 ID 목록 (1개 이상, GET /api/categories로 확인)", example = "[1, 2]")
        @NotNull @Size(min = 1)
        List<@NotNull @Positive Long> categoryIds

) {}
