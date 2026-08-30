package com.example.jejugilmoa.domain.plan.converter;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.plan.dto.FavoritePlaceResponse;
import com.example.jejugilmoa.domain.plan.entity.Favorite;

public final class FavoriteConverter {

    private FavoriteConverter() {
    }

    public static FavoritePlaceResponse toResponse(Favorite favorite) {
        Place place = favorite.getPlace();
        return new FavoritePlaceResponse(
                place.getId(),
                place.getName(),
                place.getCategory().getName(),
                place.getAddress(),
                place.getImageUrl()
        );
    }
}
