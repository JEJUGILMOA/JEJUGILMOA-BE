package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record TripCancelResponse(

        @Schema(description = "여행(Trip) ID — 여행 계획 ID와 동일", example = "1")
        Long tripId,

        @Schema(description = "여행 계획 제목", example = "제주 3박4일")
        String title,

        @Schema(description = "여행 상태 — 중단 후 CANCELLED")
        TravelPlanStatus status,

        @Schema(description = "실제 여행 시작 시각")
        LocalDateTime actualStartedAt,

        @Schema(description = "실제 여행 중단 시각")
        LocalDateTime actualCancelledAt

) {}
