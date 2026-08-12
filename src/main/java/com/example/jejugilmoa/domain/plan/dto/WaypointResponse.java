package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WaypointResponse(

        @Schema(description = "경유지(TravelCourse) ID", example = "7")
        Long waypointId,

        @Schema(description = "방문 날짜", example = "2026-08-15")
        LocalDate visitDate,

        @Schema(description = "해당 날짜 내 방문 순서 (1부터 시작)", example = "1")
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

        @Schema(description = "방문 완료 여부", example = "false")
        boolean visited,

        @Schema(description = "실제 방문 인증 시각")
        LocalDateTime visitedAt,

        @Schema(description = "해당 날짜의 시작 경유지 여부", example = "true")
        boolean isStart,

        @Schema(description = "해당 날짜의 도착 경유지 여부", example = "false")
        boolean isDestination

) {}
