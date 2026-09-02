package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.converter.PlaceConverter;
import com.example.jejugilmoa.domain.place.dto.PlaceDetailDto;
import com.example.jejugilmoa.domain.place.dto.PlaceSummaryDto;
import com.example.jejugilmoa.domain.place.dto.PopularPlaceDto;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PlaceHashtag;
import com.example.jejugilmoa.domain.place.entity.PlaceImage;
import com.example.jejugilmoa.domain.place.entity.PopularPlace;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.repository.PlaceHashtagRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceImageRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.tourapi.KorServiceClient;
import com.example.jejugilmoa.global.external.tourapi.dto.AreaBasedItem;
import com.example.jejugilmoa.global.external.tourapi.dto.DetailCommonItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceQueryService {

    private final PlaceRepository placeRepository;
    private final PopularPlaceRepository popularPlaceRepository;
    private final PlaceHashtagRepository placeHashtagRepository;
    private final PlaceImageRepository placeImageRepository;
    private final PlaceConverter placeConverter;
    private final KorServiceClient korServiceClient;
    private final PlacePersistService placePersistService;

    public PageResponse<PlaceSummaryDto> browse(String keyword, String categoryName, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return searchByKeyword(keyword.trim(), pageable);
        }
        String cat = (categoryName == null || categoryName.isBlank()) ? null : categoryName.trim();
        Page<Place> places = cat == null
            ? placeRepository.findByPublishedTrue(pageable)
            : placeRepository.findByCategoryNameAndPublishedTrue(cat, pageable);
        return PageResponse.of(places.map(placeConverter::toSummary));
    }

    private PageResponse<PlaceSummaryDto> searchByKeyword(String keyword, Pageable pageable) {
        int pageNo = pageable.getPageNumber() + 1;
        int numOfRows = pageable.getPageSize();

        KorServiceClient.KeywordSearchPage searchPage;
        try {
            searchPage = korServiceClient.searchKeyword2(keyword, pageNo, numOfRows);
        } catch (Exception e) {
            log.warn("searchKeyword2 TourAPI 실패, DB 검색으로 대체: keyword={}", keyword, e);
            return PageResponse.of(
                placeRepository.search(escapeLike(keyword), null, pageable).map(placeConverter::toSummary)
            );
        }

        List<AreaBasedItem> apiItems = searchPage.items();
        if (!apiItems.isEmpty()) {
            placePersistService.saveKorServiceItems(apiItems);
        }

        List<String> externalIds = apiItems.stream()
            .map(AreaBasedItem::contentid)
            .filter(id -> id != null && !id.isBlank())
            .toList();

        long total = searchPage.totalCount();
        int totalPages = numOfRows > 0 ? (int) Math.ceil((double) total / numOfRows) : 0;

        if (externalIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), numOfRows, total, totalPages, true);
        }

        List<Place> places = placeRepository.findByExternalIdIn(externalIds);
        Map<String, Place> byExId = places.stream()
            .collect(Collectors.toMap(Place::getExternalId, p -> p));
        List<PlaceSummaryDto> dtos = externalIds.stream()
            .map(byExId::get)
            .filter(Objects::nonNull)
            .map(placeConverter::toSummary)
            .toList();

        boolean isLast = pageNo >= totalPages;
        return new PageResponse<>(dtos, pageable.getPageNumber(), numOfRows, total, totalPages, isLast);
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!")
                    .replace("%", "!%")
                    .replace("_", "!_");
    }

    public PageResponse<PopularPlaceDto> getPopular(int page, int size, String category) {
        Pageable pageable = PageRequest.of(page, size);
        String cat = (category == null || category.isBlank()) ? null : category.trim();

        Page<PopularPlace> ppPage = cat == null
            ? popularPlaceRepository.findAllWithPlaceOrderByVisitCountDesc(pageable)
            : popularPlaceRepository.findByCategoryNameWithPlaceOrderByVisitCountDesc(cat, pageable);

        List<PopularPlace> pps = ppPage.getContent();
        List<Long> placeIds = pps.stream().map(pp -> pp.getPlace().getId()).toList();

        Map<Long, PlaceHashtag> hashtagMap = placeHashtagRepository.findByPlace_IdIn(placeIds)
            .stream().collect(Collectors.toMap(ht -> ht.getPlace().getId(), ht -> ht));

        Map<Long, List<PlaceImage>> imageMap = loadAndEnrichImages(pps, placeIds);

        List<PopularPlaceDto> dtos = pps.stream()
            .map(pp -> placeConverter.toPopular(
                pp,
                hashtagMap.get(pp.getPlace().getId()),
                imageMap.get(pp.getPlace().getId())
            ))
            .toList();

        return new PageResponse<>(dtos, ppPage.getNumber(), ppPage.getSize(),
            ppPage.getTotalElements(), ppPage.getTotalPages(), ppPage.isLast());
    }

    @Cacheable(value = "placeDetail", key = "#id")
    public PlaceDetailDto getDetail(Long id) {
        var place = placeRepository.findByIdAndPublishedTrue(id)
            .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));

        List<PlaceImage> images = placeImageRepository.findByPlace_IdIn(List.of(id));

        if (images.size() < 3 && !place.isImageEnriched() && place.getExternalId() != null) {
            log.info("상세 조회 이미지 보강: placeId={}, contentId={}", id, place.getExternalId());
            korServiceClient.detailImage2(place.getExternalId()).ifPresent(urls -> {
                placePersistService.applyImages(Map.of(place.getExternalId(), urls));
            });
            images = placeImageRepository.findByPlace_IdIn(List.of(id));
        }

        String description = place.getDescription();
        if (description == null && place.getExternalId() != null) {
            log.info("상세 조회 개요 보강: placeId={}, contentId={}", id, place.getExternalId());
            DetailCommonItem common = korServiceClient.detailCommon2(place.getExternalId());
            if (common != null && common.overview() != null && !common.overview().isBlank()) {
                description = common.overview();
                placePersistService.applyOverviews(Map.of(place.getExternalId(), description));
            }
        }

        return placeConverter.toDetail(place, images, description);
    }

    private Map<Long, List<PlaceImage>> loadAndEnrichImages(List<PopularPlace> pps, List<Long> placeIds) {
        Map<Long, List<PlaceImage>> imageMap = placeImageRepository.findByPlace_IdIn(placeIds)
            .stream().collect(Collectors.groupingBy(img -> img.getPlace().getId()));

        List<PopularPlace> needEnrichment = pps.stream()
            .filter(pp -> {
                List<PlaceImage> imgs = imageMap.getOrDefault(pp.getPlace().getId(), List.of());
                return imgs.size() < 3 && !pp.getPlace().isImageEnriched()
                        && pp.getPlace().getExternalId() != null;
            })
            .toList();

        if (needEnrichment.isEmpty()) return imageMap;

        log.info("인기 장소 이미지 보강 시작: {}건", needEnrichment.size());

        List<CompletableFuture<Map.Entry<String, List<String>>>> futures = needEnrichment.stream()
            .map(pp -> CompletableFuture.supplyAsync(() -> {
                String externalId = pp.getPlace().getExternalId();
                return korServiceClient.detailImage2(externalId)
                    .map(urls -> Map.entry(externalId, urls))
                    .orElse(null);
            }))
            .toList();

        Map<String, List<String>> fetchedMap = futures.stream()
            .map(CompletableFuture::join)
            .filter(e -> e != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!fetchedMap.isEmpty()) {
            placePersistService.applyImages(fetchedMap);
        }

        return placeImageRepository.findByPlace_IdIn(placeIds)
            .stream().collect(Collectors.groupingBy(img -> img.getPlace().getId()));
    }
}
