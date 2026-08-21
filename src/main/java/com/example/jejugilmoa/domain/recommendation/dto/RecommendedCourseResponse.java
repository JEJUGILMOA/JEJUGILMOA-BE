package com.example.jejugilmoa.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RecommendedCourseResponse(

        @Schema(description = "추천 코스 ID")
        Long courseId,

        @Schema(description = "코스 제목")
        String title,

        @Schema(description = "테마 (TravelTheme 값)")
        String theme,

        @Schema(description = "코스 설명")
        String description,

        @Schema(description = "담기 횟수")
        int copyCount,

        @Schema(description = "경유지 목록 (순서 오름차순)")
        List<CourseWaypointItem> waypoints

) {}
