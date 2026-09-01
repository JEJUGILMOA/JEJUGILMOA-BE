package com.example.jejugilmoa.domain.place.converter;

import com.example.jejugilmoa.domain.place.dto.PlaceDetailDto;
import com.example.jejugilmoa.domain.place.dto.PlaceSummaryDto;
import com.example.jejugilmoa.domain.place.dto.PopularPlaceDto;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PlaceHashtag;
import com.example.jejugilmoa.domain.place.entity.PlaceImage;
import com.example.jejugilmoa.domain.place.entity.PopularPlace;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class PlaceConverter {

    public PlaceSummaryDto toSummary(Place p) {
        return new PlaceSummaryDto(
            p.getId(),
            p.getName(),
            p.getAddress(),
            p.getImageUrl(),
            p.getCategory() != null ? p.getCategory().getName() : null,
            p.getLatitude(),
            p.getLongitude()
        );
    }

    public PlaceDetailDto toDetail(Place p, List<PlaceImage> images, String description) {
        List<String> imageUrls = images.stream()
            .sorted(Comparator.comparingInt(PlaceImage::getSequenceOrder))
            .map(PlaceImage::getImageUrl)
            .toList();
        return new PlaceDetailDto(
            p.getId(),
            p.getName(),
            p.getAddress(),
            p.getLatitude(),
            p.getLongitude(),
            description,
            p.getImageUrl(),
            imageUrls,
            p.getCategory() != null ? p.getCategory().getName() : null,
            description
        );
    }

    public PopularPlaceDto toPopular(PopularPlace pp, PlaceHashtag hashtag, List<PlaceImage> images) {
        Place place = pp.getPlace();
        String mid = hashtag != null ? hashtag.getMidLabel() : null;
        List<String> imageUrls = (images != null && !images.isEmpty())
                ? images.stream()
                    .sorted(Comparator.comparingInt(PlaceImage::getSequenceOrder))
                    .map(PlaceImage::getImageUrl)
                    .toList()
                : null;
        return new PopularPlaceDto(
            place.getId(),
            place.getName(),
            place.getImageUrl(),
            pp.getVisitCount(),
            stripJejuPrefix(place.getAddress()),
            mid != null ? List.of(mid) : null,
            imageUrls
        );
    }

    private static String stripJejuPrefix(String address) {
        if (address == null || address.isBlank()) return null;
        String stripped = address.replace("제주특별자치도", "").strip();
        return stripped.isBlank() ? null : stripped;
    }

}
