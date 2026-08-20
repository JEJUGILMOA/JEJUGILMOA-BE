package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record WaypointCreateRequest(

        @Schema(description = "장소 ID", example = "42")
        @NotNull
        Long placeId,

        @Schema(description = "선호 경유지 여부 (앵커 추천 기준점으로 사용)", example = "false")
        boolean isPreferred

) {}
