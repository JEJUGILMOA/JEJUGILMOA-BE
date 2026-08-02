package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.locationusage.service.LocationUsageLogService;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.dto.VisitCheckRequest;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TripServiceLocationUsageTest {

    private static final Long USER_ID = 11L;
    private static final Long TRIP_ID = 21L;
    private static final Long WAYPOINT_ID = 31L;
    private static final VisitCheckRequest REQUEST =
            new VisitCheckRequest(WAYPOINT_ID, 33.39397, 126.24066);

    @Mock TravelPlanRepository travelPlanRepository;
    @Mock TravelCourseRepository travelCourseRepository;
    @Mock PlaceRepository placeRepository;
    @Mock WaypointService waypointService;
    @Mock LocationUsageLogService locationUsageLogService;
    @InjectMocks TripService tripService;

    @Test
    void checkVisit_success_recordsBeforeBusinessValidation() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = TravelPlan.builder()
                .id(TRIP_ID).user(user).status(TravelPlanStatus.IN_PROGRESS).build();
        Place place = Place.builder().id(41L).build();
        TravelCourse target = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(place).visited(false).build();
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(target));
        given(travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedFalseOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(Optional.of(target));
        given(placeRepository.existsWithinDistance(
                place.getId(), REQUEST.longitude(), REQUEST.latitude(), 100.0)).willReturn(true);
        given(waypointService.listWaypoints(TRIP_ID)).willReturn(List.of());

        tripService.checkVisit(TRIP_ID, USER_ID, REQUEST);

        InOrder order = inOrder(locationUsageLogService, travelPlanRepository);
        order.verify(locationUsageLogService).recordVisitVerification(USER_ID);
        order.verify(travelPlanRepository).findByIdForUpdate(TRIP_ID);
        verify(placeRepository).existsWithinDistance(
                place.getId(), REQUEST.longitude(), REQUEST.latitude(), 100.0);
    }

    @Test
    void checkVisit_businessFailure_stillRecordsEachRequest() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan draft = TravelPlan.builder()
                .id(TRIP_ID).user(user).status(TravelPlanStatus.DRAFT).build();
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> tripService.checkVisit(TRIP_ID, USER_ID, REQUEST))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(
                        ((GeneralException) ex).getCode()).isEqualTo(PlanErrorCode.TRIP_NOT_IN_PROGRESS));
        assertThatThrownBy(() -> tripService.checkVisit(TRIP_ID, USER_ID, REQUEST))
                .isInstanceOf(GeneralException.class);

        verify(locationUsageLogService, times(2)).recordVisitVerification(USER_ID);
    }

    @Test
    void checkVisit_recordFailure_stopsBeforeBusinessLogic() {
        given(locationUsageLogService.recordVisitVerification(USER_ID))
                .willThrow(new IllegalStateException("location usage storage unavailable"));

        assertThatThrownBy(() -> tripService.checkVisit(TRIP_ID, USER_ID, REQUEST))
                .isInstanceOf(IllegalStateException.class);

        verify(travelPlanRepository, never()).findByIdForUpdate(TRIP_ID);
        verify(placeRepository, never()).existsWithinDistance(
                org.mockito.ArgumentMatchers.anyLong(), anyDouble(), anyDouble(), anyDouble());
    }
}
