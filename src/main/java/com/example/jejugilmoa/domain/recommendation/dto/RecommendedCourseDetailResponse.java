package com.example.jejugilmoa.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record RecommendedCourseDetailResponse(

        @Schema(description = "추천 코스 ID")
        Long courseId,

        @Schema(description = "대표 이미지 URL")
        String imageUrl,

        @Schema(description = "코스 제목")
        String title,

        @Schema(description = "지역 (예: 제주 한림읍)")
        String region,

        @Schema(description = "무료 여부")
        Boolean isFree,

        @Schema(description = "평점 (예: 4.8)")
        BigDecimal rating,

        @Schema(description = "주요 이동 수단 (예: WALK, DRIVE, MIXED)")
        String transportMode,

        @Schema(description = "총 장소 수")
        int placeCount,

        @Schema(description = "예상 소요 시간 (분)")
        Integer estimatedMinutes,

        @Schema(description = "코스 설명")
        String description,

        @Schema(description = "코스 순서 목록")
        List<CourseStopItem> stops

) {
    public record CourseStopItem(

            @Schema(description = "순서 번호 (1부터 시작)")
            int sequenceOrder,

            @Schema(description = "장소 ID")
            Long placeId,

            @Schema(description = "장소명")
            String placeName,

            @Schema(description = "장소 이미지 URL")
            String placeImageUrl,

            @Schema(description = "다음 장소까지 이동 시간 (분). 마지막 장소는 null.")
            Integer travelTimeToNext

    ) {}
}
