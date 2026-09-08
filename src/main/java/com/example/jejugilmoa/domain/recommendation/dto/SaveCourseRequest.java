package com.example.jejugilmoa.domain.recommendation.dto;

import com.example.jejugilmoa.domain.recommendation.enums.CourseSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record SaveCourseRequest(

        @Schema(description = "코스 출처 타입. RECOMMENDED(추천 코스) 또는 RECORD(여행 기록)")
        @NotNull(message = "코스 출처 타입은 필수입니다.")
        CourseSourceType sourceType,

        @Schema(description = "코스 ID. sourceType=RECOMMENDED이면 추천 코스 ID, sourceType=RECORD이면 여행 기록 ID")
        @NotNull(message = "코스 ID는 필수입니다.")
        Long sourceId

) {}
