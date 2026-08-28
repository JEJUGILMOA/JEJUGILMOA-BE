package com.example.jejugilmoa.domain.record.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TravelRecordPlaceResponse(
        Long recordPlaceId,
        Long placeId,
        String placeName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate visitDate,
        int sequenceOrder,
        boolean visited,
        LocalDateTime visitedAt,
        String memo,
        Integer stayMinutes,
        Integer rating,
        List<TravelRecordImageResponse> images
) {
}
