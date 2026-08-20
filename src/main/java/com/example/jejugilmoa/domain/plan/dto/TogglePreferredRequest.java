package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record TogglePreferredRequest(

        @Schema(description = "선호 경유지 여부", example = "true")
        @NotNull(message = "isPreferred 필드는 필수입니다.")
        Boolean isPreferred

) {}
