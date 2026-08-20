package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TravelTheme;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RecommendationRequest(

        @Schema(description = "출발지 좌표 (앵커 추천 시 필요, 전역 추천 시 null 허용)")
        CoordDto departureCoord,

        @Schema(description = "선호경유지 좌표 목록 (비어 있으면 전역 랜덤 추천)")
        List<CoordDto> preferredWaypoints,

        @Schema(description = "이미 추가된 장소 ID 목록 (결과에서 제외)")
        List<Long> excludedPlaceIds,

        @Schema(description = "TourAPI 폴백 페이지네이션용 제외 contentId 목록")
        List<String> excludeContentIds,

        @Schema(description = "카테고리 필터 (null 또는 생략 시 전체 조회)", nullable = true)
        TravelTheme category

) {}
