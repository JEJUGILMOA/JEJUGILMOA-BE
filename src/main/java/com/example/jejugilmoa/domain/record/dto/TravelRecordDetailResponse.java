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
        TravelRecordAuthorResponse author,
        TravelRecordPlanLinkResponse plan,
        List<TravelRecordImageResponse> images,
        List<TravelRecordPlaceResponse> places,
        long likeCount,
        long dislikeCount,
        ReactionType myReaction
) {
}
