package com.example.jejugilmoa.domain.record.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TravelRecordMapPlaceResponse(
        Long recordPlaceId,
        Long placeId,
        String placeName,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate visitDate,
        int sequenceOrder,
        boolean visited
) {
}
