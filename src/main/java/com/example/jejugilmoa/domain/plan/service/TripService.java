package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.badge.service.BadgeService;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.locationusage.service.LocationUsageLogService;
import com.example.jejugilmoa.domain.plan.converter.TravelPlanRouteConverter;
import com.example.jejugilmoa.domain.plan.converter.TripConverter;
import com.example.jejugilmoa.domain.plan.dto.TripCancelResponse;
import com.example.jejugilmoa.domain.plan.dto.TripCompleteResponse;
import com.example.jejugilmoa.domain.plan.dto.TripResponse;
import com.example.jejugilmoa.domain.plan.dto.TripStartRequest;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanRoutesResponse;
import com.example.jejugilmoa.domain.plan.dto.VisitCheckRequest;
import com.example.jejugilmoa.domain.plan.dto.VisitCheckResponse;
import com.example.jejugilmoa.domain.plan.dto.WaypointAddRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
import com.example.jejugilmoa.domain.plan.dto.WaypointReorderRequest;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.RouteGenerationStatus;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRouteJobRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRouteRepository;
import com.example.jejugilmoa.domain.user.entity.UserBadge;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    // 연속 방문 간 이동 속도 상한 — 직선 거리 기준 120 km/h 초과 시 물리적으로 불가능한 이동으로 판정
    private static final double MAX_TRAVEL_SPEED_KMH = 120.0;

    private final TravelPlanRepository travelPlanRepository;
    private final TravelCourseRepository travelCourseRepository;
    private final PlaceRepository placeRepository;
    private final WaypointService waypointService;
    private final LocationUsageLogService locationUsageLogService;
    private final BadgeService badgeService;
    private final TravelPlanRouteRepository travelPlanRouteRepository;
    private final TravelPlanRouteJobRepository travelPlanRouteJobRepository;

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

        // 유저별 IN_PROGRESS 단일성은 부분 유니크 인덱스(idx_travel_plan_user_in_progress)가 보장한다.
        // findByIdForUpdate는 이 plan 행만 잠그므로, 서로 다른 DRAFT 계획에 대한 동시 시작 요청은
        // 여기서 걸러야 여러 IN_PROGRESS 행이 만들어지는 것을 막을 수 있다.
        try {
            travelPlanRepository.saveAndFlush(plan);
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(PlanErrorCode.TRIP_ALREADY_IN_PROGRESS);
        }

        return TripConverter.toResponse(plan, waypointService.listWaypoints(plan.getId()),
                buildRoutesResponse(plan.getId()));
    }

    /**
     * 스케줄러가 날짜 기반으로 자동 시작할 때 호출합니다.
     * 각 계획을 독립 트랜잭션으로 처리해, 한 계획의 충돌이 다른 계획에 영향을 주지 않습니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void autoStart(Long planId) {
        TravelPlan plan = travelPlanRepository.findByIdForUpdate(planId)
                .orElse(null);
        if (plan == null || plan.getStatus() != TravelPlanStatus.DRAFT) {
            return;
        }

        plan.start(plan.getStartDate().atStartOfDay());
        try {
            travelPlanRepository.saveAndFlush(plan);
        } catch (DataIntegrityViolationException e) {
            // 동시 자동 시작 등으로 이미 IN_PROGRESS 계획이 생긴 경우 건너뜀
        }
    }

    /**
     * 로그인한 유저의 진행중(IN_PROGRESS)인 여행을 조회합니다.
     * 유저당 진행중 여행은 최대 1건이라는 규칙을 전제로 tripId 없이 조회합니다.
     */
    public TripResponse getCurrentTrip(Long userId) {
        TravelPlan plan = travelPlanRepository.findByUserIdAndStatus(userId, TravelPlanStatus.IN_PROGRESS)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.CURRENT_TRIP_NOT_FOUND));

        return TripConverter.toResponse(plan, waypointService.listWaypoints(plan.getId()),
                buildRoutesResponse(plan.getId()));
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
     * <p>방문 인증에 성공하면 {@link BadgeService#grantEarnedBadges}로 이번 방문으로 새로
     * 조건을 충족한 뱃지를 즉시 지급합니다. 마지막 경유지 방문 시 여행이 자동으로 완료됩니다.</p>
     *
     * @return 방문 인증 결과 — 갱신된 경유지 목록과 자동 완료 여부
     */
    @Transactional
    public VisitCheckResponse checkVisit(Long tripId, Long userId, VisitCheckRequest request) {
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

        LocalDateTime now = LocalDateTime.now();
        checkTravelSpeed(tripId, target, now);
        target.checkVisit(now);
        badgeService.grantEarnedBadges(userId);

        List<WaypointResponse> waypoints = waypointService.listWaypoints(tripId);

        List<TravelCourse> allCourses = travelCourseRepository
                .findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(tripId);
        if (!allCourses.isEmpty() && allCourses.stream().allMatch(TravelCourse::isVisited)) {
            LocalDateTime actualStartedAt = plan.getActualStartedAt();
            plan.complete(now);
            badgeService.grantEarnedBadges(userId);
            List<UserBadge> earnedBadges = badgeService.getBadgesEarnedSince(userId, actualStartedAt);
            return TripConverter.toVisitCheckResponse(waypoints, true, earnedBadges);
        }

        return TripConverter.toVisitCheckResponse(waypoints, false, null);
    }

    /**
     * 진행중인 여행의 경유지를 GPS 방문 인증 없이 건너뜁니다.
     *
     * <p>{@link #checkVisit}과 동일하게 {@code sequenceOrder} 순서상 다음 차례인 경유지만
     * 건너뛸 수 있습니다({@code WAYPOINT_OUT_OF_ORDER}). 위치 인증({@code
     * VISIT_RADIUS_METERS} 반경 검사)과 위치 사용 로그 기록은 거치지 않고 곧바로
     * {@code visited}를 true로 처리해, 다음 경유지를 인증할 수 있도록 순번을 넘깁니다.
     * 다만 {@code skipped} 플래그를 함께 남겨, 실제 GPS 방문 인증과 구분해 뱃지 집계
     * ({@link com.example.jejugilmoa.domain.badge.service.BadgeService})에서는 제외됩니다.
     * 마지막 경유지 건너뜀 시 여행이 자동으로 완료됩니다.</p>
     *
     * @return 건너뛴 후 결과 — 갱신된 경유지 목록과 자동 완료 여부
     */
    @Transactional
    public VisitCheckResponse skipWaypoint(Long tripId, Long userId, Long waypointId) {
        TravelPlan plan = findPlanAndVerifyOwner(tripId, userId);

        if (plan.getStatus() != TravelPlanStatus.IN_PROGRESS) {
            throw new GeneralException(PlanErrorCode.TRIP_NOT_IN_PROGRESS);
        }

        TravelCourse target = travelCourseRepository.findById(waypointId)
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

        LocalDateTime now = LocalDateTime.now();
        target.skip(now);

        List<WaypointResponse> waypoints = waypointService.listWaypoints(tripId);

        List<TravelCourse> allCourses = travelCourseRepository
                .findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(tripId);
        if (!allCourses.isEmpty() && allCourses.stream().allMatch(TravelCourse::isVisited)) {
            LocalDateTime actualStartedAt = plan.getActualStartedAt();
            plan.complete(now);
            badgeService.grantEarnedBadges(userId);
            List<UserBadge> earnedBadges = badgeService.getBadgesEarnedSince(userId, actualStartedAt);
            return TripConverter.toVisitCheckResponse(waypoints, true, earnedBadges);
        }

        return TripConverter.toVisitCheckResponse(waypoints, false, null);
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

    @Transactional
    public List<WaypointResponse> reorderWaypoints(Long tripId, Long userId,
            WaypointReorderRequest request) {
        TravelPlan plan = findPlanAndVerifyOwner(tripId, userId);
        if (plan.getStatus() != TravelPlanStatus.IN_PROGRESS)
            throw new GeneralException(PlanErrorCode.TRIP_NOT_IN_PROGRESS);
        return waypointService.reorderWaypoints(tripId, userId, request);
    }

    /**
     * 진행중인 여행을 완료 처리합니다.
     *
     * <p>모든 경유지가 방문 인증되어 있어야 하며, 하나라도 미방문이면
     * {@code TRIP_NOT_COMPLETABLE} 예외가 발생합니다.</p>
     *
     * <p>응답의 {@code earnedBadges}는 이번 여행 시작({@code actualStartedAt}) 이후 획득한
     * 뱃지 전체 목록입니다 — 여행 중 방문 인증마다 즉시 지급되므로, 완료 시점에는 그동안
     * 쌓인 뱃지를 모아 보여주는 역할만 합니다.</p>
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

        LocalDateTime actualStartedAt = plan.getActualStartedAt();
        plan.complete(LocalDateTime.now());
        badgeService.grantEarnedBadges(userId);
        List<UserBadge> earnedBadges = badgeService.getBadgesEarnedSince(userId, actualStartedAt);

        return TripConverter.toCompleteResponse(
                plan, courses.size(), calculateTotalDistanceKm(courses), earnedBadges);
    }

    /**
     * 진행중인 여행을 중단합니다.
     *
     * <p>진행중(IN_PROGRESS) 상태인 여행만 중단할 수 있으며, 그 외 상태이면
     * {@code TRIP_NOT_CANCELLABLE} 예외가 발생합니다. 중단 후 상태는 {@code CANCELLED}로
     * 전환되며, 이후 새로운 여행을 시작할 수 있습니다.</p>
     */
    @Transactional
    public TripCancelResponse cancel(Long tripId, Long userId) {
        TravelPlan plan = findPlanAndVerifyOwner(tripId, userId);

        if (plan.getStatus() != TravelPlanStatus.IN_PROGRESS) {
            throw new GeneralException(PlanErrorCode.TRIP_NOT_CANCELLABLE);
        }

        plan.cancel(LocalDateTime.now());
        return TripConverter.toCancelResponse(plan);
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

    // 이전 GPS 인증 방문 경유지와의 이동 속도를 검사합니다.
    // 직선 거리 기준으로도 MAX_TRAVEL_SPEED_KMH를 초과하면 물리적으로 불가능한 이동으로 판정합니다.
    private void checkTravelSpeed(Long tripId, TravelCourse target, LocalDateTime now) {
        travelCourseRepository.findLastGpsVerifiedWithPlace(tripId).ifPresent(previous -> {
            if (previous.getId().equals(target.getId())) return;
            double distanceKm = haversineKm(
                    previous.getPlace().getLatitude().doubleValue(),
                    previous.getPlace().getLongitude().doubleValue(),
                    target.getPlace().getLatitude().doubleValue(),
                    target.getPlace().getLongitude().doubleValue());
            long elapsedSeconds = java.time.Duration.between(previous.getVisitedAt(), now).toSeconds();
            if (elapsedSeconds > 0 && distanceKm / (elapsedSeconds / 3600.0) > MAX_TRAVEL_SPEED_KMH) {
                throw new GeneralException(PlanErrorCode.WAYPOINT_VISIT_TOO_FAST);
            }
        });
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

    private TravelPlanRoutesResponse buildRoutesResponse(Long planId) {
        var savedRoutes = travelPlanRouteRepository.findAllByTravelPlanIdOrderByRouteDateAsc(planId);
        RouteGenerationStatus generation = travelPlanRouteJobRepository.findStateByPlanId(planId)
                .map(job -> switch (job.status()) {
                    case PENDING -> RouteGenerationStatus.PENDING;
                    case RUNNING -> job.leaseValid() ? RouteGenerationStatus.RUNNING : RouteGenerationStatus.PENDING;
                    case DONE -> RouteGenerationStatus.DONE;
                }).orElse(RouteGenerationStatus.NOT_REQUESTED);
        return new TravelPlanRoutesResponse(planId, new TravelPlanRoutesResponse.Generation(generation),
                savedRoutes.stream().map(TravelPlanRouteConverter::toResponse).toList());
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
