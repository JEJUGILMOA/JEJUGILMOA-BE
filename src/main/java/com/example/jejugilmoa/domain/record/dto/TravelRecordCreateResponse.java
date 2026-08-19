package com.example.jejugilmoa.domain.record.dto;

import com.example.jejugilmoa.domain.plan.enums.Visibility;

import java.time.Instant;

public record TravelRecordCreateResponse(
        Long recordId,
        Long tripId,
        String title,
        Visibility visibility,
        Instant createdAt
) {
}
