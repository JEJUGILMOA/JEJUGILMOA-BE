package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.badge.dto.BadgeEarnedResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record VisitCheckResponse(

        @Schema(description = "방문 인증 후 전체 경유지 목록 (순서 오름차순)")
        List<WaypointResponse> waypoints,

        @Schema(description = "모든 경유지가 처리되어 여행이 자동 완료됐으면 true", example = "false")
        boolean autoCompleted,

        @Schema(description = "자동 완료 시 이번 여행에서 획득한 뱃지 목록; 자동 완료가 아니면 null")
        List<BadgeEarnedResponse> earnedBadges

) {}
