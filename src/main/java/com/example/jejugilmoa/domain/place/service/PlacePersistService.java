package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PlaceHashtag;
import com.example.jejugilmoa.domain.place.entity.PlaceImage;
import com.example.jejugilmoa.domain.place.entity.PopularPlace;
import com.example.jejugilmoa.domain.place.repository.CategoryRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceHashtagRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceImageRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.global.external.tourapi.ClsSystem3Resolver;
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
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlacePersistService {

    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;
    private final PopularPlaceRepository popularPlaceRepository;
    private final PlaceHashtagRepository placeHashtagRepository;
    private final PlaceImageRepository placeImageRepository;
    private final GeometryFactory geometryFactory;
    private final ClsSystem3Resolver clsSystem3Resolver;

    private static final Map<String, double[]> SIGNGU_COORDS = Map.of(
        TourApiClient.SIGNGU_JEJU_SI,  new double[]{126.5312, 33.4996},
        TourApiClient.SIGNGU_SEOGWIPO, new double[]{126.5600, 33.2541}
    );

    private static final Map<String, String> CATEGORY_MAPPING = Map.of(
        "관광지", "자연",
        "음식",   "음식",
        "카페",   "카페"
    );

    /** cat2 중분류코드 → 카테고리명. ClsSystem3Resolver.OLD_CAT2_LABELS 코드체계 기준. */
    private static final Map<String, String> CAT2_TO_CATEGORY = Map.ofEntries(
        Map.entry("A0101", "자연"),   // 자연관광지
        Map.entry("A0102", "자연"),   // 동식물관광지
        Map.entry("A0201", "역사"),   // 역사관광지
        Map.entry("A0202", "자연"),   // 휴양관광지
        Map.entry("A0203", "체험"),   // 체험관광지
        Map.entry("A0204", "체험"),   // 산업관광지
        Map.entry("A0205", "역사"),   // 건축/조형물
        Map.entry("A0206", "역사"),   // 문화시설
        Map.entry("A0301", "체험"),   // 육상 레포츠
        Map.entry("A0302", "체험"),   // 수상 레포츠
        Map.entry("A0303", "체험"),   // 항공 레포츠
        Map.entry("A0304", "체험"),   // 복합 레포츠
        Map.entry("A0401", "음식"),   // 음식점
        Map.entry("A0402", "카페"),   // 카페/찻집
        Map.entry("A0501", "쇼핑"),   // 쇼핑몰
        Map.entry("A0502", "쇼핑")    // 상점
        // B02xx 숙박 — 제외 (매핑 없음 → skip)
    );

    /** contenttypeid → 카테고리명. cat2 매핑 불가 시 fallback으로만 사용. 기본값 없음. */
    private static final Map<String, String> CONTENT_TYPE_CATEGORY = Map.of(
        "12", "자연",   // 관광지 (cat2로 세분화 가능)
        "14", "역사",   // 문화시설
        "28", "체험",   // 레포츠
        "38", "쇼핑",
        "39", "음식"
        // "32" 숙박 — 제외
    );

    /** cat2 우선, contenttypeid fallback. 매핑 불가 시 null 반환 → 저장 대상 아님. */
    String deriveCategoryName(String cat2, String contenttypeid) {
        if (cat2 != null) {
            String cat = CAT2_TO_CATEGORY.get(cat2.trim());
            if (cat != null) return cat;
        }
        return contenttypeid != null ? CONTENT_TYPE_CATEGORY.get(contenttypeid.trim()) : null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveItems(String signguCd, List<TourListItem> items) {
        double[] coords = SIGNGU_COORDS.getOrDefault(signguCd, new double[]{126.5312, 33.4996});
        Map<String, Category> categoryCache = new HashMap<>();

        for (TourListItem item : items) {
            if (item.rlteTatsCd() == null || placeRepository.existsByExternalId(item.rlteTatsCd())) {
                continue;
            }

            String raw = item.rlteCtgryLclsNm();
            String categoryName = (raw != null) ? CATEGORY_MAPPING.get(raw) : null;
            if (categoryName == null) {
                log.debug("알 수 없는 카테고리 '{}', 건너뜀: {}", raw, item.rlteTatsNm());
                continue;
            }
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
    public int saveKorServiceItems(List<AreaBasedItem> items) {
        Map<String, Category> categoryCache = new HashMap<>();
        int count = 0;

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

            String categoryName = deriveCategoryName(item.cat2(), item.contenttypeid());
            if (categoryName == null) {
                log.debug("카테고리 결정 불가, 건너뜀: contentid={}, contenttypeid={}, cat2={}", item.contentid(), item.contenttypeid(), item.cat2());
                continue;
            }
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
                placeImageRepository.save(PlaceImage.builder()
                    .place(saved)
                    .imageUrl(item.firstimage())
                    .sequenceOrder(1)
                    .build());
            }
            popularPlaceRepository.findByPlace(saved).ifPresentOrElse(
                pp -> { /* 이미 있으면 유지 */ },
                () -> popularPlaceRepository.save(PopularPlace.builder()
                    .place(saved)
                    .visitCount(0)
                    .searchCount(0)
                    .build())
            );

            String mid = clsSystem3Resolver.resolveMid(item.cat2());
            String sub = clsSystem3Resolver.resolve(item.cat3());
            if (mid != null || sub != null) {
                placeHashtagRepository.save(PlaceHashtag.builder()
                    .place(saved)
                    .midLabel(mid)
                    .subLabel(sub)
                    .build());
            }
            count++;
        }
        return count;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int applyOverviews(Map<String, String> overviews) {
        List<Place> places = placeRepository.findByExternalIdIn(new ArrayList<>(overviews.keySet()));
        for (Place place : places) {
            place.updateCommonInfo(overviews.get(place.getExternalId()));
        }
        return places.size();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int applyImages(Map<String, List<String>> imageUrlMap) {
        List<Place> places = placeRepository.findByExternalIdIn(new ArrayList<>(imageUrlMap.keySet()));
        log.info("applyImages: 요청 {}건 externalId → DB 조회 {}건 place", imageUrlMap.size(), places.size());
        int count = 0;
        for (Place place : places) {
            if (place.isImageEnriched()) continue;
            List<String> urls = imageUrlMap.get(place.getExternalId());
            if (urls != null && !urls.isEmpty()) {
                placeImageRepository.deleteByPlace(place);
                for (int i = 0; i < Math.min(urls.size(), 3); i++) {
                    placeImageRepository.save(PlaceImage.builder()
                            .place(place)
                            .imageUrl(urls.get(i))
                            .sequenceOrder(i + 1)
                            .build());
                }
                log.info("이미지 저장: placeId={}, externalId={}, {}건", place.getId(), place.getExternalId(), Math.min(urls.size(), 3));
            } else {
                log.info("이미지 없음(API 반환 0건): placeId={}, externalId={}", place.getId(), place.getExternalId());
            }
            place.markImageEnriched();
            count++;
        }
        return count;
    }

    private String buildAddress(TourListItem item) {
        String signguNm = item.rlteSignguNm();
        return (signguNm != null && !signguNm.isBlank())
            ? "제주특별자치도 " + signguNm
            : "제주특별자치도";
    }
}
