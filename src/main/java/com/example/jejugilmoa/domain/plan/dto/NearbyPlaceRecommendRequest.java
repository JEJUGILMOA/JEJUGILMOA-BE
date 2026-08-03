package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record NearbyPlaceRecommendRequest(

        @Schema(description = "경유지 좌표 목록 (1개 이상)")
        @NotEmpty(message = "경유지 좌표를 1개 이상 입력해주세요.")
        @Valid
        List<WaypointCoord> waypoints,

        @Schema(description = "이미 노출된 추천 장소의 TourAPI contentId 목록. 다시 추천 시 누적 전달.",
                example = "[\"126508\", \"264570\"]")
        List<String> excludeContentIds

) {
    public record WaypointCoord(
            @Schema(description = "위도", example = "33.4996") double lat,
            @Schema(description = "경도", example = "126.5312") double lng
    ) {}

    @Override
    public List<String> excludeContentIds() {
        return excludeContentIds != null ? excludeContentIds : List.of();
    }
}
