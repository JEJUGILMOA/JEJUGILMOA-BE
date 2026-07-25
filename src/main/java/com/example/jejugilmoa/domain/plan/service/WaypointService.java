package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.converter.WaypointConverter;
import com.example.jejugilmoa.domain.plan.dto.WaypointAddRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        travelCourseRepository.save(course);

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
        travelCourseRepository.flush(); // 삭제 반영 후 재조회를 위해 플러시

        // 제거된 순서 이후의 경유지들을 1씩 앞당겨 순서 공백을 메움
        travelCourseRepository.findAllByTravelPlanIdOrderBySequenceOrderAsc(planId).stream()
                .filter(c -> c.getSequenceOrder() > removedOrder)
                .forEach(TravelCourse::decrementOrder);

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

    private TravelPlan findPlanAndVerifyOwner(Long planId, Long userId) {
        TravelPlan plan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        }
        return plan;
    }
}
