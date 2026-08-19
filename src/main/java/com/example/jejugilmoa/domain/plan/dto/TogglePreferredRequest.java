package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TogglePreferredRequest(

        @Schema(description = "선호 경유지 여부", example = "true")
        boolean isPreferred

) {}
