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
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
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
import java.util.Optional;
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

    /** 여행 중 부채꼴 추천: B→C 방향 기준 ±60° (cos 60° = 0.5) */
    private static final double COS_HALF_ANGLE = 0.5;
    private static final double EARTH_RADIUS_M = 6_371_000.0;

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

    // 여행 중 부채꼴 추천 결과를 담는 값 객체
    private record TripRecommendContext(
            List<Place> candidates,
            String departureName,
            String destinationName,
            double[] referenceCoords  // 이동 시간 추정 기준 좌표 [경도, 위도], null이면 생략
    ) {}

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

        // 이미 담긴 경유지 목록 (place_id 수집 + DRAFT 앵커 구성에 재사용)
        List<TravelCourse> existingCourses = travelCourseRepository
                .findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(planId);
        List<Long> addedPlaceIds = existingCourses.stream()
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

        // 여행 진행 중이면 B→C 부채꼴 추천, 그 외는 기존 출발지→목적지 코리도 추천
        if (plan.getStatus() == TravelPlanStatus.IN_PROGRESS) {
            TripRecommendContext ctx = buildTripRecommendContext(plan, categoryIds, excludedIds);
            List<PlaceRecommendationItem> items = ctx.candidates().stream()
                    .map(p -> RecommendationConverter.toItem(p, ctx.referenceCoords(), plan.getTransportMode()))
                    .toList();
            return new RecommendationResponse(ctx.departureName(), ctx.destinationName(), items);
        }

        // DRAFT: 앵커 반경 + 코리도 합산 추천
        List<Place> waypointPlaces = existingCourses.stream().map(TravelCourse::getPlace).toList();
        List<Place> candidates = queryCandidates(
                plan.getDeparturePlace(), waypointPlaces, plan.getDestinationPlace(),
                categoryIds, excludedIds);

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
     * 여행 진행 중 B→C 부채꼴 추천 컨텍스트를 구성합니다.
     *
     * <ul>
     *   <li>B = 마지막으로 방문 인증한 경유지. 아직 없으면 출발지 Place 사용.</li>
     *   <li>C = 다음 미방문 경유지.</li>
     *   <li>C가 없거나(전 경유지 방문 완료) B 좌표가 없으면 인기순 폴백.</li>
     *   <li>부채꼴 결과가 없어도 인기순 폴백.</li>
     * </ul>
     */
    private TripRecommendContext buildTripRecommendContext(
            TravelPlan plan, List<Long> categoryIds, List<Long> excludedIds) {

        // C: 다음 미방문 경유지
        Optional<TravelCourse> nextOpt = travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedFalseOrderByVisitDateAscSequenceOrderAsc(plan.getId());

        if (nextOpt.isEmpty()) {
            // 모든 경유지 방문 완료 — 인기 기반 폴백
            return new TripRecommendContext(
                    placeRepository.findByCategoriesOrderByPopularity(categoryIds, excludedIds, RECOMMENDATION_LIMIT),
                    null, null, null);
        }

        Place c = nextOpt.get().getPlace();

        // B: 마지막 방문 경유지, 없으면 출발지 Place
        Optional<TravelCourse> lastOpt = travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedTrueOrderByVisitDateDescSequenceOrderDesc(plan.getId());

        Place b;
        String departureName;
        if (lastOpt.isPresent()) {
            b = lastOpt.get().getPlace();
            departureName = b.getName();
        } else {
            b = plan.getDeparturePlace();
            departureName = b != null ? b.getName() : plan.getDepartureLocationName();
        }

        String destinationName = c.getName();

        if (b == null || b.getLatitude() == null || c.getLatitude() == null) {
            return new TripRecommendContext(
                    placeRepository.findByCategoriesOrderByPopularity(categoryIds, excludedIds, RECOMMENDATION_LIMIT),
                    departureName, destinationName, null);
        }

        double bLon = b.getLongitude().doubleValue();
        double bLat = b.getLatitude().doubleValue();
        double cLon = c.getLongitude().doubleValue();
        double cLat = c.getLatitude().doubleValue();
        double radiusMeters = haversineMeters(bLat, bLon, cLat, cLon);
        double[] refCoords = {bLon, bLat};

        List<Place> results = placeRepository.findInSector(
                bLon, bLat, cLon, cLat,
                radiusMeters, COS_HALF_ANGLE,
                categoryIds, excludedIds, RECOMMENDATION_LIMIT);

        if (results.isEmpty()) {
            results = placeRepository.findByCategoriesOrderByPopularity(categoryIds, excludedIds, RECOMMENDATION_LIMIT);
        }

        return new TripRecommendContext(results, departureName, destinationName, refCoords);
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * DRAFT 상태 경유지 추천.
     *
     * <ul>
     *   <li>출발지·도착지 좌표가 모두 있으면: 앵커 반경 OR 코리도 합집합 ({@code findNearAnchorsOrCorridor})</li>
     *   <li>둘 중 하나만 있으면: 앵커 반경만 ({@code findNearAnchors})</li>
     *   <li>좌표 있는 앵커가 없으면: 인기도 폴백</li>
     * </ul>
     */
    private List<Place> queryCandidates(
            Place departurePlace, List<Place> waypointPlaces, Place destinationPlace,
            List<Long> categoryIds, List<Long> excludedIds) {

        List<Place> allAnchors = new ArrayList<>();
        if (departurePlace != null) allAnchors.add(departurePlace);
        allAnchors.addAll(waypointPlaces);
        if (destinationPlace != null) allAnchors.add(destinationPlace);

        List<Place> withCoords = allAnchors.stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .toList();

        if (withCoords.isEmpty()) {
            return placeRepository.findByCategoriesOrderByPopularity(categoryIds, excludedIds, RECOMMENDATION_LIMIT);
        }

        String wkt = buildMultiPointWkt(withCoords);

        boolean hasDept = departurePlace != null && departurePlace.getLatitude() != null;
        boolean hasDest = destinationPlace != null && destinationPlace.getLatitude() != null;

        if (hasDept && hasDest) {
            return placeRepository.findNearAnchorsOrCorridor(
                    wkt,
                    departurePlace.getLongitude().doubleValue(), departurePlace.getLatitude().doubleValue(),
                    destinationPlace.getLongitude().doubleValue(), destinationPlace.getLatitude().doubleValue(),
                    CORRIDOR_WIDTH_METERS,
                    categoryIds, excludedIds, RECOMMENDATION_LIMIT);
        }

        return placeRepository.findNearAnchors(wkt, CORRIDOR_WIDTH_METERS, categoryIds, excludedIds, RECOMMENDATION_LIMIT);
    }

    /** MULTIPOINT WKT 생성. 경도 우선, 위도 후순 (ADR-0002). */
    private String buildMultiPointWkt(List<Place> places) {
        String points = places.stream()
                .map(p -> "(" + p.getLongitude().doubleValue() + " " + p.getLatitude().doubleValue() + ")")
                .collect(Collectors.joining(","));
        return "MULTIPOINT(" + points + ")";
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
     * 경유지 좌표 기반 주변 추천 장소 조회 (TourAPI locationBasedList2).
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
                        return korServiceClient.locationBasedList2(lat, lng, NEARBY_RADIUS_METERS, NEARBY_NUM_OF_ROWS);
                    } catch (TourApiException e) {
                        log.warn("locationBasedList2 호출 실패: lat={}, lng={}", lat, lng, e);
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

        // 이미 추가된 장소, 이전 노출 장소, 좌표 파싱 불가 항목 제외
        List<LocationBasedItem> candidates = deduplicated.values().stream()
                .filter(item -> !ctx.addedExternalIds().contains(item.contentid()))
                .filter(item -> !excludeSet.contains(item.contentid()))
                .filter(item -> isValidCoord(item.mapx()) && isValidCoord(item.mapy()))
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
                Double.parseDouble(item.mapx()),
                Double.parseDouble(item.mapy())
        );
    }

    private boolean isValidCoord(String value) {
        if (value == null || value.isBlank()) return false;
        try { Double.parseDouble(value); return true; } catch (NumberFormatException e) { return false; }
    }

    private int parseTypeId(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return -1; }
    }

    private int parseDist(String value) {
        if (value == null || value.isBlank()) return Integer.MAX_VALUE;
        try { return (int) Double.parseDouble(value); } catch (NumberFormatException e) { return Integer.MAX_VALUE; }
    }

    private void verifyOwner(TravelPlan plan, Long userId) {
        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        }
    }
}
