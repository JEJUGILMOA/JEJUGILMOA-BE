package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.converter.PlaceConverter;
import com.example.jejugilmoa.domain.place.dto.PlaceDetailDto;
import com.example.jejugilmoa.domain.place.dto.PlaceSummaryDto;
import com.example.jejugilmoa.domain.place.dto.PopularPlaceDto;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceQueryService {

    private final PlaceRepository placeRepository;
    private final PopularPlaceRepository popularPlaceRepository;
    private final PlaceConverter placeConverter;

    public PageResponse<PlaceSummaryDto> browse(String keyword, String categoryName, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : escapeLike(keyword.trim());
        String cat = (categoryName == null || categoryName.isBlank()) ? null : categoryName.trim();
        return PageResponse.of(placeRepository.search(kw, cat, pageable).map(placeConverter::toSummary));
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!")
                    .replace("%", "!%")
                    .replace("_", "!_");
    }

    @Cacheable(value = "popularPlaces", key = "#limit")
    public List<PopularPlaceDto> getPopular(int limit) {
        return popularPlaceRepository
            .findAllByOrderByVisitCountDesc(PageRequest.of(0, limit))
            .stream()
            .map(placeConverter::toPopular)
            .toList();
    }

    @Cacheable(value = "placeDetail", key = "#id")
    public PlaceDetailDto getDetail(Long id) {
        var place = placeRepository.findByIdAndPublishedTrue(id)
            .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));
        return placeConverter.toDetail(place);
    }
}
