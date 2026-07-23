package com.example.jejugilmoa.domain.place.dto;

import java.io.Serializable;

public record PopularPlaceDto(
    Long placeId,
    String name,
    String imageUrl,
    int visitCount
) implements Serializable {}
