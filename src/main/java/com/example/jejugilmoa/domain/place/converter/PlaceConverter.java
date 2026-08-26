package com.example.jejugilmoa.domain.place.converter;

import com.example.jejugilmoa.domain.place.dto.PlaceDetailDto;
import com.example.jejugilmoa.domain.place.dto.PlaceSummaryDto;
import com.example.jejugilmoa.domain.place.dto.PopularPlaceDto;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PopularPlace;
import org.springframework.stereotype.Component;

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

    public PlaceDetailDto toDetail(Place p) {
        return new PlaceDetailDto(
            p.getId(),
            p.getName(),
            p.getAddress(),
            p.getLatitude(),
            p.getLongitude(),
            p.getDescription(),
            p.getImageUrl(),
            List.of(),  // TarRlteTarService1 미제공 — 추후 PlaceImage 테이블로 확장
            p.getCategory() != null ? p.getCategory().getName() : null,
            null,  // homepage: TarRlteTarService1 미제공
            null,  // tel: TarRlteTarService1 미제공
            p.getDescription()
        );
    }

    public PopularPlaceDto toPopular(PopularPlace pp) {
        return new PopularPlaceDto(
            pp.getPlace().getId(),
            pp.getPlace().getName(),
            pp.getPlace().getImageUrl(),
            pp.getVisitCount()
        );
    }
}
