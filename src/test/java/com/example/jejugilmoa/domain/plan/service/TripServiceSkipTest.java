package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.badge.service.BadgeService;
import com.example.jejugilmoa.domain.locationusage.service.LocationUsageLogService;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TripServiceSkipTest {

    private static final Long USER_ID = 11L;
    private static final Long TRIP_ID = 21L;
    private static final Long WAYPOINT_ID = 31L;

    @Mock TravelPlanRepository travelPlanRepository;
    @Mock TravelCourseRepository travelCourseRepository;
    @Mock PlaceRepository placeRepository;
    @Mock WaypointService waypointService;
    @Mock LocationUsageLogService locationUsageLogService;
    @Mock BadgeService badgeService;
    @InjectMocks TripService tripService;

    @Test
    void skipWaypoint_success_marksVisitedWithoutGpsOrLocationLog() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = TravelPlan.builder()
                .id(TRIP_ID).user(user).status(TravelPlanStatus.IN_PROGRESS).build();
        Place place = Place.builder().id(41L).build();
        TravelCourse target = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(place).visited(false).build();
        List<WaypointResponse> expected = List.of();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(target));
        given(travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedFalseOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(Optional.of(target));
        given(waypointService.listWaypoints(TRIP_ID)).willReturn(expected);

        List<WaypointResponse> result = tripService.skipWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID);

        assertThat(result).isSameAs(expected);
        assertThat(target.isVisited()).isTrue();
        assertThat(target.getVisitedAt()).isNotNull();
        assertThat(target.isSkipped()).isTrue();
        assertThat(target.getSkippedAt()).isNotNull();
        verifyNoInteractions(locationUsageLogService);
        verifyNoInteractions(badgeService);
        verify(placeRepository, never()).existsWithinDistance(
                anyLong(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void skipWaypoint_failure_notInProgress() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan draft = TravelPlan.builder()
                .id(TRIP_ID).user(user).status(TravelPlanStatus.DRAFT).build();
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> tripService.skipWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.TRIP_NOT_IN_PROGRESS));
    }

    @Test
    void skipWaypoint_failure_accessDenied() {
        User owner = User.builder().id(999L).nickname("주인").build();
        TravelPlan plan = TravelPlan.builder()
                .id(TRIP_ID).user(owner).status(TravelPlanStatus.IN_PROGRESS).build();
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));

        assertThatThrownBy(() -> tripService.skipWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.PLAN_ACCESS_DENIED));
    }

    @Test
    void skipWaypoint_failure_waypointNotBelongingToTrip() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = TravelPlan.builder()
                .id(TRIP_ID).user(user).status(TravelPlanStatus.IN_PROGRESS).build();
        TravelPlan otherPlan = TravelPlan.builder().id(999L).user(user).build();
        Place place = Place.builder().id(1L).build();
        TravelCourse foreign = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(otherPlan).place(place).visited(false).build();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(foreign));

        assertThatThrownBy(() -> tripService.skipWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.WAYPOINT_NOT_FOUND));
    }

    @Test
    void skipWaypoint_failure_alreadyVisited() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = TravelPlan.builder()
                .id(TRIP_ID).user(user).status(TravelPlanStatus.IN_PROGRESS).build();
        Place place = Place.builder().id(1L).build();
        TravelCourse visited = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(place).visited(true).build();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(visited));

        assertThatThrownBy(() -> tripService.skipWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.WAYPOINT_ALREADY_VISITED));
    }

    @Test
    void skipWaypoint_failure_outOfOrder() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = TravelPlan.builder()
                .id(TRIP_ID).user(user).status(TravelPlanStatus.IN_PROGRESS).build();
        Place place = Place.builder().id(1L).build();
        TravelCourse target = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(place).visited(false).sequenceOrder(2).build();
        TravelCourse earlier = TravelCourse.builder()
                .id(30L).travelPlan(plan).place(place).visited(false).sequenceOrder(1).build();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(target));
        given(travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedFalseOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(Optional.of(earlier));

        assertThatThrownBy(() -> tripService.skipWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.WAYPOINT_OUT_OF_ORDER));
        assertThat(target.isVisited()).isFalse();
    }
}
