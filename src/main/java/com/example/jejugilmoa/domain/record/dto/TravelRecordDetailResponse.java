package com.example.jejugilmoa.domain.record.dto;

import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.enums.ReactionType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TravelRecordDetailResponse(
        Long recordId,
        String title,
        String description,
        Visibility visibility,
        LocalDate actualStartDate,
        LocalDate actualEndDate,
        Instant createdAt,
        Instant updatedAt,
        @io.swagger.v3.oas.annotations.media.Schema(description = "현재 썸네일 이미지 ID", nullable = true)
        Long thumbnailImageId,
        @io.swagger.v3.oas.annotations.media.Schema(description = "현재 썸네일 이미지 URL", nullable = true)
        String thumbnailUrl,
        TravelRecordAuthorResponse author,
        TravelRecordPlanLinkResponse plan,
        List<TravelRecordImageResponse> images,
        int imageCount,
        List<TravelRecordImageResponse> allImages,
        List<TravelRecordPlaceResponse> places,
        long likeCount,
        long dislikeCount,
        ReactionType myReaction
) {
}
