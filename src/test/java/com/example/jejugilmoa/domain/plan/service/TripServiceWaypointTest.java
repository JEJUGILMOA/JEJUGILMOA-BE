package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.locationusage.service.LocationUsageLogService;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.dto.WaypointAddRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TripServiceWaypointTest {

    private static final Long USER_ID = 11L;
    private static final Long TRIP_ID = 21L;
    private static final Long WAYPOINT_ID = 101L;

    @Mock TravelPlanRepository travelPlanRepository;
    @Mock TravelCourseRepository travelCourseRepository;
    @Mock PlaceRepository placeRepository;
    @Mock WaypointService waypointService;
    @Mock LocationUsageLogService locationUsageLogService;
    @InjectMocks TripService tripService;

    private TravelPlan plan(TravelPlanStatus status, User owner) {
        return TravelPlan.builder().id(TRIP_ID).user(owner).status(status).build();
    }

    @Test
    void addWaypoint_success_delegatesToWaypointService() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = plan(TravelPlanStatus.IN_PROGRESS, user);
        WaypointAddRequest request = new WaypointAddRequest(42L, LocalDate.of(2026, 8, 15), null);
        List<WaypointResponse> expected = List.of();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(waypointService.addWaypoint(TRIP_ID, USER_ID, request)).willReturn(expected);

        List<WaypointResponse> result = tripService.addWaypoint(TRIP_ID, USER_ID, request);

        assertThat(result).isSameAs(expected);
        then(waypointService).should().addWaypoint(TRIP_ID, USER_ID, request);
    }

    @Test
    void addWaypoint_failure_notInProgress() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan draft = plan(TravelPlanStatus.DRAFT, user);
        WaypointAddRequest request = new WaypointAddRequest(42L, LocalDate.of(2026, 8, 15), null);
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> tripService.addWaypoint(TRIP_ID, USER_ID, request))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.TRIP_NOT_IN_PROGRESS));
        then(waypointService).should(never()).addWaypoint(TRIP_ID, USER_ID, request);
    }

    @Test
    void addWaypoint_failure_accessDenied() {
        User owner = User.builder().id(999L).nickname("주인").build();
        TravelPlan plan = plan(TravelPlanStatus.IN_PROGRESS, owner);
        WaypointAddRequest request = new WaypointAddRequest(42L, LocalDate.of(2026, 8, 15), null);
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));

        assertThatThrownBy(() -> tripService.addWaypoint(TRIP_ID, USER_ID, request))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.PLAN_ACCESS_DENIED));
    }

    @Test
    void removeWaypoint_success_delegatesToWaypointService() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = plan(TravelPlanStatus.IN_PROGRESS, user);
        Place place = Place.builder().id(1L).build();
        TravelCourse unvisited = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(place).visited(false).build();
        List<WaypointResponse> expected = List.of();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(unvisited));
        given(waypointService.removeWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID)).willReturn(expected);

        List<WaypointResponse> result = tripService.removeWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID);

        assertThat(result).isSameAs(expected);
        then(waypointService).should().removeWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID);
    }

    @Test
    void removeWaypoint_failure_notInProgress() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan draft = plan(TravelPlanStatus.DRAFT, user);
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> tripService.removeWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.TRIP_NOT_IN_PROGRESS));
        then(waypointService).should(never()).removeWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID);
    }

    @Test
    void removeWaypoint_failure_alreadyVisited() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = plan(TravelPlanStatus.IN_PROGRESS, user);
        Place place = Place.builder().id(1L).build();
        TravelCourse visited = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(place).visited(true).build();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(visited));

        assertThatThrownBy(() -> tripService.removeWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.VISITED_WAYPOINT_NOT_REMOVABLE));
        then(waypointService).should(never()).removeWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID);
    }

    @Test
    void removeWaypoint_failure_startPoint() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = plan(TravelPlanStatus.IN_PROGRESS, user);
        Place place = Place.builder().id(1L).build();
        TravelCourse startCourse = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(place).start(true).visited(false).build();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(startCourse));

        assertThatThrownBy(() -> tripService.removeWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.START_OR_DESTINATION_NOT_REMOVABLE));
    }

    @Test
    void removeWaypoint_failure_destination() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = plan(TravelPlanStatus.IN_PROGRESS, user);
        Place place = Place.builder().id(1L).build();
        TravelCourse destCourse = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(place).destination(true).visited(false).build();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(destCourse));

        assertThatThrownBy(() -> tripService.removeWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.START_OR_DESTINATION_NOT_REMOVABLE));
    }

    @Test
    void removeWaypoint_failure_waypointNotBelongingToTrip() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = plan(TravelPlanStatus.IN_PROGRESS, user);
        TravelPlan otherPlan = TravelPlan.builder().id(999L).user(user).build();
        Place place = Place.builder().id(1L).build();
        TravelCourse foreign = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(otherPlan).place(place).visited(false).build();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(foreign));

        assertThatThrownBy(() -> tripService.removeWaypoint(TRIP_ID, USER_ID, WAYPOINT_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.WAYPOINT_NOT_FOUND));
    }
}
