package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NearbyPlaceItem(

        @Schema(description = "TourAPI contentId", example = "126508")
        String contentId,

        @Schema(description = "TourAPI contentTypeId (12=관광지, 14=문화시설, 39=음식점 등)", example = "12")
        int contentTypeId,

        @Schema(description = "장소명", example = "제주 민속자연사박물관")
        String title,

        @Schema(description = "주소", example = "제주특별자치도 제주시 일주동로 17")
        String address,

        @Schema(description = "대표 이미지 URL")
        String imageUrl,

        @Schema(description = "중심 좌표로부터의 거리 (미터)", example = "320")
        int dist,

        @Schema(description = "경도 (WGS84)", example = "126.5312")
        double mapX,

        @Schema(description = "위도 (WGS84)", example = "33.4996")
        double mapY

) {}
