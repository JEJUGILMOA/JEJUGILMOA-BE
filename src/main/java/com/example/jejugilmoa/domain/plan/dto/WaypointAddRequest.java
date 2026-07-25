package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record WaypointAddRequest(

        @Schema(description = "추가할 장소 ID", example = "42")
        @NotNull(message = "장소 ID는 필수입니다.")
        Long placeId

) {}
