package com.example.jejugilmoa.domain.badge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record BadgeEarnedResponse(

        @Schema(description = "뱃지 ID", example = "3")
        Long badgeId,

        @Schema(description = "뱃지명", example = "애월 단골")
        String name,

        @Schema(description = "설명", example = "애월 카페거리를 3번 방문했어요.")
        String description,

        @Schema(description = "뱃지 이미지 URL")
        String imageUrl,

        @Schema(description = "획득 시각") LocalDateTime acquiredAt

) {}
