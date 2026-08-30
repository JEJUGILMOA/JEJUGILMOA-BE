package com.example.jejugilmoa.domain.plan.dto;

public record FavoritePlaceResponse(
        Long placeId,
        String name,
        String category,
        String address,
        String imageUrl
) {
}
