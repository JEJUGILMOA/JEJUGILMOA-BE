package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CoordDto(
        @Schema(description = "위도", example = "33.4996") double latitude,
        @Schema(description = "경도", example = "126.5312") double longitude
) {}
