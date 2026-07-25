package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.converter.RecommendationConverter;
import com.example.jejugilmoa.domain.plan.dto.PlaceRecommendationItem;
import com.example.jejugilmoa.domain.plan.dto.RecommendationResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

    private final TravelPlanRepository travelPlanRepository;
    private final TravelCourseRepository travelCourseRepository;
    private final PlaceRepository placeRepository;

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
                .findAllByTravelPlanIdOrderBySequenceOrderAsc(planId)
                .stream()
                .map(c -> c.getPlace().getId())
                .toList();

        // 최종 제외 목록 = 이미 추가된 + 이번 요청에서 건너뛰는 장소
        // IN 절 빈 배열 오류 방지를 위해 비어 있으면 -1L 삽입 (DB에 id=-1인 행은 없음)
        List<Long> excludedIds = new ArrayList<>(addedPlaceIds);
        excludedIds.addAll(additionalExcludedIds);
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

    private void verifyOwner(TravelPlan plan, Long userId) {
        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        }
    }
}
