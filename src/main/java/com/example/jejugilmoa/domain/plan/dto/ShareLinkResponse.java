package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record ShareLinkResponse(
        @Schema(description = "여행 계획 ID", example = "1") Long planId,
        @Schema(description = "공유 토큰", example = "550e8400-e29b-41d4-a716-446655440000") String shareToken,
        @Schema(description = "공유 링크 만료 시각(UTC)", example = "2026-09-03T00:00:00Z") Instant expiresAt
) {}
