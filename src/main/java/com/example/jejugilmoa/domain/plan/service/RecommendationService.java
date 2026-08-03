package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.converter.RecommendationConverter;
import com.example.jejugilmoa.domain.plan.dto.NearbyPlaceItem;
import com.example.jejugilmoa.domain.plan.dto.NearbyPlaceRecommendRequest;
import com.example.jejugilmoa.domain.plan.dto.NearbyPlaceRecommendResponse;
import com.example.jejugilmoa.domain.plan.dto.PlaceRecommendationItem;
import com.example.jejugilmoa.domain.plan.dto.RecommendationResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.tourapi.KorServiceClient;
import com.example.jejugilmoa.global.external.tourapi.TourApiException;
import com.example.jejugilmoa.global.external.tourapi.dto.LocationBasedItem;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    /**
     * 경로 좌우 탐색 범위 (미터).
     * 출발지-목적지 직선으로부터 이 거리 이내의 장소를 후보로 삼습니다.
     * 제주도 폭(약 41km)을 고려해 5km로 설정했습니다.
     */
    private static final double CORRIDOR_WIDTH_METERS = 5_000.0;

    /** 한 번에 반환할 추천 경유지 수 */
    private static final int RECOMMENDATION_LIMIT = 5;

    private static final int NEARBY_RADIUS_METERS = 5_000;
    private static final int NEARBY_NUM_OF_ROWS = 30;
    private static final int NEARBY_RESULT_SIZE = 3;

    // 우리 Category.name → TourAPI contentTypeId 매핑
    private static final Map<String, Set<Integer>> CATEGORY_CONTENT_TYPES = new HashMap<>(Map.of(
            "자연", Set.of(12, 28),   // 관광지, 레포츠
            "문화", Set.of(14),        // 문화시설
            "음식", Set.of(39),        // 음식점
            "숙박", Set.of(32),        // 숙박
            "쇼핑", Set.of(38),        // 쇼핑
            "축제", Set.of(15),        // 행사/공연/축제
            "코스", Set.of(25)         // 여행코스
    ));

    private final TravelPlanRepository travelPlanRepository;
    private final TravelCourseRepository travelCourseRepository;
    private final PlaceRepository placeRepository;
    private final KorServiceClient korServiceClient;
    private final PlatformTransactionManager txManager;

    private TransactionTemplate readOnlyTx;

    @PostConstruct
    void init() {
        readOnlyTx = new TransactionTemplate(txManager);
        readOnlyTx.setReadOnly(true);
    }

    // DB 조회 결과를 트랜잭션 밖으로 전달하기 위한 값 객체
    private record NearbyQueryContext(
            Set<String> addedExternalIds,
            Set<Integer> existingTypeIds,
            List<double[]> coords
    ) {}

    /**
     * 경유지를 추천합니다 (최초 추천 / 재추천 공통 진입점).
     *
     * <ol>
     *   <li>이미 추가된 경유지 ID + {@code additionalExcludedIds}를 합쳐 제외 목록을 만듭니다.</li>
     *   <li>출발지·목적지 모두 {@link Place} 엔티티를 가진 경우 PostGIS corridor 쿼리로 추천합니다.</li>
     *   <li>좌표가 없으면 인기도(visitor_count) 순 폴백 쿼리를 사용합니다.</li>
     * </ol>
     *
     * @param planId               여행 계획 ID
     * @param userId               요청 사용자 ID (소유권 검증)
     * @param additionalExcludedIds 건너뛸 장소 ID (재추천 시 전달, 최초 추천 시 빈 목록)
     */
    public RecommendationResponse recommend(Long planId, Long userId, List<Long> additionalExcludedIds) {
        // findByIdWithPreferences: preferredCategories → category 를 한 번에 페치해 N+1 방지
        TravelPlan plan = travelPlanRepository.findByIdWithPreferences(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        verifyOwner(plan, userId);

        // 이미 담긴 경유지의 place_id 수집
        List<Long> addedPlaceIds = travelCourseRepository
                .findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(planId)
                .stream()
                .map(c -> c.getPlace().getId())
                .toList();

        // 최종 제외 목록 = 이미 추가된 + 이번 요청에서 건너뛰는 장소
        // IN 절 빈 배열 오류 방지를 위해 비어 있으면 -1L 삽입 (DB에 id=-1인 행은 없음)
        List<Long> excludedIds = new ArrayList<>(addedPlaceIds);
        excludedIds.addAll(additionalExcludedIds);

        // 시작지점과 도착지점은 추천 후보에서 제외
        if (plan.getDeparturePlace() != null) {
            excludedIds.add(plan.getDeparturePlace().getId());
        }
        if (plan.getDestinationPlace() != null) {
            excludedIds.add(plan.getDestinationPlace().getId());
        }
        if (excludedIds.isEmpty()) excludedIds.add(-1L);

        // 선호 카테고리 ID (이번 여행 계획에 등록된 카테고리)
        List<Long> categoryIds = plan.getPreferredCategories().stream()
                .map(pref -> pref.getCategory().getId())
                .toList();
        if (categoryIds.isEmpty()) categoryIds = List.of(-1L); // 빈 IN 절 방지

        List<Place> candidates = queryCandidates(plan, categoryIds, excludedIds);

        // 이동 시간 추정을 위한 출발지 좌표 [경도, 위도]
        double[] deptLngLat = extractDepartureCoords(plan);

        List<PlaceRecommendationItem> items = candidates.stream()
                .map(p -> RecommendationConverter.toItem(p, deptLngLat, plan.getTransportMode()))
                .toList();

        String departureName = plan.getDeparturePlace() != null
                ? plan.getDeparturePlace().getName()
                : plan.getDepartureLocationName();
        String destinationName = plan.getDestinationPlace() != null
                ? plan.getDestinationPlace().getName()
                : plan.getDestinationLocationName();

        return new RecommendationResponse(departureName, destinationName, items);
    }

    /**
     * 출발지·목적지에 Place 엔티티가 모두 있으면 PostGIS corridor 쿼리,
     * 그렇지 않으면 인기도 순 폴백 쿼리를 실행합니다.
     *
     * <p>corridor 쿼리: ST_DWithin으로 경로 선분에서 {@value CORRIDOR_WIDTH_METERS}m 이내
     * 장소를 찾고, ST_LineLocatePoint로 출발지→목적지 방향 순서로 정렬합니다.</p>
     */
    private List<Place> queryCandidates(TravelPlan plan, List<Long> categoryIds, List<Long> excludedIds) {
        Place dept = plan.getDeparturePlace();
        Place dest = plan.getDestinationPlace();

        if (dept != null && dest != null) {
            // ST_MakePoint(경도, 위도) — 경도 우선 (ADR-0002)
            return placeRepository.findAlongCorridor(
                    dept.getLongitude().doubleValue(),
                    dept.getLatitude().doubleValue(),
                    dest.getLongitude().doubleValue(),
                    dest.getLatitude().doubleValue(),
                    CORRIDOR_WIDTH_METERS,
                    categoryIds,
                    excludedIds,
                    RECOMMENDATION_LIMIT
            );
        }

        // 텍스트 입력 출발지/목적지만 있는 경우 → 인기도 기반 폴백
        return placeRepository.findByCategoriesOrderByPopularity(categoryIds, excludedIds, RECOMMENDATION_LIMIT);
    }

    /**
     * 출발지 Place 가 있으면 [경도, 위도] 배열 반환, 없으면 null 반환.
     * null 이면 RecommendationConverter 에서 이동 시간 추정을 건너뜁니다.
     */
    private double[] extractDepartureCoords(TravelPlan plan) {
        Place dept = plan.getDeparturePlace();
        if (dept == null) return null;
        return new double[]{dept.getLongitude().doubleValue(), dept.getLatitude().doubleValue()};
    }

    /**
     * 경유지 좌표 기반 주변 추천 장소 조회 (TourAPI locationBasedList1).
     *   각 경유지 좌표로 TourAPI를 병렬 호출해 반경 5km 내 관광정보를 가져옵니다.
     *   이미 추가된 경유지와 이전에 노출된 장소({@code excludeContentIds})를 제외합니다.
     *   현재 코스에 없는 카테고리를 우선 추천하고, 부족하면 같은 카테고리로 보충합니다.
     *   최대 3개를 반환하며, 추가 후보가 남아 있으면 {@code hasMore=true}로 응답합니다.
     */
    public NearbyPlaceRecommendResponse recommendNearby(Long planId, Long userId, NearbyPlaceRecommendRequest request) {
        // DB 조회만 트랜잭션 안에서 수행하고 커넥션을 즉시 반납
        NearbyQueryContext ctx = readOnlyTx.execute(status -> {
            TravelPlan plan = travelPlanRepository.findById(planId)
                    .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
            verifyOwner(plan, userId);

            List<TravelCourse> courses = travelCourseRepository
                    .findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(planId);

            Set<String> addedExternalIds = courses.stream()
                    .map(c -> c.getPlace().getExternalId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<Integer> existingTypeIds = courses.stream()
                    .map(c -> c.getPlace().getCategory().getName())
                    .flatMap(name -> CATEGORY_CONTENT_TYPES.getOrDefault(name, Set.of()).stream())
                    .collect(Collectors.toSet());

            List<double[]> coords = courses.stream()
                    .map(TravelCourse::getPlace)
                    .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                    .map(p -> new double[]{p.getLatitude().doubleValue(), p.getLongitude().doubleValue()})
                    .toList();

            return new NearbyQueryContext(addedExternalIds, existingTypeIds, coords);
        });

        // 트랜잭션 종료 후 TourAPI 병렬 호출
        Set<String> excludeSet = new HashSet<>(request.excludeContentIds());

        List<CompletableFuture<List<LocationBasedItem>>> futures = ctx.coords().stream()
                .map(latLng -> CompletableFuture.supplyAsync(() -> {
                    double lat = latLng[0], lng = latLng[1];
                    try {
                        return korServiceClient.locationBasedList1(lat, lng, NEARBY_RADIUS_METERS, NEARBY_NUM_OF_ROWS);
                    } catch (TourApiException e) {
                        log.warn("locationBasedList1 호출 실패: lat={}, lng={}", lat, lng, e);
                        return List.<LocationBasedItem>of();
                    }
                }))
                .toList();

        // contentId 기준 중복 제거 (먼저 나온 것 우선)
        Map<String, LocationBasedItem> deduplicated = new LinkedHashMap<>();
        futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .forEach(item -> deduplicated.putIfAbsent(item.contentid(), item));

        // 이미 추가된 장소 및 이전 노출 장소 제외
        List<LocationBasedItem> candidates = deduplicated.values().stream()
                .filter(item -> !ctx.addedExternalIds().contains(item.contentid()))
                .filter(item -> !excludeSet.contains(item.contentid()))
                .toList();

        boolean hasMore = candidates.size() > NEARBY_RESULT_SIZE;

        // 카테고리 우선순위 분리
        List<LocationBasedItem> priority = new ArrayList<>();
        List<LocationBasedItem> fallback = new ArrayList<>();
        for (LocationBasedItem item : candidates) {
            if (!ctx.existingTypeIds().contains(parseTypeId(item.contenttypeid()))) {
                priority.add(item);
            } else {
                fallback.add(item);
            }
        }

        // 거리순 정렬 후 최대 3개 선택
        List<LocationBasedItem> selected = new ArrayList<>(
                priority.stream()
                        .sorted(Comparator.comparingInt(i -> parseDist(i.dist())))
                        .limit(NEARBY_RESULT_SIZE)
                        .toList()
        );
        if (selected.size() < NEARBY_RESULT_SIZE) {
            fallback.stream()
                    .sorted(Comparator.comparingInt(i -> parseDist(i.dist())))
                    .limit(NEARBY_RESULT_SIZE - selected.size())
                    .forEach(selected::add);
        }

        List<NearbyPlaceItem> recommendations = selected.stream()
                .map(this::toNearbyPlaceItem)
                .toList();

        return new NearbyPlaceRecommendResponse(recommendations, hasMore);
    }

    private NearbyPlaceItem toNearbyPlaceItem(LocationBasedItem item) {
        return new NearbyPlaceItem(
                item.contentid(),
                parseTypeId(item.contenttypeid()),
                item.title(),
                item.addr1(),
                item.firstimage(),
                parseDist(item.dist()),
                parseDouble(item.mapx()),
                parseDouble(item.mapy())
        );
    }

    private int parseTypeId(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return -1; }
    }

    private int parseDist(String value) {
        if (value == null || value.isBlank()) return Integer.MAX_VALUE;
        try { return (int) Double.parseDouble(value); } catch (NumberFormatException e) { return Integer.MAX_VALUE; }
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try { return Double.parseDouble(value); } catch (NumberFormatException e) { return 0.0; }
    }

    private void verifyOwner(TravelPlan plan, Long userId) {
        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        }
    }
}
