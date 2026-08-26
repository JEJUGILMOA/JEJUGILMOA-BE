package com.example.jejugilmoa.domain.record.dto;

import com.example.jejugilmoa.domain.plan.enums.Visibility;

import java.time.Instant;

public record TravelRecordUpdateResponse(
        Long recordId,
        String title,
        String description,
        Visibility visibility,
        Instant updatedAt
) {
}
