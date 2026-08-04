package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record NearbyPlaceRecommendRequest(

        @Schema(description = "이미 노출된 추천 장소의 TourAPI contentId 목록. 다시 추천 시 누적 전달.",
                example = "[\"126508\", \"264570\"]")
        List<String> excludeContentIds

) {
    @Override
    public List<String> excludeContentIds() {
        return excludeContentIds != null ? excludeContentIds : List.of();
    }
}
