package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.converter.TravelPlanConverter;
import com.example.jejugilmoa.domain.plan.converter.WaypointConverter;
import com.example.jejugilmoa.domain.plan.dto.BudgetCreateRequest;
import com.example.jejugilmoa.domain.plan.dto.DayPlanRequest;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanCreateRequest;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanDetailResponse;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanListResponse;
import com.example.jejugilmoa.domain.plan.dto.WaypointCreateRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.entity.TravelPlanPreference;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanPreferenceRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravelPlanService {

    private static final int MAX_TRIP_DAYS = 30;

    private final TravelPlanRepository travelPlanRepository;
    private final TravelPlanPreferenceRepository travelPlanPreferenceRepository;
    private final TravelCourseRepository travelCourseRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;

    @Transactional(readOnly = true)
    public void assertPlanEditable(Long planId, Long userId) {
        TravelPlan plan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        }
        if (plan.getStatus() != TravelPlanStatus.DRAFT) {
            throw new GeneralException(PlanErrorCode.PLAN_NOT_EDITABLE);
        }
    }

    @Transactional(readOnly = true)
    public TravelPlanDetailResponse getPlanDetail(Long planId, Long userId) {
        TravelPlan plan = travelPlanRepository.findByIdWithPreferences(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        }

        List<WaypointResponse> waypoints = travelCourseRepository
                .findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(planId)
                .stream()
                .map(WaypointConverter::toResponse)
                .toList();

        return TravelPlanConverter.toDetail(plan, waypoints);
    }

    @Transactional(readOnly = true)
    public List<TravelPlanListResponse> getMyPlans(Long userId, TravelPlanStatus status) {
        var plans = (status == null)
                ? travelPlanRepository.findMyPlans(userId)
                : travelPlanRepository.findMyPlansByStatus(userId, status);

        if (plans.isEmpty()) return List.of();

        List<Long> planIds = plans.stream().map(TravelPlan::getId).toList();
        Map<Long, Integer> courseCountMap = travelPlanRepository.countCoursesByPlanIds(planIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));

        return plans.stream()
                .map(plan -> TravelPlanConverter.toSummary(plan, courseCountMap.getOrDefault(plan.getId(), 0)))
                .toList();
    }

    @Transactional
    public TravelPlanDetailResponse create(Long userId, TravelPlanCreateRequest request) {
        if (request.startDate().isBefore(LocalDate.now()))
            throw new GeneralException(PlanErrorCode.INVALID_START_DATE);
        if (request.endDate().isBefore(request.startDate()))
            throw new GeneralException(PlanErrorCode.INVALID_DATE_RANGE);
        if (ChronoUnit.DAYS.between(request.startDate(), request.endDate()) > MAX_TRIP_DAYS)
            throw new GeneralException(PlanErrorCode.TRIP_DURATION_EXCEEDED);

        if (request.departurePlaceId() == null
                && (request.departureLocationName() == null || request.departureLocationName().isBlank()))
            throw new GeneralException(PlanErrorCode.DEPARTURE_REQUIRED);

        Place departurePlace = request.departurePlaceId() != null
                ? placeRepository.findByIdAndPublishedTrue(request.departurePlaceId())
                        .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND))
                : null;

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        TravelPlan plan = TravelPlanConverter.toEntity(user, departurePlace, request);
        travelPlanRepository.save(plan);

        if (request.categories() != null && !request.categories().isEmpty()) {
            if (new HashSet<>(request.categories()).size() != request.categories().size())
                throw new GeneralException(PlanErrorCode.DUPLICATE_THEME);
            List<TravelPlanPreference> preferences = request.categories().stream()
                    .map(theme -> TravelPlanPreference.builder().travelPlan(plan).theme(theme).build())
                    .toList();
            travelPlanPreferenceRepository.saveAll(preferences);
            plan.getPreferredCategories().addAll(preferences);
        }

        // 날짜별 경유지를 하나의 트랜잭션에서 원자적으로 저장
        List<DayPlanRequest> days = request.days() != null ? request.days() : List.of();
        Set<LocalDate> seenDates = new HashSet<>();
        for (DayPlanRequest day : days) {
            if (!seenDates.add(day.visitDate()))
                throw new GeneralException(PlanErrorCode.DUPLICATE_VISIT_DATE);
            if (day.visitDate().isBefore(plan.getStartDate()) || day.visitDate().isAfter(plan.getEndDate()))
                throw new GeneralException(PlanErrorCode.INVALID_VISIT_DATE);

            List<WaypointCreateRequest> waypoints = day.waypoints() != null ? day.waypoints() : List.of();
            Set<Long> seenPlaceIds = new HashSet<>();
            for (int i = 0; i < waypoints.size(); i++) {
                WaypointCreateRequest wReq = waypoints.get(i);
                if (!seenPlaceIds.add(wReq.placeId()))
                    throw new GeneralException(PlanErrorCode.PLACE_ALREADY_ADDED);
                Place place = placeRepository.findByIdAndPublishedTrue(wReq.placeId())
                        .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));
                travelCourseRepository.save(TravelCourse.builder()
                        .travelPlan(plan)
                        .place(place)
                        .visitDate(day.visitDate())
                        .sequenceOrder(i + 1)
                        .preferred(wReq.isPreferred())
                        .start(i == 0)
                        .destination(i == waypoints.size() - 1)
                        .build());
            }
        }

        if (request.budget() != null) {
            BudgetCreateRequest b = request.budget();
            plan.updateBudget(b.budgetTransportation(), b.budgetAccommodation(), b.budgetFood(), b.budgetEtc());
        }

        List<WaypointResponse> waypoints = travelCourseRepository
                .findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(plan.getId())
                .stream()
                .map(WaypointConverter::toResponse)
                .toList();

        return TravelPlanConverter.toDetail(plan, waypoints);
    }

    @Transactional
    public TravelPlanDetailResponse replace(Long planId, Long userId, TravelPlanCreateRequest request) {
        TravelPlan plan = travelPlanRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        }
        if (plan.getStatus() != TravelPlanStatus.DRAFT) {
            throw new GeneralException(PlanErrorCode.PLAN_NOT_EDITABLE);
        }

        if (request.startDate().isBefore(LocalDate.now()))
            throw new GeneralException(PlanErrorCode.INVALID_START_DATE);
        if (request.endDate().isBefore(request.startDate()))
            throw new GeneralException(PlanErrorCode.INVALID_DATE_RANGE);
        if (ChronoUnit.DAYS.between(request.startDate(), request.endDate()) > MAX_TRIP_DAYS)
            throw new GeneralException(PlanErrorCode.TRIP_DURATION_EXCEEDED);

        if (request.departurePlaceId() == null
                && (request.departureLocationName() == null || request.departureLocationName().isBlank()))
            throw new GeneralException(PlanErrorCode.DEPARTURE_REQUIRED);

        Place departurePlace = request.departurePlaceId() != null
                ? placeRepository.findByIdAndPublishedTrue(request.departurePlaceId())
                        .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND))
                : null;

        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate());
        int totalAvailableTime = (int) (days + 1) * 8 * 60;
        plan.updatePlanMeta(
                request.title(),
                request.startDate(),
                request.endDate(),
                departurePlace,
                request.departureLocationName(),
                request.departureLatitude(),
                request.departureLongitude(),
                request.companion(),
                totalAvailableTime,
                request.startDate().equals(request.endDate())
        );

        // 선호 카테고리 교체: DELETE 확정 후 INSERT해야 UK 위반을 피할 수 있다
        plan.getPreferredCategories().clear();
        plan.getTravelCourses().clear();
        travelPlanRepository.flush();

        if (request.categories() != null && !request.categories().isEmpty()) {
            if (new HashSet<>(request.categories()).size() != request.categories().size())
                throw new GeneralException(PlanErrorCode.DUPLICATE_THEME);
            request.categories().stream()
                    .map(theme -> TravelPlanPreference.builder().travelPlan(plan).theme(theme).build())
                    .forEach(plan.getPreferredCategories()::add);
        }

        List<DayPlanRequest> dayList = request.days() != null ? request.days() : List.of();
        Set<LocalDate> seenDates = new HashSet<>();
        for (DayPlanRequest day : dayList) {
            if (!seenDates.add(day.visitDate()))
                throw new GeneralException(PlanErrorCode.DUPLICATE_VISIT_DATE);
            if (day.visitDate().isBefore(plan.getStartDate()) || day.visitDate().isAfter(plan.getEndDate()))
                throw new GeneralException(PlanErrorCode.INVALID_VISIT_DATE);

            List<WaypointCreateRequest> waypoints = day.waypoints() != null ? day.waypoints() : List.of();
            Set<Long> seenPlaceIds = new HashSet<>();
            for (int i = 0; i < waypoints.size(); i++) {
                WaypointCreateRequest wReq = waypoints.get(i);
                if (!seenPlaceIds.add(wReq.placeId()))
                    throw new GeneralException(PlanErrorCode.PLACE_ALREADY_ADDED);
                Place place = placeRepository.findByIdAndPublishedTrue(wReq.placeId())
                        .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));
                travelCourseRepository.save(TravelCourse.builder()
                        .travelPlan(plan)
                        .place(place)
                        .visitDate(day.visitDate())
                        .sequenceOrder(i + 1)
                        .preferred(wReq.isPreferred())
                        .start(i == 0)
                        .destination(i == waypoints.size() - 1)
                        .build());
            }
        }

        BudgetCreateRequest b = request.budget();
        plan.updateBudget(
                b != null ? b.budgetTransportation() : null,
                b != null ? b.budgetAccommodation() : null,
                b != null ? b.budgetFood() : null,
                b != null ? b.budgetEtc() : null
        );

        List<WaypointResponse> waypoints = travelCourseRepository
                .findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(plan.getId())
                .stream()
                .map(WaypointConverter::toResponse)
                .toList();

        return TravelPlanConverter.toDetail(plan, waypoints);
    }

    @Transactional
    public void deletePlan(Long planId, Long userId) {
        TravelPlan plan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        }

        travelPlanRepository.delete(plan);
    }

}
