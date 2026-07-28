package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.converter.WaypointConverter;
import com.example.jejugilmoa.domain.plan.dto.WaypointAddRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointMemoRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointReorderRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 추천 경유지를 여행 코스에 추가합니다.
     *
     * <p>sequenceOrder는 현재 경유지 수 + 1 로 자동 배정됩니다.
     * 같은 장소를 중복으로 추가하면 {@code PLACE_ALREADY_ADDED} 예외가 발생합니다.</p>
     *
     * @return 추가 후 갱신된 전체 경유지 목록 (순서 오름차순)
     */
    @Transactional
    public List<WaypointResponse> addWaypoint(Long planId, Long userId, WaypointAddRequest request) {
        TravelPlan plan = findPlanAndVerifyOwner(planId, userId);

        if (travelCourseRepository.existsByTravelPlanIdAndPlaceId(planId, request.placeId())) {
            throw new GeneralException(PlanErrorCode.PLACE_ALREADY_ADDED);
        }

        Place place = placeRepository.findByIdAndPublishedTrue(request.placeId())
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));

        // 다음 순서 = 현재 경유지 수 + 1 (시퀀스 gap 없이 연속 배정)
        int nextOrder = travelCourseRepository.countByTravelPlanId(planId) + 1;

        TravelCourse course = TravelCourse.builder()
                .travelPlan(plan)
                .place(place)
                .sequenceOrder(nextOrder)
                .build();
        try {
            travelCourseRepository.saveAndFlush(course);
        } catch (DataIntegrityViolationException e) {
            // uk_course_plan_place 위반: 동시 요청으로 pre-check을 통과한 중복 추가
            throw new GeneralException(PlanErrorCode.PLACE_ALREADY_ADDED);
        }

        return listWaypoints(planId);
    }

    /**
     * 경유지를 여행 코스에서 제거합니다.
     *
     * <p>제거한 경유지 이후의 sequenceOrder를 1씩 감소시켜 연속성을 유지합니다.
     * waypointId가 해당 planId에 속하지 않으면 {@code WAYPOINT_NOT_FOUND} 예외가 발생합니다.</p>
     *
     * @return 제거 후 갱신된 전체 경유지 목록 (순서 오름차순)
     */
    @Transactional
    public List<WaypointResponse> removeWaypoint(Long planId, Long userId, Long waypointId) {
        findPlanAndVerifyOwner(planId, userId);

        // planId 소속 여부를 함께 확인해 타 계획의 경유지를 삭제하지 못하도록 방지
        TravelCourse target = travelCourseRepository.findById(waypointId)
                .filter(c -> c.getTravelPlan().getId().equals(planId))
                .orElseThrow(() -> new GeneralException(PlanErrorCode.WAYPOINT_NOT_FOUND));

        int removedOrder = target.getSequenceOrder();
        travelCourseRepository.delete(target);
        // flushAutomatically=true가 DELETE를 먼저 반영하므로 별도 flush 불필요
        travelCourseRepository.decrementSequenceOrderAfter(planId, removedOrder);

        return listWaypoints(planId);
    }

    /**
     * 경유지 순서를 변경합니다.
     *
     * 요청의 waypointIds는 해당 planId에 속한 모든 경유지 ID를 빠짐없이 포함해야 합니다.
     * Phase-1에서 임시 offset으로 순번을 밀어 uk_course_plan_sequence 충돌을 방지한 뒤,
     * Phase-2에서 최종 순번을 개별 UPDATE로 확정합니다.
     *
     * @return 재정렬 후 전체 경유지 목록 (순서 오름차순)
     */
    @Transactional
    public List<WaypointResponse> reorderWaypoints(Long planId, Long userId, WaypointReorderRequest request) {
        findPlanAndVerifyOwner(planId, userId);

        List<Long> newOrder = request.waypointIds();
        List<TravelCourse> existing = travelCourseRepository.findAllByTravelPlanIdOrderBySequenceOrderAsc(planId);

        Set<Long> planIds = existing.stream().map(TravelCourse::getId).collect(Collectors.toSet());
        if (new HashSet<>(newOrder).size() != newOrder.size()
                || !new HashSet<>(newOrder).equals(planIds)) {
            throw new GeneralException(PlanErrorCode.INVALID_WAYPOINT_ORDER);
        }

        // Phase-1: 현재 최대 순번보다 큰 임시 값으로 전체 이동 (unique 제약 임시 회피)
        travelCourseRepository.shiftSequenceOrderByOffset(planId, newOrder.size() + 1);

        // Phase-2: 요청 순서대로 최종 순번 확정
        for (int i = 0; i < newOrder.size(); i++) {
            travelCourseRepository.updateSequenceOrder(newOrder.get(i), planId, i + 1);
        }

        return listWaypoints(planId);
    }

    /**
     * 경유지에 메모를 작성/수정합니다.
     *
     * memo가 null이면 기존 메모를 삭제합니다.
     *
     * @return 갱신 후 전체 경유지 목록 (순서 오름차순)
     */
    @Transactional
    public List<WaypointResponse> updateMemo(Long planId, Long userId, Long waypointId, WaypointMemoRequest request) {
        findPlanAndVerifyOwner(planId, userId);

        TravelCourse target = travelCourseRepository.findById(waypointId)
                .filter(c -> c.getTravelPlan().getId().equals(planId))
                .orElseThrow(() -> new GeneralException(PlanErrorCode.WAYPOINT_NOT_FOUND));

        target.updateMemo(request.memo());

        return listWaypoints(planId);
    }

    /**
     * 특정 계획의 전체 경유지 목록을 조회합니다.
     */
    public List<WaypointResponse> listWaypoints(Long planId) {
        return travelCourseRepository
                .findAllByTravelPlanIdOrderBySequenceOrderAsc(planId)
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
}
