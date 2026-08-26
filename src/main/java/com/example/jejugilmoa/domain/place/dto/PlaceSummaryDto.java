package com.example.jejugilmoa.domain.place.dto;

import java.math.BigDecimal;

public record PlaceSummaryDto(
    Long id,
    String name,
    String address,
    String imageUrl,
    String categoryName,
    BigDecimal latitude,
    BigDecimal longitude
) {}
