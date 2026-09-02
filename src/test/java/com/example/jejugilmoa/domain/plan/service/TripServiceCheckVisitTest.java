package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.badge.service.BadgeService;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TripServiceCheckVisitTest {

    private static final Long USER_ID = 11L;
    private static final Long TRIP_ID = 21L;
    private static final Long WAYPOINT_ID = 32L;
    private static final Long PREV_WAYPOINT_ID = 31L;

    // 애월(서쪽) — 이전 방문 장소
    private static final BigDecimal PREV_LAT = new BigDecimal("33.4625");
    private static final BigDecimal PREV_LON = new BigDecimal("126.3223");

    // 성산(동쪽) — 현재 방문 목표 장소 (직선 약 57.8 km)
    private static final BigDecimal TARGET_LAT = new BigDecimal("33.4587");
    private static final BigDecimal TARGET_LON = new BigDecimal("126.9426");

    @Mock TravelPlanRepository travelPlanRepository;
    @Mock TravelCourseRepository travelCourseRepository;
    @Mock PlaceRepository placeRepository;
    @Mock WaypointService waypointService;
    @Mock LocationUsageLogService locationUsageLogService;
    @Mock BadgeService badgeService;
    @InjectMocks TripService tripService;

    private TravelPlan inProgressPlan(User user) {
        return TravelPlan.builder().id(TRIP_ID).user(user).status(TravelPlanStatus.IN_PROGRESS).build();
    }

    @Test
    void checkVisit_failure_travelSpeedExceedsLimit() {
        // 이전 GPS 인증 방문: 30초 전, 애월 위치 (직선 ~57.8 km 떨어진 성산을 30초 만에 이동 → ~6936 km/h)
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = inProgressPlan(user);

        Place prevPlace = Place.builder().id(41L).latitude(PREV_LAT).longitude(PREV_LON).build();
        TravelCourse previous = TravelCourse.builder()
                .id(PREV_WAYPOINT_ID).travelPlan(plan).place(prevPlace)
                .visited(true).skipped(false)
                .visitedAt(LocalDateTime.now().minusSeconds(30))
                .build();

        Place targetPlace = Place.builder().id(42L).latitude(TARGET_LAT).longitude(TARGET_LON).build();
        TravelCourse target = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(targetPlace).visited(false).build();

        VisitCheckRequest request = new VisitCheckRequest(WAYPOINT_ID, TARGET_LON.doubleValue(), TARGET_LAT.doubleValue());

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(target));
        given(travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedFalseOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(Optional.of(target));
        given(placeRepository.existsWithinDistance(targetPlace.getId(), request.longitude(), request.latitude(), 100.0))
                .willReturn(true);
        given(travelCourseRepository.findLastGpsVerifiedWithPlace(TRIP_ID)).willReturn(Optional.of(previous));

        assertThatThrownBy(() -> tripService.checkVisit(TRIP_ID, USER_ID, request))
                .isInstanceOf(GeneralException.class)
                .satisfies(ex -> assertThat(((GeneralException) ex).getCode())
                        .isEqualTo(PlanErrorCode.WAYPOINT_VISIT_TOO_FAST));
    }

    @Test
    void checkVisit_success_travelSpeedWithinLimit() {
        // 이전 GPS 인증 방문: 3시간 전, 애월 위치 (직선 ~57.8 km / 3h ≈ 19.3 km/h → 허용)
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = inProgressPlan(user);

        Place prevPlace = Place.builder().id(41L).latitude(PREV_LAT).longitude(PREV_LON).build();
        TravelCourse previous = TravelCourse.builder()
                .id(PREV_WAYPOINT_ID).travelPlan(plan).place(prevPlace)
                .visited(true).skipped(false)
                .visitedAt(LocalDateTime.now().minusHours(3))
                .build();

        Place targetPlace = Place.builder().id(42L).latitude(TARGET_LAT).longitude(TARGET_LON).build();
        TravelCourse target = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(targetPlace).visited(false).build();

        VisitCheckRequest request = new VisitCheckRequest(WAYPOINT_ID, TARGET_LON.doubleValue(), TARGET_LAT.doubleValue());

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(target));
        given(travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedFalseOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(Optional.of(target));
        given(placeRepository.existsWithinDistance(targetPlace.getId(), request.longitude(), request.latitude(), 100.0))
                .willReturn(true);
        given(travelCourseRepository.findLastGpsVerifiedWithPlace(TRIP_ID)).willReturn(Optional.of(previous));
        given(waypointService.listWaypoints(TRIP_ID)).willReturn(List.of());

        tripService.checkVisit(TRIP_ID, USER_ID, request);
    }

    @Test
    void checkVisit_success_firstVisitHasNoPreviousToCompareAgainst() {
        // GPS 인증 방문 이력이 없는 첫 번째 방문 — 속도 검사 자체를 건너뜀
        User user = User.builder().id(USER_ID).nickname("테스트").build();
        TravelPlan plan = inProgressPlan(user);

        Place targetPlace = Place.builder().id(42L).latitude(TARGET_LAT).longitude(TARGET_LON).build();
        TravelCourse target = TravelCourse.builder()
                .id(WAYPOINT_ID).travelPlan(plan).place(targetPlace).visited(false).build();

        VisitCheckRequest request = new VisitCheckRequest(WAYPOINT_ID, TARGET_LON.doubleValue(), TARGET_LAT.doubleValue());

        given(travelPlanRepository.findByIdForUpdate(TRIP_ID)).willReturn(Optional.of(plan));
        given(travelCourseRepository.findById(WAYPOINT_ID)).willReturn(Optional.of(target));
        given(travelCourseRepository
                .findFirstByTravelPlanIdAndVisitedFalseOrderByVisitDateAscSequenceOrderAsc(TRIP_ID))
                .willReturn(Optional.of(target));
        given(placeRepository.existsWithinDistance(targetPlace.getId(), request.longitude(), request.latitude(), 100.0))
                .willReturn(true);
        given(travelCourseRepository.findLastGpsVerifiedWithPlace(TRIP_ID)).willReturn(Optional.empty());
        given(waypointService.listWaypoints(TRIP_ID)).willReturn(List.of());

        tripService.checkVisit(TRIP_ID, USER_ID, request);
    }
}
