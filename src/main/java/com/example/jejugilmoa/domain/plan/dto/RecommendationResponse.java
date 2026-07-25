package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RecommendationResponse(

        @Schema(description = "출발지명", example = "제주국제공항")
        String departureLocationName,

        @Schema(description = "목적지명", example = "성산일출봉")
        String destinationLocationName,

        @Schema(description = "추천 경유지 목록 (최대 5개, 출발지→목적지 방향 순)")
        List<PlaceRecommendationItem> recommendations

) {}
