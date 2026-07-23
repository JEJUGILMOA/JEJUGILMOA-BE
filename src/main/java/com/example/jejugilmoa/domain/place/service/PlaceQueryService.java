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

    public PageResponse<PlaceSummaryDto> browse(String categoryName, Pageable pageable) {
        var page = (categoryName == null || categoryName.isBlank())
            ? placeRepository.findByPublishedTrue(pageable)
            : placeRepository.findByCategoryNameAndPublishedTrue(categoryName, pageable);
        return PageResponse.of(page.map(placeConverter::toSummary));
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
