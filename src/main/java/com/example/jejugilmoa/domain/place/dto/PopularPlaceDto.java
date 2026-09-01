package com.example.jejugilmoa.domain.place.dto;

import java.io.Serializable;
import java.util.List;

public record PopularPlaceDto(
    Long placeId,
    String name,
    String imageUrl,
    int visitCount,
    String region,
    List<String> hashtags,
    List<String> imageUrls
) implements Serializable {}
