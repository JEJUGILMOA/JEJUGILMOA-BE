package com.example.jejugilmoa.domain.record.dto;

public record TravelRecordImageResponse(
        Long imageId,
        String imageUrl,
        @io.swagger.v3.oas.annotations.media.Schema(
                description = "기록 또는 장소 이미지의 objectKey. 썸네일 선택 시 사용합니다.",
                example = "records/42/place-1.jpg")
        String objectKey,
        int sequenceOrder
) {
}
