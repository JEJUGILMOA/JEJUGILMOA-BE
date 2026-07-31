package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record WaypointAddRequest(

        @Schema(description = "추가할 장소 ID", example = "42")
        @NotNull(message = "장소 ID는 필수입니다.")
        Long placeId,

        @Schema(description = "추가할 날짜 (여행 계획의 시작일~종료일 범위 내)", example = "2026-08-15")
        @NotNull(message = "방문 날짜는 필수입니다.")
        LocalDate visitDate

) {}
