package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.converter.RecommendationConverter;
import com.example.jejugilmoa.domain.plan.dto.*;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.enums.TravelTheme;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.tourapi.KorServiceClient;
import com.example.jejugilmoa.global.external.tourapi.TourApiException;
import com.example.jejugilmoa.global.external.tourapi.dto.LocationBasedItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private static final double COS_HALF_ANGLE = 0.5;
    private static final double EARTH_RADIUS_M = 6_371_000.0;
    private static final int RECOMMENDATION_LIMIT = 10;
    private static final int SINGLE_ANCHOR_RADIUS_METERS = 5_000;
    private static final int FALLBACK_RADIUS_METERS = 5_000;
    private static final int FALLBACK_NUM_OF_ROWS = 30;
    private static final int FALLBACK_RESULT_SIZE = 10;
    private static final long FALLBACK_TIMEOUT_SECONDS = 12L;

    private static final Executor FALLBACK_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "tourapi-fallback");
        t.setDaemon(true);
        return t;
    });

    private final TravelPlanRepository travelPlanRepository;
    private final TravelCourseRepository travelCourseRepository;
    private final PlaceRepository placeRepository;
    private final KorServiceClient korServiceClient;

    // 여행 중 부채꼴 추천 결과를 담는 값 객체
    private record TripRecommendContext(List<Place> candidates) {}

    /**
     * planId 기반 추천 (GET/POST /api/plans/{planId}/recommendations 공통 진입점).
     *
     * - DRAFT: 선호경유지 앵커 기반 → recommendStateless 위임
     * - IN_PROGRESS: B→C 부채꼴 + 카테고리 필터
     */
    public RecommendationResponse recommend(Long planId, Long userId, List<Long> additionalExcludedIds) {
        TravelPlan plan = travelPlanRepository.findByIdWithPreferences(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        verifyOwner(plan, userId);

        List<TravelCourse> existingCourses = travelCourseRepository
                .findAllByTravelPlanIdWithPlaceOrderByVisitDateAscSequenceOrderAsc(planId);

        List<Long> excludedIds = new ArrayList<>();
        existingCourses.forEach(c -> excludedIds.add(c.getPlace().getId()));
        excludedIds.addAll(additionalExcludedIds);
        if (plan.getDeparturePlace() != null) excludedIds.add(plan.getDeparturePlace().getId());
        if (excludedIds.isEmpty()) excludedIds.add(-1L);

        if (plan.getStatus() == TravelPlanStatus.IN_PROGRESS) {
            List<String> categoryNames = plan.getPreferredCategories().stream()
                    .map(pref -> pref.getTheme().getCategoryName())
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (categoryNames.isEmpty()) categoryNames = List.of("__none__");
            TripRecommendContext ctx = buildTripRecommendContext(plan, categoryNames, excludedIds);
            return new RecommendationResponse(
                    ctx.candidates().stream().map(RecommendationConverter::toItem).toList(), false);
        }

        // DRAFT: 앵커 기반 stateless 추천으로 위임
        List<CoordDto> preferredCoords = travelCourseRepository
                .findAllByTravelPlanIdAndPreferredTrue(planId).stream()
                .filter(c -> hasCoords(c.getPlace()))
                .map(c -> new CoordDto(
                        c.getPlace().getLatitude().doubleValue(),
                        c.getPlace().getLongitude().doubleValue()))
                .toList();

        CoordDto departureCoord = (plan.getDepartureLatitude() != null && plan.getDepartureLongitude() != null)
                ? new CoordDto(plan.getDepartureLatitude().doubleValue(),
                               plan.getDepartureLongitude().doubleValue())
                : null;

        return recommendStateless(new RecommendationRequest(
                departureCoord, preferredCoords, excludedIds, List.of(), null));
    }

    /**
     * Stateless 추천 (POST /api/recommendations).
     * planId 없이 프론트가 좌표를 직접 전달합니다.
     *
     * - preferredWaypoints 없음 → 전역 랜덤
     * - preferredWaypoints 1개 + 출발지 없음 → 단일 앵커 반경 쿼리
     * - preferredWaypoints 여러 개 → 앵커 체인 부채꼴, 결과 없으면 TourAPI 폴백
     */
    public RecommendationResponse recommendStateless(RecommendationRequest request) {
        List<Long> excludedIds = new ArrayList<>(
                request.excludedPlaceIds() != null ? request.excludedPlaceIds() : List.of());
        if (excludedIds.isEmpty()) excludedIds.add(-1L);

        TravelTheme category = request.category();
        String categoryName = (category != null && category.hasDbCategory())
                ? category.getCategoryName() : null;

        if (request.preferredWaypoints() == null || request.preferredWaypoints().isEmpty()) {
            List<Place> results = (categoryName != null)
                    ? placeRepository.findRandomByCategory(categoryName, excludedIds, RECOMMENDATION_LIMIT + 1)
                    : placeRepository.findRandom(excludedIds, RECOMMENDATION_LIMIT + 1);
            boolean hasMore = results.size() > RECOMMENDATION_LIMIT;
            return new RecommendationResponse(
                    results.stream().limit(RECOMMENDATION_LIMIT).map(RecommendationConverter::toItem).toList(),
                    hasMore);
        }

        // 앵커 체인: 출발지 → 선호경유지A → B → C ...
        List<CoordDto> chain = buildChain(request.departureCoord(), request.preferredWaypoints());

        // 단일 앵커: 방향 기준점 없으므로 부채꼴 대신 반경 쿼리
        if (chain.size() == 1) {
            CoordDto anchor = chain.get(0);
            List<Place> results = (categoryName != null)
                    ? placeRepository.findWithinRadiusByCategory(
                            anchor.longitude(), anchor.latitude(), SINGLE_ANCHOR_RADIUS_METERS,
                            categoryName, excludedIds, RECOMMENDATION_LIMIT + 1)
                    : placeRepository.findWithinRadius(
                            anchor.longitude(), anchor.latitude(), SINGLE_ANCHOR_RADIUS_METERS,
                            excludedIds, RECOMMENDATION_LIMIT + 1);
            if (!results.isEmpty()) {
                boolean hasMore = results.size() > RECOMMENDATION_LIMIT;
                return new RecommendationResponse(
                        results.stream().limit(RECOMMENDATION_LIMIT).map(RecommendationConverter::toItem).toList(),
                        hasMore);
            }
            if (category != null && !category.hasTourApiType()) {
                return new RecommendationResponse(List.of(), false);
            }
            return recommendViaFallback(
                    request.preferredWaypoints(),
                    request.excludeContentIds() != null ? request.excludeContentIds() : List.of(),
                    category);
        }

        Set<Long> seen = new LinkedHashSet<>();
        List<Place> dbResults = new ArrayList<>();

        for (int i = 0; i < chain.size() - 1; i++) {
            CoordDto p1 = chain.get(i), p2 = chain.get(i + 1);
            CoordDto m = midpoint(p1, p2);
            double radius = haversineMeters(p1.latitude(), p1.longitude(),
                                           p2.latitude(), p2.longitude()) / 2;
            if (categoryName != null) {
                dbResults.addAll(placeRepository.findInSectorAllByCategory(
                        m.longitude(), m.latitude(), p1.longitude(), p1.latitude(),
                        radius, COS_HALF_ANGLE, categoryName, excludedIds, RECOMMENDATION_LIMIT));
                dbResults.addAll(placeRepository.findInSectorAllByCategory(
                        m.longitude(), m.latitude(), p2.longitude(), p2.latitude(),
                        radius, COS_HALF_ANGLE, categoryName, excludedIds, RECOMMENDATION_LIMIT));
            } else {
                dbResults.addAll(placeRepository.findInSectorAll(
                        m.longitude(), m.latitude(), p1.longitude(), p1.latitude(),
                        radius, COS_HALF_ANGLE, excludedIds, RECOMMENDATION_LIMIT));
                dbResults.addAll(placeRepository.findInSectorAll(
                        m.longitude(), m.latitude(), p2.longitude(), p2.latitude(),
                        radius, COS_HALF_ANGLE, excludedIds, RECOMMENDATION_LIMIT));
            }
        }

        List<Place> allDeduped = dbResults.stream()
                .filter(p -> seen.add(p.getId()))
                .toList();
        boolean hasMore = allDeduped.size() > RECOMMENDATION_LIMIT;
        List<Place> deduped = allDeduped.stream().limit(RECOMMENDATION_LIMIT).toList();

        if (!deduped.isEmpty()) {
            return new RecommendationResponse(
                    deduped.stream().map(RecommendationConverter::toItem).toList(), hasMore);
        }

        // TourAPI 타입 매핑 없는 카테고리(예: CAFE)는 폴백 없이 빈 결과 반환
        if (category != null && !category.hasTourApiType()) {
            return new RecommendationResponse(List.of(), false);
        }

        return recommendViaFallback(
                request.preferredWaypoints(),
                request.excludeContentIds() != null ? request.excludeContentIds() : List.of(),
                category);
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────────

    private TripRecommendContext buildTripRecommendContext(
            TravelPlan plan, List<String> categoryNames, List<Long> excludedIds) {

        Optional<TravelCourse> nextOpt = travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedFalseWithPlaceOrderByVisitDateAscSequenceOrderAsc(plan.getId());

        if (nextOpt.isEmpty()) {
            return new TripRecommendContext(
                    placeRepository.findByCategoriesOrderByPopularity(categoryNames, excludedIds, RECOMMENDATION_LIMIT));
        }

        Place c = nextOpt.get().getPlace();
        Optional<TravelCourse> lastOpt = travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedTrueWithPlaceOrderByVisitDateDescSequenceOrderDesc(plan.getId());

        Place b = lastOpt.map(TravelCourse::getPlace).orElse(plan.getDeparturePlace());

        if (!hasCoords(b) || !hasCoords(c)) {
            return new TripRecommendContext(
                    placeRepository.findByCategoriesOrderByPopularity(categoryNames, excludedIds, RECOMMENDATION_LIMIT));
        }

        double bLon = b.getLongitude().doubleValue();
        double bLat = b.getLatitude().doubleValue();
        double cLon = c.getLongitude().doubleValue();
        double cLat = c.getLatitude().doubleValue();
        double radius = haversineMeters(bLat, bLon, cLat, cLon);

        List<Place> results = placeRepository.findInSector(
                bLon, bLat, cLon, cLat, radius, COS_HALF_ANGLE, categoryNames, excludedIds, RECOMMENDATION_LIMIT);

        if (results.isEmpty()) {
            results = placeRepository.findByCategoriesOrderByPopularity(categoryNames, excludedIds, RECOMMENDATION_LIMIT);
        }
        return new TripRecommendContext(results);
    }

    private RecommendationResponse recommendViaFallback(
            List<CoordDto> preferredWaypoints, List<String> excludeContentIds, TravelTheme category) {

        Set<String> excludeSet = new HashSet<>(excludeContentIds);
        Integer contentTypeId = (category != null) ? category.getContentTypeId() : null;

        List<CompletableFuture<List<LocationBasedItem>>> futures = preferredWaypoints.stream()
                .map(coord -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return korServiceClient.locationBasedList2(
                                coord.latitude(), coord.longitude(),
                                FALLBACK_RADIUS_METERS, FALLBACK_NUM_OF_ROWS, contentTypeId);
                    } catch (TourApiException e) {
                        log.warn("locationBasedList2 호출 실패: lat={}, lng={}", coord.latitude(), coord.longitude(), e);
                        return List.<LocationBasedItem>of();
                    }
                }, FALLBACK_EXECUTOR))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(FALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
        } catch (CompletionException e) {
            log.warn("TourAPI 폴백 타임아웃 ({}초 초과)", FALLBACK_TIMEOUT_SECONDS, e.getCause());
        }

        Map<String, LocationBasedItem> deduplicated = new LinkedHashMap<>();
        futures.stream()
                .filter(f -> f.isDone() && !f.isCompletedExceptionally())
                .map(f -> f.getNow(List.of()))
                .flatMap(List::stream)
                .forEach(item -> deduplicated.putIfAbsent(item.contentid(), item));

        List<LocationBasedItem> candidates = deduplicated.values().stream()
                .filter(item -> !excludeSet.contains(item.contentid()))
                .filter(item -> isValidCoord(item.mapx()) && isValidCoord(item.mapy()))
                .toList();

        boolean hasMore = candidates.size() > FALLBACK_RESULT_SIZE;

        List<PlaceRecommendationItem> items = candidates.stream()
                .limit(FALLBACK_RESULT_SIZE)
                .map(item -> new PlaceRecommendationItem(
                        null,
                        item.contentid(),
                        item.title(),
                        null,
                        item.firstimage(),
                        item.addr1(),
                        parseBigDecimal(item.mapy()),
                        parseBigDecimal(item.mapx())))
                .toList();

        return new RecommendationResponse(items, hasMore);
    }

    private List<CoordDto> buildChain(CoordDto departureCoord, List<CoordDto> preferredWaypoints) {
        List<CoordDto> chain = new ArrayList<>();
        if (departureCoord != null) chain.add(departureCoord);
        chain.addAll(preferredWaypoints);
        return chain;
    }

    private CoordDto midpoint(CoordDto p1, CoordDto p2) {
        return new CoordDto(
                (p1.latitude() + p2.latitude()) / 2,
                (p1.longitude() + p2.longitude()) / 2);
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private boolean hasCoords(Place place) {
        return place != null && place.getLatitude() != null && place.getLongitude() != null;
    }

    private boolean isValidCoord(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void verifyOwner(TravelPlan plan, Long userId) {
        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        }
    }
}
