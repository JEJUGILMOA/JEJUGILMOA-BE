package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.locationusage.service.LocationUsageLogService;
import com.example.jejugilmoa.domain.plan.converter.TripConverter;
import com.example.jejugilmoa.domain.plan.dto.TripCompleteResponse;
import com.example.jejugilmoa.domain.plan.dto.TripResponse;
import com.example.jejugilmoa.domain.plan.dto.TripStartRequest;
import com.example.jejugilmoa.domain.plan.dto.VisitCheckRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointAddRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {

    // 방문 인증 허용 반경 — 일반적인 GPS 오차(약 10~50m)를 감안한 값
    private static final double VISIT_RADIUS_METERS = 100.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final TravelPlanRepository travelPlanRepository;
    private final TravelCourseRepository travelCourseRepository;
    private final PlaceRepository placeRepository;
    private final WaypointService waypointService;
    private final LocationUsageLogService locationUsageLogService;

    /**
     * 여행 계획(DRAFT)을 시작해 진행중(IN_PROGRESS) 상태로 전환합니다.
     * tripId는 별도 엔티티 없이 여행 계획 ID를 그대로 사용합니다.
     */
    @Transactional
    public TripResponse start(Long userId, TripStartRequest request) {
        TravelPlan plan = findPlanAndVerifyOwner(request.planId(), userId);

        if (plan.getStatus() != TravelPlanStatus.DRAFT) {
            throw new GeneralException(PlanErrorCode.TRIP_NOT_STARTABLE);
        }

        plan.start(LocalDateTime.now());

        return TripConverter.toResponse(plan, waypointService.listWaypoints(plan.getId()));
    }

    /**
     * 로그인한 유저의 진행중(IN_PROGRESS)인 여행을 조회합니다.
     * 유저당 진행중 여행은 최대 1건이라는 규칙을 전제로 tripId 없이 조회합니다.
     */
    public TripResponse getCurrentTrip(Long userId) {
        TravelPlan plan = travelPlanRepository.findByUserIdAndStatus(userId, TravelPlanStatus.IN_PROGRESS)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.CURRENT_TRIP_NOT_FOUND));

        return TripConverter.toResponse(plan, waypointService.listWaypoints(plan.getId()));
    }

    /**
     * 진행중인 여행의 경유지를 방문 인증합니다.
     *
     * <p>경유지는 {@code sequenceOrder} 순서대로만 인증할 수 있습니다 — 아직 미방문인
     * 경유지 중 순번이 가장 빠른 것이 아니면 {@code WAYPOINT_OUT_OF_ORDER} 예외가 발생합니다.</p>
     *
     * <p>요청에 담긴 현재 위치(latitude/longitude)가 경유지 장소 반경
     * {@value #VISIT_RADIUS_METERS}m 이내가 아니면 {@code WAYPOINT_LOCATION_MISMATCH}
     * 예외가 발생해 실제 방문하지 않은 인증을 막습니다.</p>
     *
     * @return 방문 인증 후 갱신된 전체 경유지 목록 (순서 오름차순)
     */
    @Transactional
    public List<WaypointResponse> checkVisit(Long tripId, Long userId, VisitCheckRequest request) {
        locationUsageLogService.recordVisitVerification(userId);

        TravelPlan plan = findPlanAndVerifyOwner(tripId, userId);

        if (plan.getStatus() != TravelPlanStatus.IN_PROGRESS) {
            throw new GeneralException(PlanErrorCode.TRIP_NOT_IN_PROGRESS);
        }

        TravelCourse target = travelCourseRepository.findById(request.waypointId())
                .filter(c -> c.getTravelPlan().getId().equals(tripId))
                .orElseThrow(() -> new GeneralException(PlanErrorCode.WAYPOINT_NOT_FOUND));

        if (target.isVisited()) {
            throw new GeneralException(PlanErrorCode.WAYPOINT_ALREADY_VISITED);
        }

        TravelCourse nextUnvisited = travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedFalseOrderByVisitDateAscSequenceOrderAsc(tripId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.WAYPOINT_NOT_FOUND));
        if (!nextUnvisited.getId().equals(target.getId())) {
            throw new GeneralException(PlanErrorCode.WAYPOINT_OUT_OF_ORDER);
        }

        boolean withinRange = placeRepository.existsWithinDistance(
                target.getPlace().getId(), request.longitude(), request.latitude(), VISIT_RADIUS_METERS);
        if (!withinRange) {
            throw new GeneralException(PlanErrorCode.WAYPOINT_LOCATION_MISMATCH);
        }

        target.checkVisit(LocalDateTime.now());

        return waypointService.listWaypoints(tripId);
    }

    /**
     * 진행중인 여행에 경유지를 추가합니다.
     *
     * <p>여행 계획 편집({@code /api/plans})과 달리, 여행이 진행중(IN_PROGRESS)일 때만
     * 허용됩니다 — 그 외 상태이면 {@code TRIP_NOT_IN_PROGRESS} 예외가 발생합니다.
     * 날짜 범위·중복 검증 및 순번 계산은 {@link WaypointService#addWaypoint}에 위임합니다.</p>
     *
     * @return 추가 후 갱신된 전체 경유지 목록 (순서 오름차순)
     */
    @Transactional
    public List<WaypointResponse> addWaypoint(Long tripId, Long userId, WaypointAddRequest request) {
        TravelPlan plan = findPlanAndVerifyOwner(tripId, userId);

        if (plan.getStatus() != TravelPlanStatus.IN_PROGRESS) {
            throw new GeneralException(PlanErrorCode.TRIP_NOT_IN_PROGRESS);
        }

        return waypointService.addWaypoint(tripId, userId, request);
    }

    /**
     * 진행중인 여행에서 경유지를 삭제합니다.
     *
     * <p>여행이 진행중(IN_PROGRESS)일 때만 허용되며, 이미 방문 인증된 경유지는
     * 삭제할 수 없습니다({@code VISITED_WAYPOINT_NOT_REMOVABLE}). 삭제 후 같은 날짜의
     * 순번 당김은 {@link WaypointService#removeWaypoint}에 위임합니다.</p>
     *
     * @return 삭제 후 갱신된 전체 경유지 목록 (순서 오름차순)
     */
    @Transactional
    public List<WaypointResponse> removeWaypoint(Long tripId, Long userId, Long waypointId) {
        TravelPlan plan = findPlanAndVerifyOwner(tripId, userId);

        if (plan.getStatus() != TravelPlanStatus.IN_PROGRESS) {
            throw new GeneralException(PlanErrorCode.TRIP_NOT_IN_PROGRESS);
        }

        TravelCourse target = travelCourseRepository.findById(waypointId)
                .filter(c -> c.getTravelPlan().getId().equals(tripId))
                .orElseThrow(() -> new GeneralException(PlanErrorCode.WAYPOINT_NOT_FOUND));
        if (target.isStart() || target.isDestination()) {
            throw new GeneralException(PlanErrorCode.START_OR_DESTINATION_NOT_REMOVABLE);
        }
        if (target.isVisited()) {
            throw new GeneralException(PlanErrorCode.VISITED_WAYPOINT_NOT_REMOVABLE);
        }

        return waypointService.removeWaypoint(tripId, userId, waypointId);
    }

    /**
     * 진행중인 여행을 완료 처리합니다.
     *
     * <p>모든 경유지가 방문 인증되어 있어야 하며, 하나라도 미방문이면
     * {@code TRIP_NOT_COMPLETABLE} 예외가 발생합니다.</p>
     */
    @Transactional
    public TripCompleteResponse complete(Long tripId, Long userId) {
        TravelPlan plan = findPlanAndVerifyOwner(tripId, userId);

        if (plan.getStatus() != TravelPlanStatus.IN_PROGRESS) {
            throw new GeneralException(PlanErrorCode.TRIP_NOT_IN_PROGRESS);
        }

        List<TravelCourse> courses = travelCourseRepository
                .findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(tripId);
        if (courses.isEmpty() || courses.stream().anyMatch(course -> !course.isVisited())) {
            throw new GeneralException(PlanErrorCode.TRIP_NOT_COMPLETABLE);
        }

        plan.complete(LocalDateTime.now());

        return TripConverter.toCompleteResponse(plan, courses.size(), calculateTotalDistanceKm(courses));
    }

    // 방문 순서(visitDate, sequenceOrder)를 그대로 따라 인접 장소 간 직선거리를 합산 (Haversine)
    private double calculateTotalDistanceKm(List<TravelCourse> orderedCourses) {
        double totalKm = 0.0;
        for (int i = 1; i < orderedCourses.size(); i++) {
            Place from = orderedCourses.get(i - 1).getPlace();
            Place to = orderedCourses.get(i).getPlace();
            totalKm += haversineKm(
                    from.getLatitude().doubleValue(), from.getLongitude().doubleValue(),
                    to.getLatitude().doubleValue(), to.getLongitude().doubleValue());
        }
        return totalKm;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    // SELECT FOR UPDATE: 같은 plan에 대한 시작/방문인증 요청을 직렬화
    private TravelPlan findPlanAndVerifyOwner(Long planId, Long userId) {
        TravelPlan plan = travelPlanRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        }
        return plan;
    }
}
