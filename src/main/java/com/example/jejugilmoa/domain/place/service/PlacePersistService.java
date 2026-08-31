package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PopularPlace;
import com.example.jejugilmoa.domain.place.repository.CategoryRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.global.external.tourapi.TourApiClient;
import com.example.jejugilmoa.global.external.tourapi.dto.AreaBasedItem;
import com.example.jejugilmoa.global.external.tourapi.dto.TourListItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlacePersistService {

    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;
    private final PopularPlaceRepository popularPlaceRepository;
    private final GeometryFactory geometryFactory;

    // TarRlteTarService1은 좌표를 제공하지 않으므로 시군구 중심 좌표로 대체
    // 순서: [경도(lon), 위도(lat)] — ST_MakePoint(lon, lat)
    private static final Map<String, double[]> SIGNGU_COORDS = Map.of(
        TourApiClient.SIGNGU_JEJU_SI,  new double[]{126.5312, 33.4996},
        TourApiClient.SIGNGU_SEOGWIPO, new double[]{126.5600, 33.2541}
    );

    // rlteCtgryLclsNm → 내부 카테고리명 매핑
    private static final Map<String, String> CATEGORY_MAPPING = Map.of(
        "관광지", "자연",
        "음식",   "음식",
        "카페",   "카페"
    );

    // KorService2 contenttypeid → 내부 카테고리명 매핑
    private static final Map<String, String> CONTENT_TYPE_CATEGORY = Map.of(
        "12", "자연",
        "14", "자연",
        "28", "레포츠",
        "32", "숙박",
        "38", "쇼핑",
        "39", "음식"
    );

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveItems(String signguCd, List<TourListItem> items) {
        double[] coords = SIGNGU_COORDS.getOrDefault(signguCd, new double[]{126.5312, 33.4996});
        Map<String, Category> categoryCache = new HashMap<>();

        for (TourListItem item : items) {
            if (item.rlteTatsCd() == null || placeRepository.existsByExternalId(item.rlteTatsCd())) {
                continue;
            }

            String raw = item.rlteCtgryLclsNm();
            String categoryName = (raw != null) ? CATEGORY_MAPPING.getOrDefault(raw, "자연") : "자연";
            var category = categoryCache.computeIfAbsent(categoryName,
                name -> categoryRepository.findByName(name).orElse(null));
            if (category == null) {
                log.warn("카테고리 없음, 건너뜀: {}", categoryName);
                continue;
            }

            var geom = geometryFactory.createPoint(new Coordinate(coords[0], coords[1]));
            var place = Place.builder()
                .externalId(item.rlteTatsCd())
                .name(item.rlteTatsNm())
                .address(buildAddress(item))
                .latitude(BigDecimal.valueOf(coords[1]))
                .longitude(BigDecimal.valueOf(coords[0]))
                .geom(geom)
                .category(category)
                .published(true)
                .build();

            var saved = placeRepository.save(place);
            // rlteRank: 1이 가장 높은 연관순위 → visitCount로 변환 (rank 1 = 500점)
            upsertPopularPlace(saved, Math.max(1, 51 - item.rlteRank()) * 10);
        }
    }

    private void upsertPopularPlace(Place place, int initialScore) {
        popularPlaceRepository.findByPlace(place).ifPresentOrElse(
            pp -> { /* 이미 있으면 유지 */ },
            () -> popularPlaceRepository.save(PopularPlace.builder()
                .place(place)
                .visitCount(initialScore)
                .searchCount(0)
                .build())
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveKorServiceItems(List<AreaBasedItem> items) {
        Map<String, Category> categoryCache = new HashMap<>();

        for (AreaBasedItem item : items) {
            if (item.contentid() == null || placeRepository.existsByExternalId(item.contentid())) {
                continue;
            }

            double lng, lat;
            try {
                lng = Double.parseDouble(item.mapx());
                lat = Double.parseDouble(item.mapy());
            } catch (NumberFormatException e) {
                log.warn("좌표 파싱 실패: contentid={}, mapx={}, mapy={}", item.contentid(), item.mapx(), item.mapy());
                continue;
            }

            String categoryName = CONTENT_TYPE_CATEGORY.getOrDefault(item.contenttypeid(), "자연");
            var category = categoryCache.computeIfAbsent(categoryName,
                name -> categoryRepository.findByName(name).orElse(null));
            if (category == null) {
                log.warn("카테고리 없음, 건너뜀: {}", categoryName);
                continue;
            }

            var geom = geometryFactory.createPoint(new Coordinate(lng, lat));
            geom.setSRID(4326);
            var place = Place.builder()
                .externalId(item.contentid())
                .name(item.title())
                .address(item.addr1() != null ? item.addr1() : "제주특별자치도")
                .latitude(BigDecimal.valueOf(lat))
                .longitude(BigDecimal.valueOf(lng))
                .geom(geom)
                .category(category)
                .published(true)
                .build();

            var saved = placeRepository.save(place);
            if (item.firstimage() != null && !item.firstimage().isBlank()) {
                saved.updateImageUrl(item.firstimage());
            }
            popularPlaceRepository.findByPlace(saved).ifPresentOrElse(
                pp -> { /* 이미 있으면 유지 */ },
                () -> popularPlaceRepository.save(PopularPlace.builder()
                    .place(saved)
                    .visitCount(0)
                    .searchCount(0)
                    .build())
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int applyOverviews(Map<String, String> overviews) {
        List<Place> places = placeRepository.findByExternalIdIn(new ArrayList<>(overviews.keySet()));
        for (Place place : places) {
            place.updateCommonInfo(overviews.get(place.getExternalId()));
        }
        return places.size();
    }

    private String buildAddress(TourListItem item) {
        String signguNm = item.rlteSignguNm();
        return (signguNm != null && !signguNm.isBlank())
            ? "제주특별자치도 " + signguNm
            : "제주특별자치도";
    }
}
