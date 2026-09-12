package com.example.jejugilmoa.domain.recommendation.dto;

import com.example.jejugilmoa.domain.recommendation.enums.CourseSourceType;
import io.swagger.v3.oas.annotations.media.Schema;

public record SavedCourseListItemResponse(

        @Schema(description = "담은 코스 ID")
        Long savedCourseId,

        @Schema(description = "코스 출처 타입 (RECOMMENDED / RECORD)")
        CourseSourceType sourceType,

        @Schema(description = "코스 제목")
        String title,

        @Schema(description = "대표 이미지 URL")
        String imageUrl,

        @Schema(description = "지역")
        String region,

        @Schema(description = "총 장소 수")
        int placeCount,

        @Schema(description = "예상 소요 시간 (분). RECORD 타입은 null.")
        Integer estimatedMinutes

) {}
