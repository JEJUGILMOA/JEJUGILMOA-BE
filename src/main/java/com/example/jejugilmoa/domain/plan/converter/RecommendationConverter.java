package com.example.jejugilmoa.domain.plan.converter;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.plan.dto.PlaceRecommendationItem;

public class RecommendationConverter {

    private RecommendationConverter() {}

    public static PlaceRecommendationItem toItem(Place place) {
        return new PlaceRecommendationItem(
                place.getId(),
                null,
                place.getName(),
                place.getCategory().getName(),
                place.getImageUrl(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude()
        );
    }
}
