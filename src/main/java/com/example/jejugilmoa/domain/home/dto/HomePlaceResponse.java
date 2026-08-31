package com.example.jejugilmoa.domain.home.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record HomePlaceResponse(

        @Schema(description = "장소 ID")
        Long placeId,

        @Schema(description = "장소명")
        String name,

        @Schema(description = "카테고리")
        String categoryName,

        @Schema(description = "지역 (시군구)")
        String region,

        @Schema(description = "대표 이미지 URL")
        String imageUrl,

        @Schema(description = "장소 설명")
        String description,

        @Schema(description = "큐레이션 레이블 (TODAY_PICK / TRAVELER_PICK / null)")
        String curationLabel,

        @Schema(description = "평점")
        BigDecimal rating

) {}
