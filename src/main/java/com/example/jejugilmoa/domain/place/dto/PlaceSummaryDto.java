package com.example.jejugilmoa.domain.place.dto;

public record PlaceSummaryDto(
    Long id,
    String name,
    String address,
    String imageUrl,
    String categoryName
) {}
