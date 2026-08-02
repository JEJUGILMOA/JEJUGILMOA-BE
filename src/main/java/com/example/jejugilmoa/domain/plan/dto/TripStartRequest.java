package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record TripStartRequest(

        @Schema(description = "시작할 여행 계획 ID", example = "1")
        @NotNull(message = "여행 계획 ID는 필수입니다.")
        Long planId

) {}
