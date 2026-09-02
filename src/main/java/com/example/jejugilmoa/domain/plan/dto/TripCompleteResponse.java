package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.badge.dto.BadgeEarnedResponse;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TripCompleteResponse(

        @Schema(description = "여행(Trip) ID — 여행 계획 ID와 동일", example = "1")
        Long tripId,

        @Schema(description = "여행 계획 제목", example = "제주 3박4일")
        String title,

        @Schema(description = "여행 상태") TravelPlanStatus status,

        @Schema(description = "시작 날짜", example = "2026-07-20")
        LocalDate startDate,

        @Schema(description = "종료 날짜", example = "2026-07-23")
        LocalDate endDate,

        @Schema(description = "체류일수", example = "4")
        int durationDays,

        @Schema(description = "담은 장소 수", example = "7")
        int placeCount,

        @Schema(description = "총 이동거리 (km, 소수점 첫째자리)", example = "86.0")
        double totalDistanceKm,

        @Schema(description = "실제 여행 시작 시각") LocalDateTime actualStartedAt,

        @Schema(description = "실제 여행 완료 시각") LocalDateTime actualCompletedAt,

        @Schema(description = "이번 여행에서 새로 획득한 뱃지 목록") List<BadgeEarnedResponse> earnedBadges

) {}
