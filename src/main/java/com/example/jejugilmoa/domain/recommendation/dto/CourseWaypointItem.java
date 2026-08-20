package com.example.jejugilmoa.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CourseWaypointItem(

        @Schema(description = "경유 순서")
        int sequenceOrder,

        @Schema(description = "장소 ID")
        Long placeId,

        @Schema(description = "장소명")
        String placeName,

        @Schema(description = "대표 이미지 URL")
        String imageUrl,

        @Schema(description = "위도")
        BigDecimal latitude,

        @Schema(description = "경도")
        BigDecimal longitude

) {}
