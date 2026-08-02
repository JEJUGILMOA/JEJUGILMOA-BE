package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record VisitCheckRequest(

        @Schema(description = "방문 인증할 경유지(TravelCourse) ID", example = "7")
        @NotNull(message = "경유지 ID는 필수입니다.")
        Long waypointId,

        @Schema(description = "현재 위치 위도", example = "33.39397")
        @NotNull(message = "현재 위치 위도는 필수입니다.")
        @DecimalMin(value = "-90", message = "위도 값이 올바르지 않습니다.")
        @DecimalMax(value = "90", message = "위도 값이 올바르지 않습니다.")
        Double latitude,

        @Schema(description = "현재 위치 경도", example = "126.24066")
        @NotNull(message = "현재 위치 경도는 필수입니다.")
        @DecimalMin(value = "-180", message = "경도 값이 올바르지 않습니다.")
        @DecimalMax(value = "180", message = "경도 값이 올바르지 않습니다.")
        Double longitude

) {}
