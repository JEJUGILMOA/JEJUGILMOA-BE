package com.example.jejugilmoa.domain.place.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record PlaceDetailDto(
    Long id,
    String name,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String description,
    String imageUrl,
    List<String> images,
    String categoryName
) implements Serializable {}
