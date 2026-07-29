package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record WaypointResponse(

        @Schema(description = "경유지(TravelCourse) ID", example = "7")
        Long waypointId,

        @Schema(description = "방문 순서 (1부터 시작)", example = "1")
        int sequenceOrder,

        @Schema(description = "장소 ID", example = "42")
        Long placeId,

        @Schema(description = "장소명", example = "애월 카페거리")
        String placeName,

        @Schema(description = "카테고리명", example = "카페")
        String categoryName,

        @Schema(description = "대표 이미지 URL")
        String imageUrl,

        @Schema(description = "주소", example = "제주특별자치도 제주시 애월읍")
        String address,

        @Schema(description = "장소 메모", example = "노을 시간대 방문 추천")
        String memo

) {}
