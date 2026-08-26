package com.example.jejugilmoa.domain.record.dto;

import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.enums.ReactionType;

import java.time.Instant;
import java.time.LocalDate;

public record TravelRecordCardResponse(
        Long recordId,
        String title,
        String description,
        Visibility visibility,
        String thumbnailUrl,
        long visitedPlaceCount,
        long photoCount,
        long likeCount,
        long dislikeCount,
        ReactionType myReaction,
        LocalDate actualStartDate,
        LocalDate actualEndDate,
        Instant createdAt,
        TravelRecordAuthorResponse author
) {
}
