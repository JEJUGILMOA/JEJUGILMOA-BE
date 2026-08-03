package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record NearbyPlaceRecommendResponse(

        @Schema(description = "추천 장소 목록 (최대 3개)")
        List<NearbyPlaceItem> recommendations,

        @Schema(description = "추가로 보여줄 TourAPI 후보가 있으면 true. false이면 Naver API fallback으로 전환.")
        boolean hasMore

) {}
