package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.badge.entity.Badge;
import com.example.jejugilmoa.domain.badge.enums.BadgeGroup;
import com.example.jejugilmoa.domain.badge.enums.BadgeType;
import com.example.jejugilmoa.domain.badge.service.BadgeService;
import com.example.jejugilmoa.domain.locationusage.service.LocationUsageLogService;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.dto.VisitCheckRequest;
import com.example.jejugilmoa.domain.plan.dto.VisitCheckResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.entity.UserBadge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TripServiceVisitBadgeTest {

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
    @Mock BadgeService badgeService;
    @InjectMocks TripService tripService;

    @Test
    void checkVisit_success_returnsNewlyEarnedBadgesEvenWhenTripContinues() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = TravelPlan.builder()
                .id(TRIP_ID).user(user).status(TravelPlanStatus.IN_PROGRESS).build();
        Place place = Place.builder().id(41L).build();
        TravelCourse target = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(place).visited(false).build();
        // 미방문 경유지가 남아 있어 자동 완료되지 않는 상황
        TravelCourse remaining = TravelCourse.builder()
                .id(32L).travelPlan(plan).place(place).visited(false).build();
        Badge badge = Badge.builder().id(5L).name("애월 단골")
                .badgeType(BadgeType.PLACE).displayGroup(BadgeGroup.EXPLORATION).build();
        UserBadge earned = UserBadge.builder().id(1L).user(user).badge(badge).build();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(target));
        given(travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedFalseOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(Optional.of(target));
        given(placeRepository.existsWithinDistance(
                place.getId(), REQUEST.longitude(), REQUEST.latitude(), 100.0)).willReturn(true);
        given(badgeService.grantEarnedBadges(USER_ID)).willReturn(List.of(earned));
        given(waypointService.listWaypoints(TRIP_ID)).willReturn(List.of());
        given(travelCourseRepository.findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(List.of(target, remaining));

        VisitCheckResponse response = tripService.checkVisit(TRIP_ID, USER_ID, REQUEST);

        assertThat(response.autoCompleted()).isFalse();
        assertThat(response.earnedBadges()).hasSize(1);
        assertThat(response.earnedBadges().get(0).badgeId()).isEqualTo(5L);
        assertThat(response.earnedBadges().get(0).name()).isEqualTo("애월 단골");
        assertThat(plan.getStatus()).isEqualTo(TravelPlanStatus.IN_PROGRESS);
        verify(badgeService).grantEarnedBadges(USER_ID);
    }

    @Test
    void checkVisit_success_returnsEmptyBadgeListWhenNothingEarned() {
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = TravelPlan.builder()
                .id(TRIP_ID).user(user).status(TravelPlanStatus.IN_PROGRESS).build();
        Place place = Place.builder().id(41L).build();
        TravelCourse target = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(place).visited(false).build();
        TravelCourse remaining = TravelCourse.builder()
                .id(32L).travelPlan(plan).place(place).visited(false).build();

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(target));
        given(travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedFalseOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(Optional.of(target));
        given(placeRepository.existsWithinDistance(
                place.getId(), REQUEST.longitude(), REQUEST.latitude(), 100.0)).willReturn(true);
        given(badgeService.grantEarnedBadges(USER_ID)).willReturn(List.of());
        given(waypointService.listWaypoints(TRIP_ID)).willReturn(List.of());
        given(travelCourseRepository.findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(List.of(target, remaining));

        VisitCheckResponse response = tripService.checkVisit(TRIP_ID, USER_ID, REQUEST);

        assertThat(response.autoCompleted()).isFalse();
        assertThat(response.earnedBadges()).isEmpty();
    }
}
