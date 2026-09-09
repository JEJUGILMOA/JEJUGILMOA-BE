package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PlaceHashtag;
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

    /** cat2 중분류코드 → 카테고리명. 구분류(A0xxx)·신분류(NA/FD/SH/HS/EV/EX/LS/VE) 모두 포함. */
    private static final Map<String, String> CAT2_TO_CATEGORY = Map.ofEntries(
        // ── 구분류체계 (A0xxx) ──────────────────────────────────────────
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
        Map.entry("A0502", "쇼핑"),   // 상점
        // B02xx 숙박 — 제외

        // ── 신분류체계 ──────────────────────────────────────────────────
        // NA 자연관광
        Map.entry("NA01", "자연"),    // 자연경관(산)
        Map.entry("NA02", "자연"),    // 자연경관(하천·해양)
        Map.entry("NA03", "자연"),    // 자연생태
        Map.entry("NA04", "자연"),    // 자연공원
        Map.entry("NA05", "자연"),    // 기타자연관광
        // FD 음식
        Map.entry("FD01", "음식"),    // 한식
        Map.entry("FD02", "음식"),    // 외국식
        Map.entry("FD03", "음식"),    // 간이음식
        Map.entry("FD04", "음식"),    // 주점
        Map.entry("FD05", "카페"),    // 카페/찻집
        // SH 쇼핑
        Map.entry("SH01", "쇼핑"),    // 백화점
        Map.entry("SH02", "쇼핑"),    // 쇼핑몰
        Map.entry("SH03", "쇼핑"),    // 대형마트
        Map.entry("SH04", "쇼핑"),    // 면세점
        Map.entry("SH05", "쇼핑"),    // 전문매장/상가
        Map.entry("SH06", "쇼핑"),    // 시장
        Map.entry("SH07", "쇼핑"),    // 기타쇼핑시설
        // HS 역사관광
        Map.entry("HS01", "역사"),    // 역사유적지
        Map.entry("HS02", "역사"),    // 역사유물
        Map.entry("HS03", "역사"),    // 종교성지
        Map.entry("HS04", "역사"),    // 안보관광지
        // EV 축제/공연/행사
        Map.entry("EV01", "축제"),    // 축제
        Map.entry("EV02", "축제"),    // 공연
        Map.entry("EV03", "축제"),    // 행사
        // EX 체험관광
        Map.entry("EX01", "체험"),    // 전통체험
        Map.entry("EX02", "체험"),    // 공예체험
        Map.entry("EX03", "체험"),    // 농산어촌 체험
        Map.entry("EX04", "체험"),    // 산사체험
        Map.entry("EX05", "체험"),    // 웰니스관광
        Map.entry("EX06", "체험"),    // 산업관광
        Map.entry("EX07", "체험"),    // 기타체험
        // LS 레저스포츠
        Map.entry("LS01", "체험"),    // 육상레저스포츠
        Map.entry("LS02", "체험"),    // 수상레저스포츠
        Map.entry("LS03", "체험"),    // 항공레저스포츠
        Map.entry("LS04", "체험"),    // 복합레저스포츠
        // VE 문화관광 (contentTypeId 기준 분류)
        Map.entry("VE01", "자연"),    // 랜드마크관광 (건물·전망대 등, contentTypeId=12)
        Map.entry("VE02", "체험"),    // 테마공원 (테마파크·동물원, contentTypeId=12)
        Map.entry("VE03", "자연"),    // 도시공원 (contentTypeId=12)
        Map.entry("VE04", "자연"),    // 도시·지역문화관광 (골목길·둘레길, contentTypeId=12)
        Map.entry("VE05", "자연"),    // 복합관광시설 (관광단지, contentTypeId=12)
        Map.entry("VE06", "역사"),    // 공연시설 (contentTypeId=14)
        Map.entry("VE07", "역사"),    // 전시시설 (박물관·미술관, contentTypeId=14)
        Map.entry("VE08", "역사"),    // 행사시설 (contentTypeId=14)
        Map.entry("VE09", "역사"),    // 교육시설 (contentTypeId=14)
        Map.entry("VE10", "체험"),    // 레저스포츠시설 (contentTypeId=28)
        Map.entry("VE12", "역사"),    // 기타문화관광지 (contentTypeId=14)
        // AC 숙박
        Map.entry("AC01", "숙박"),    // 호텔
        Map.entry("AC02", "숙박"),    // 콘도미니엄
        Map.entry("AC03", "숙박"),    // 펜션/민박
        Map.entry("AC04", "숙박"),    // 모텔
        Map.entry("AC05", "숙박"),    // 캠핑 (야영장·글램핑)
        Map.entry("AC06", "숙박"),    // 호스텔/게스트하우스
        // B02xx 구분류 숙박
        Map.entry("B0201", "숙박"),   // 관광호텔
        Map.entry("B0202", "숙박"),   // 콘도미니엄
        Map.entry("B0204", "숙박"),   // 펜션
        Map.entry("B0206", "숙박"),   // 민박
        Map.entry("B0207", "숙박"),   // 게스트하우스
        Map.entry("B0210", "숙박")    // 야영장/캠핑
    );

    /** contenttypeid → 카테고리명. cat2 매핑 불가 시 fallback으로만 사용. 기본값 없음. */
    private static final Map<String, String> CONTENT_TYPE_CATEGORY = Map.of(
        "12", "자연",   // 관광지 (cat2로 세분화 가능)
        "14", "역사",   // 문화시설
        "28", "체험",   // 레포츠
        "32", "숙박",   // 숙박
        "38", "쇼핑",
        "39", "음식"
    );

    /** 신분류체계(lclsSystm2) 우선, 구분류체계(cat2), contenttypeid 순 fallback. 매핑 불가 시 null → 저장 대상 아님. */
    String deriveCategoryName(String lclsSystm2, String cat2, String contenttypeid) {
        if (lclsSystm2 != null) {
            String cat = CAT2_TO_CATEGORY.get(lclsSystm2.trim());
            if (cat != null) return cat;
        }
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

            String categoryName = deriveCategoryName(item.lclsSystm2(), item.cat2(), item.contenttypeid());
            if (categoryName == null) {
                log.debug("카테고리 결정 불가, 건너뜀: contentid={}, contenttypeid={}, lclsSystm2={}, cat2={}",
                    item.contentid(), item.contenttypeid(), item.lclsSystm2(), item.cat2());
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
                placeImageRepository.insertIgnore(saved.getId(), item.firstimage(), 1);
            }

            String mid = clsSystem3Resolver.resolveMid(
                item.lclsSystm2() != null ? item.lclsSystm2() : item.cat2());
            String sub = clsSystem3Resolver.resolve(
                item.lclsSystm3() != null ? item.lclsSystm3() : item.cat3());
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
            if (placeRepository.claimImageEnrichment(place.getId()) == 0) continue;
            List<String> urls = imageUrlMap.get(place.getExternalId());
            if (urls != null && !urls.isEmpty()) {
                placeImageRepository.deleteByPlace(place);
                for (int i = 0; i < Math.min(urls.size(), 3); i++) {
                    placeImageRepository.insertIgnore(place.getId(), urls.get(i), i + 1);
                }
                log.info("이미지 저장: placeId={}, externalId={}, {}건", place.getId(), place.getExternalId(), Math.min(urls.size(), 3));
            } else {
                log.info("이미지 없음(API 반환 0건): placeId={}, externalId={}", place.getId(), place.getExternalId());
            }
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
