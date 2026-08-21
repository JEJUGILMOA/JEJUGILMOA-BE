package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RecommendationResponse(

        @Schema(description = "추천 경유지 목록 (최대 10개, DB결과 / TourAPI폴백 공통)")
        List<PlaceRecommendationItem> items,

        @Schema(description = "추가 추천 결과 존재 여부. true이면 excludedPlaceIds / excludeContentIds에 현재 결과를 추가해 재요청하면 새 결과를 받을 수 있습니다.")
        boolean hasMore

) {}
