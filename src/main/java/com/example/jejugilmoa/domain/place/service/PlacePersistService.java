package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PopularPlace;
import com.example.jejugilmoa.domain.place.repository.CategoryRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.global.external.tourapi.TourApiClient;
import com.example.jejugilmoa.global.external.tourapi.dto.TourListItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        "음식",   "음식"
    );

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveItems(String signguCd, List<TourListItem> items) {
        double[] coords = SIGNGU_COORDS.getOrDefault(signguCd, new double[]{126.5312, 33.4996});

        for (TourListItem item : items) {
            if (item.rlteTatsCd() == null || placeRepository.existsByExternalId(item.rlteTatsCd())) {
                continue;
            }

            String categoryName = CATEGORY_MAPPING.getOrDefault(item.rlteCtgryLclsNm(), "자연");
            var category = categoryRepository.findByName(categoryName).orElse(null);
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

    private String buildAddress(TourListItem item) {
        String signguNm = item.rlteSignguNm();
        return (signguNm != null && !signguNm.isBlank())
            ? "제주특별자치도 " + signguNm
            : "제주특별자치도";
    }
}
