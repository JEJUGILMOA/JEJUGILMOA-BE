package com.example.jejugilmoa.domain.record.dto;

import com.example.jejugilmoa.domain.plan.enums.Visibility;

import java.time.Instant;
import java.util.List;

public record TravelRecordMapResponse(
        Long recordId,
        String title,
        Visibility visibility,
        Instant createdAt,
        TravelRecordAuthorResponse author,
        List<TravelRecordMapPlaceResponse> places
) {
}
