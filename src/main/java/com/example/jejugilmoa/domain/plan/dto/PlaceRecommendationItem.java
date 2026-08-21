package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record PlaceRecommendationItem(

        @Schema(description = "장소 ID (DB 장소일 때만 존재)", example = "42")
        Long placeId,

        @Schema(description = "TourAPI contentId (폴백 결과일 때만 존재)", example = "126508")
        String contentId,

        @Schema(description = "장소명", example = "애월 카페거리")
        String name,

        @Schema(description = "카테고리명", example = "카페")
        String categoryName,

        @Schema(description = "대표 이미지 URL", example = "https://cdn.example.com/img/42.jpg")
        String imageUrl,

        @Schema(description = "주소", example = "제주특별자치도 제주시 애월읍")
        String address,

        @Schema(description = "위도", example = "33.46281")
        BigDecimal latitude,

        @Schema(description = "경도", example = "126.31466")
        BigDecimal longitude

) {}
