package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TravelTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RecommendationRequest(

        @Valid
        @Schema(description = "출발지 좌표 (앵커 추천 시 필요, 전역 추천 시 null 허용)")
        CoordDto departureCoord,

        @Valid
        @Size(max = 10, message = "선호 경유지는 최대 10개까지 지정할 수 있습니다.")
        @Schema(description = "선호경유지 좌표 목록 (비어 있으면 전역 랜덤 추천)")
        List<CoordDto> preferredWaypoints,

        @Size(max = 200, message = "제외 장소 ID는 최대 200개까지 지정할 수 있습니다.")
        @Schema(description = "이미 추가된 장소 ID 목록 (결과에서 제외)")
        List<Long> excludedPlaceIds,

        @Size(max = 200, message = "제외 contentId는 최대 200개까지 지정할 수 있습니다.")
        @Schema(description = "TourAPI 폴백 페이지네이션용 제외 contentId 목록")
        List<String> excludeContentIds,

        @Schema(description = "카테고리 필터 (null 또는 생략 시 전체 조회)", nullable = true)
        TravelTheme category

) {}
