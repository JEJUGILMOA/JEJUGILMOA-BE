package com.example.jejugilmoa.domain.home.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record HomeCourseResponse(

        @Schema(description = "코스 ID")
        Long courseId,

        @Schema(description = "대표 이미지 URL")
        String imageUrl,

        @Schema(description = "지역")
        String region,

        @Schema(description = "코스 제목")
        String title,

        @Schema(description = "코스 설명")
        String description,

        @Schema(description = "태그 목록")
        List<String> tags,

        @Schema(description = "총 예상 소요 시간 (분)")
        Integer estimatedMinutes,

        @Schema(description = "경유지 수")
        Integer placeCount,

        @Schema(description = "이동 수단 (WALK / DRIVE / MIXED)")
        String transportMode,

        @Schema(description = "코스 미리보기 (첫 3개 장소)")
        List<CoursePreviewItem> preview

) {

    public record CoursePreviewItem(
            @Schema(description = "장소 ID")
            Long placeId,
            @Schema(description = "장소 이미지 URL")
            String imageUrl
    ) {}
}
