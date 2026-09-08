package com.example.jejugilmoa.domain.recommendation.dto;

import com.example.jejugilmoa.domain.recommendation.enums.CourseSourceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SavedCourseDetailResponse(

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
        Integer estimatedMinutes,

        @Schema(description = "주요 이동 수단. RECORD 타입은 null.")
        String transportMode,

        @Schema(description = "코스 설명. RECORD 타입은 null.")
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

            @Schema(description = "다음 장소까지 이동 시간 (분). 마지막 장소 및 RECORD 타입은 null.")
            Integer travelTimeToNext

    ) {}
}
