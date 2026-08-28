package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record FavoriteCreateRequest(
        @Schema(description = "즐겨찾기할 장소 ID", example = "1")
        @NotNull(message = "장소 ID는 필수입니다.")
        Long placeId
) {
}
