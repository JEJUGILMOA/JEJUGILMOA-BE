package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TravelPlanUpdateRequest(

        @Schema(description = "여행 제목 (null이면 수정 안 함)", example = "제주 가을 여행")
        @Size(max = 200)
        String title,

        @Schema(description = "출발지 Place ID (null이고 departureLocationName도 null/빈값이면 출발지 수정 안 함)")
        Long departurePlaceId,

        @Schema(description = "출발지 직접 입력 텍스트 (null이고 departurePlaceId도 null이면 출발지 수정 안 함)", example = "새별오름")
        @Size(max = 200)
        String departureLocationName,

        @Schema(description = "목적지 Place ID (null이고 destinationLocationName도 null/빈값이면 목적지 수정 안 함)")
        Long destinationPlaceId,

        @Schema(description = "목적지 직접 입력 텍스트 (null이고 destinationPlaceId도 null이면 목적지 수정 안 함)", example = "중문관광단지")
        @Size(max = 200)
        String destinationLocationName,

        @Schema(description = "선호 카테고리 ID 목록 (null이면 수정 안 함, 전달 시 1개 이상 필수)", example = "[1, 2]")
        @Size(min = 1)
        List<@NotNull @Positive Long> categoryIds

) {}
