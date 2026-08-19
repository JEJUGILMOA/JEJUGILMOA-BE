package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.converter.WaypointConverter;
import com.example.jejugilmoa.domain.plan.dto.WaypointAddRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointReorderRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WaypointService {

    private final TravelPlanRepository travelPlanRepository;
    private final TravelCourseRepository travelCourseRepository;
    private final PlaceRepository placeRepository;

    @Transactional
    public List<WaypointResponse> addWaypoint(Long planId, Long userId, WaypointAddRequest request) {
        TravelPlan plan = findPlanAndVerifyOwner(planId, userId);

        LocalDate visitDate = request.visitDate();
        if (visitDate.isBefore(plan.getStartDate()) || visitDate.isAfter(plan.getEndDate())) {
            throw new GeneralException(PlanErrorCode.INVALID_VISIT_DATE);
        }

        if (travelCourseRepository.existsByTravelPlanIdAndPlaceId(planId, request.placeId())) {
            throw new GeneralException(PlanErrorCode.PLACE_ALREADY_ADDED);
        }

        Place place = placeRepository.findByIdAndPublishedTrue(request.placeId())
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));

        // 같은 Day 내 마지막 순번 + 1
        int nextOrder = travelCourseRepository.countByTravelPlanIdAndVisitDate(planId, visitDate) + 1;

        TravelCourse course = TravelCourse.builder()
                .travelPlan(plan)
                .place(place)
                .visitDate(visitDate)
                .sequenceOrder(nextOrder)
                .preferred(Boolean.TRUE.equals(request.isPreferred()))
                .build();
        try {
            travelCourseRepository.saveAndFlush(course);
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(PlanErrorCode.PLACE_ALREADY_ADDED);
        }

        refreshStartDestinationFlags(planId, visitDate);
        return listWaypoints(planId);
    }

    @Transactional
    public List<WaypointResponse> removeWaypoint(Long planId, Long userId, Long waypointId) {
        findPlanAndVerifyOwner(planId, userId);

        TravelCourse target = travelCourseRepository.findById(waypointId)
                .filter(c -> c.getTravelPlan().getId().equals(planId))
                .orElseThrow(() -> new GeneralException(PlanErrorCode.WAYPOINT_NOT_FOUND));

        int removedOrder = target.getSequenceOrder();
        LocalDate visitDate = target.getVisitDate();
        travelCourseRepository.delete(target);
        // 같은 Day에서 제거된 순번 이후만 당김
        travelCourseRepository.decrementSequenceOrderAfter(planId, visitDate, removedOrder);

        refreshStartDestinationFlags(planId, visitDate);
        return listWaypoints(planId);
    }

    @Transactional
    public List<WaypointResponse> reorderWaypoints(Long planId, Long userId, WaypointReorderRequest request) {
        TravelPlan plan = findPlanAndVerifyOwner(planId, userId);
        if (plan.getStatus() != TravelPlanStatus.DRAFT) {
            throw new GeneralException(PlanErrorCode.PLAN_NOT_EDITABLE);
        }

        LocalDate visitDate = request.visitDate();
        List<Long> newOrder = request.waypointIds();

        List<TravelCourse> existing =
                travelCourseRepository.findAllByTravelPlanIdAndVisitDateOrderBySequenceOrderAsc(planId, visitDate);

        Set<Long> existingIds = existing.stream().map(TravelCourse::getId).collect(Collectors.toSet());
        if (new HashSet<>(newOrder).size() != newOrder.size()
                || !new HashSet<>(newOrder).equals(existingIds)) {
            throw new GeneralException(PlanErrorCode.INVALID_WAYPOINT_ORDER);
        }

        // Phase-1: 해당 Day의 순번을 임시 offset으로 이동 (uk_course_plan_date_sequence 충돌 방지)
        travelCourseRepository.shiftSequenceOrderByOffset(planId, visitDate, newOrder.size() + 1);

        // Phase-2: 요청 순서대로 최종 순번 확정
        for (int i = 0; i < newOrder.size(); i++) {
            travelCourseRepository.updateSequenceOrder(newOrder.get(i), planId, i + 1);
        }

        refreshStartDestinationFlags(planId, visitDate);
        return listWaypoints(planId);
    }

    @Transactional
    public List<WaypointResponse> togglePreferred(Long planId, Long userId, Long waypointId, boolean preferred) {
        TravelPlan plan = findPlanAndVerifyOwner(planId, userId);
        if (plan.getStatus() != TravelPlanStatus.DRAFT) {
            throw new GeneralException(PlanErrorCode.PLAN_NOT_EDITABLE);
        }
        TravelCourse course = travelCourseRepository.findById(waypointId)
                .filter(c -> c.getTravelPlan().getId().equals(planId))
                .orElseThrow(() -> new GeneralException(PlanErrorCode.WAYPOINT_NOT_FOUND));
        course.updatePreferred(preferred);
        return listWaypoints(planId);
    }

    public List<WaypointResponse> listWaypoints(Long planId) {
        return travelCourseRepository
                .findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(planId)
                .stream()
                .map(WaypointConverter::toResponse)
                .toList();
    }

    // SELECT FOR UPDATE: 같은 plan에 대한 경유지 추가/삭제를 직렬화해 순번 충돌 방지
    private TravelPlan findPlanAndVerifyOwner(Long planId, Long userId) {
        TravelPlan plan = travelPlanRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        }
        return plan;
    }

    // 경유지 추가/삭제/재정렬 후 해당 날짜의 첫 번째(is_start)·마지막(is_destination) 플래그를 재계산
    private void refreshStartDestinationFlags(Long planId, LocalDate visitDate) {
        List<TravelCourse> courses = travelCourseRepository
                .findAllByTravelPlanIdAndVisitDateOrderBySequenceOrderAsc(planId, visitDate);
        if (courses.isEmpty()) return;
        for (int i = 0; i < courses.size(); i++) {
            courses.get(i).updateStartDestination(i == 0, i == courses.size() - 1);
        }
    }
}
