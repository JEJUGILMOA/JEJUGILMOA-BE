package com.example.jejugilmoa.domain.map.dto;

import java.math.BigDecimal;

public record MapPlaceDto(
    Long id,
    String name,
    String categoryName,
    String imageUrl,
    BigDecimal latitude,
    BigDecimal longitude
) {}
