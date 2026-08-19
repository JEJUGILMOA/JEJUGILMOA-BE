package com.example.jejugilmoa.domain.user.dto;

import com.example.jejugilmoa.domain.user.enums.TravelStyle;
import io.swagger.v3.oas.annotations.media.Schema;

public record TravelPreferenceUpdateRequest(

        @Schema(description = "자연 선호 여부 (null이면 수정 안 함)", example = "true")
        Boolean nature,

        @Schema(description = "음식 선호 여부 (null이면 수정 안 함)", example = "true")
        Boolean food,

        @Schema(description = "카페 선호 여부 (null이면 수정 안 함)", example = "false")
        Boolean cafe,

        @Schema(description = "전통시장 선호 여부 (null이면 수정 안 함)", example = "false")
        Boolean traditionMarket,

        @Schema(description = "역사 선호 여부 (null이면 수정 안 함)", example = "true")
        Boolean history,

        @Schema(description = "체험 선호 여부 (null이면 수정 안 함)", example = "false")
        Boolean experience,

        @Schema(description = "여행 스타일 (null이면 수정 안 함) — RELAXED: 여유롭게, MODERATE: 보통, ACTIVE: 활동적으로", example = "RELAXED")
        TravelStyle travelStyle

) {}
