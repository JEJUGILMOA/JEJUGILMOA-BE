package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.locationusage.service.LocationUsageLogService;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.dto.TripCompleteResponse;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TripServiceCompleteTest {

    private static final Long USER_ID = 11L;
    private static final Long TRIP_ID = 21L;

    @Mock TravelPlanRepository travelPlanRepository;
    @Mock TravelCourseRepository travelCourseRepository;
    @Mock PlaceRepository placeRepository;
    @Mock WaypointService waypointService;
    @Mock LocationUsageLogService locationUsageLogService;
    @InjectMocks TripService tripService;

    @Test
    void complete_success_transitionsToCompletedAndReturnsStats() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = TravelPlan.builder()
                .id(TRIP_ID).user(user).title("제주 3박4일")
                .startDate(LocalDate.of(2026, 7, 20)).endDate(LocalDate.of(2026, 7, 23))
                .status(TravelPlanStatus.IN_PROGRESS).build();

        // 애월(위도 33.4625, 경도 126.3223) -> 성산(위도 33.4587, 경도 126.9426) 약 57.8km
        Place placeA = Place.builder().id(1L)
                .latitude(new BigDecimal("33.4625")).longitude(new BigDecimal("126.3223")).build();
        Place placeB = Place.builder().id(2L)
                .latitude(new BigDecimal("33.4587")).longitude(new BigDecimal("126.9426")).build();
        TravelCourse courseA = TravelCourse.builder().id(101L).travelPlan(plan).place(placeA).visited(true).build();
        TravelCourse courseB = TravelCourse.builder().id(102L).travelPlan(plan).place(placeB).visited(true).build();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(List.of(courseA, courseB));

        TripCompleteResponse response = tripService.complete(TRIP_ID, USER_ID);

        assertThat(plan.getStatus()).isEqualTo(TravelPlanStatus.COMPLETED);
        assertThat(response.status()).isEqualTo(TravelPlanStatus.COMPLETED);
        assertThat(response.durationDays()).isEqualTo(4);
        assertThat(response.placeCount()).isEqualTo(2);
        assertThat(response.totalDistanceKm()).isCloseTo(57.8, within(1.0));
        assertThat(response.actualCompletedAt()).isNotNull();
    }

    @Test
    void complete_failure_notInProgress() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan draft = TravelPlan.builder()
                .id(TRIP_ID).user(user).status(TravelPlanStatus.DRAFT).build();
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> tripService.complete(TRIP_ID, USER_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.TRIP_NOT_IN_PROGRESS));
    }

    @Test
    void complete_failure_unvisitedWaypointExists() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = TravelPlan.builder()
                .id(TRIP_ID).user(user).status(TravelPlanStatus.IN_PROGRESS).build();
        Place place = Place.builder().id(1L).build();
        TravelCourse visited = TravelCourse.builder().id(101L).travelPlan(plan).place(place).visited(true).build();
        TravelCourse unvisited = TravelCourse.builder().id(102L).travelPlan(plan).place(place).visited(false).build();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(List.of(visited, unvisited));

        assertThatThrownBy(() -> tripService.complete(TRIP_ID, USER_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.TRIP_NOT_COMPLETABLE));
        assertThat(plan.getStatus()).isEqualTo(TravelPlanStatus.IN_PROGRESS);
    }

    @Test
    void complete_failure_accessDenied() {
        User owner = User.builder().id(999L).nickname("주인").build();
        TravelPlan plan = TravelPlan.builder()
                .id(TRIP_ID).user(owner).status(TravelPlanStatus.IN_PROGRESS).build();
        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));

        assertThatThrownBy(() -> tripService.complete(TRIP_ID, USER_ID))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.PLAN_ACCESS_DENIED));
    }
}
