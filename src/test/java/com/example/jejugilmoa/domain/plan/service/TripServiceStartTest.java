package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.locationusage.service.LocationUsageLogService;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.dto.TripStartRequest;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TripServiceStartTest {

    private static final Long USER_ID = 11L;
    private static final Long PLAN_ID = 21L;
    private static final TripStartRequest REQUEST = new TripStartRequest(PLAN_ID);

    @Mock TravelPlanRepository travelPlanRepository;
    @Mock TravelCourseRepository travelCourseRepository;
    @Mock PlaceRepository placeRepository;
    @Mock WaypointService waypointService;
    @Mock LocationUsageLogService locationUsageLogService;
    @InjectMocks TripService tripService;

    @Test
    void start_success_flushesSoDuplicateInProgressIsCaughtImmediately() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = TravelPlan.builder()
                .id(PLAN_ID).user(user).status(TravelPlanStatus.DRAFT).build();
        given(travelPlanRepository.findByIdForUpdate(PLAN_ID)).willReturn(Optional.of(plan));
        given(travelPlanRepository.saveAndFlush(plan)).willReturn(plan);
        given(waypointService.listWaypoints(PLAN_ID)).willReturn(List.of());

        tripService.start(USER_ID, REQUEST);

        assertThat(plan.getStatus()).isEqualTo(TravelPlanStatus.IN_PROGRESS);
    }

    @Test
    void start_concurrentStartOnAnotherPlan_violatesPartialUniqueIndex() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = TravelPlan.builder()
                .id(PLAN_ID).user(user).status(TravelPlanStatus.DRAFT).build();
        given(travelPlanRepository.findByIdForUpdate(PLAN_ID)).willReturn(Optional.of(plan));
        given(travelPlanRepository.saveAndFlush(plan))
                .willThrow(new DataIntegrityViolationException("idx_travel_plan_user_in_progress"));

        assertThatThrownBy(() -> tripService.start(USER_ID, REQUEST))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.TRIP_ALREADY_IN_PROGRESS));
    }
}
