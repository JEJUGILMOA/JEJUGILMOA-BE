package com.example.jejugilmoa.domain.plan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.plan.dto.ShareLinkResponse;
import com.example.jejugilmoa.domain.plan.dto.SharedPlanResponse;
import com.example.jejugilmoa.domain.plan.entity.DayDeparture;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.entity.TravelSharedPlan;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.DayDepartureRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelSharedPlanRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanShareServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T03:00:00Z");
    private static final Long PLAN_ID = 10L;
    private static final Long OWNER_ID = 20L;

    @Mock TravelPlanRepository travelPlanRepository;
    @Mock TravelSharedPlanRepository sharedPlanRepository;
    @Mock TravelCourseRepository travelCourseRepository;
    @Mock DayDepartureRepository dayDepartureRepository;

    PlanShareService service;
    TravelPlan plan;

    @BeforeEach
    void setUp() {
        service = new PlanShareService(travelPlanRepository, sharedPlanRepository, travelCourseRepository,
                dayDepartureRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        User owner = mock(User.class);
        lenient().when(owner.getId()).thenReturn(OWNER_ID);
        plan = mock(TravelPlan.class);
        lenient().when(plan.getId()).thenReturn(PLAN_ID);
        lenient().when(plan.getUser()).thenReturn(owner);
    }

    @Test
    void ownerCreatesLinkExpiringExactlyThirtyDaysAfterIssue() {
        given(travelPlanRepository.findByIdForUpdate(PLAN_ID)).willReturn(Optional.of(plan));
        given(sharedPlanRepository.findByTravelPlanId(PLAN_ID)).willReturn(Optional.empty());
        given(sharedPlanRepository.existsByShareToken(any())).willReturn(false);
        given(sharedPlanRepository.saveAndFlush(any())).willAnswer(invocation -> invocation.getArgument(0));

        ShareLinkResponse response = service.createShareLink(PLAN_ID, OWNER_ID);

        assertThat(response.planId()).isEqualTo(PLAN_ID);
        assertThat(response.shareToken()).isNotBlank();
        assertThat(response.expiresAt()).isEqualTo(NOW.plusSeconds(30L * 24 * 60 * 60));
    }

    @Test
    void validExistingLinkIsReturnedWithoutExtendingExpiry() {
        Instant originalExpiry = NOW.plusSeconds(60);
        TravelSharedPlan existing = sharedPlan("existing-token", originalExpiry, true);
        given(travelPlanRepository.findByIdForUpdate(PLAN_ID)).willReturn(Optional.of(plan));
        given(sharedPlanRepository.findByTravelPlanId(PLAN_ID)).willReturn(Optional.of(existing));

        ShareLinkResponse first = service.createShareLink(PLAN_ID, OWNER_ID);
        ShareLinkResponse second = service.createShareLink(PLAN_ID, OWNER_ID);

        assertThat(first.shareToken()).isEqualTo("existing-token");
        assertThat(second.shareToken()).isEqualTo(first.shareToken());
        assertThat(second.expiresAt()).isEqualTo(originalExpiry);
        verify(sharedPlanRepository, never()).saveAndFlush(any());
    }

    @Test
    void expiredLinkIsReissuedAndOnlyOneRowIsSaved() {
        TravelSharedPlan existing = sharedPlan("expired-token", NOW, true);
        given(travelPlanRepository.findByIdForUpdate(PLAN_ID)).willReturn(Optional.of(plan));
        given(sharedPlanRepository.findByTravelPlanId(PLAN_ID)).willReturn(Optional.of(existing));
        given(sharedPlanRepository.existsByShareToken(any())).willReturn(false);
        given(sharedPlanRepository.saveAndFlush(existing)).willReturn(existing);

        ShareLinkResponse response = service.createShareLink(PLAN_ID, OWNER_ID);

        assertThat(response.shareToken()).isNotEqualTo("expired-token");
        assertThat(response.expiresAt()).isEqualTo(NOW.plusSeconds(30L * 24 * 60 * 60));
        ArgumentCaptor<TravelSharedPlan> captor = ArgumentCaptor.forClass(TravelSharedPlan.class);
        verify(sharedPlanRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    @Test
    void inactiveLinkIsReissuedAndActivated() {
        TravelSharedPlan existing = sharedPlan("inactive-token", NOW.plusSeconds(60), false);
        given(travelPlanRepository.findByIdForUpdate(PLAN_ID)).willReturn(Optional.of(plan));
        given(sharedPlanRepository.findByTravelPlanId(PLAN_ID)).willReturn(Optional.of(existing));
        given(sharedPlanRepository.existsByShareToken(any())).willReturn(false);
        given(sharedPlanRepository.saveAndFlush(existing)).willReturn(existing);

        ShareLinkResponse response = service.createShareLink(PLAN_ID, OWNER_ID);

        assertThat(response.shareToken()).isNotEqualTo("inactive-token");
        assertThat(existing.isActive()).isTrue();
    }

    @Test
    void nonOwnerCannotCreateLink() {
        given(travelPlanRepository.findByIdForUpdate(PLAN_ID)).willReturn(Optional.of(plan));

        assertCode(() -> service.createShareLink(PLAN_ID, 999L), PlanErrorCode.PLAN_ACCESS_DENIED);
    }

    @Test
    void missingPlanCannotCreateLink() {
        given(travelPlanRepository.findByIdForUpdate(PLAN_ID)).willReturn(Optional.empty());

        assertCode(() -> service.createShareLink(PLAN_ID, OWNER_ID), PlanErrorCode.PLAN_NOT_FOUND);
    }

    @Test
    void validTokenReturnsCurrentPlanWithSortedPublicWaypoints() throws Exception {
        TravelSharedPlan shared = sharedPlan("valid-token", NOW.plusSeconds(60), true);
        given(sharedPlanRepository.findByShareToken("valid-token")).willReturn(Optional.of(shared));
        given(travelPlanRepository.findById(PLAN_ID)).willReturn(Optional.of(plan));
        given(plan.getTitle()).willReturn("현재 제목");
        given(plan.getStartDate()).willReturn(LocalDate.of(2026, 8, 10));
        given(plan.getEndDate()).willReturn(LocalDate.of(2026, 8, 11));
        given(plan.getBudgetFood()).willReturn(30_000);

        TravelCourse dayOne = course(LocalDate.of(2026, 8, 10), 2, "두 번째");
        TravelCourse dayTwo = course(LocalDate.of(2026, 8, 11), 1, "다음 날");
        given(travelCourseRepository.findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(PLAN_ID))
                .willReturn(List.of(dayOne, dayTwo));

        DayDeparture startDep = departure(LocalDate.of(2026, 8, 10), "제주국제공항");
        given(dayDepartureRepository.findAllByTravelPlanIdOrderByVisitDateAsc(PLAN_ID))
                .willReturn(List.of(startDep));

        SharedPlanResponse response = service.getSharedPlan("valid-token");

        assertThat(response.title()).isEqualTo("현재 제목");
        assertThat(response.departure()).isEqualTo("제주국제공항");
        assertThat(response.waypoints()).extracting(SharedPlanResponse.SharedWaypointResponse::placeName)
                .containsExactly("두 번째", "다음 날");
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response);
        assertThat(json).doesNotContain("userId", "email", "oauth", "visitedAt", "latitudeAtVisit");
    }

    @Test
    void missingExpiredAndInactiveTokensFailWithDistinctCodes() {
        given(sharedPlanRepository.findByShareToken("missing")).willReturn(Optional.empty());
        given(sharedPlanRepository.findByShareToken("expired"))
                .willReturn(Optional.of(sharedPlan("expired", NOW, true)));
        given(sharedPlanRepository.findByShareToken("inactive"))
                .willReturn(Optional.of(sharedPlan("inactive", NOW.plusSeconds(60), false)));

        assertCode(() -> service.getSharedPlan("missing"), PlanErrorCode.SHARE_TOKEN_NOT_FOUND);
        assertCode(() -> service.getSharedPlan("expired"), PlanErrorCode.SHARE_LINK_EXPIRED);
        assertCode(() -> service.getSharedPlan("inactive"), PlanErrorCode.SHARE_LINK_INACTIVE);
    }

    private DayDeparture departure(LocalDate date, String locationName) {
        return DayDeparture.builder().travelPlan(plan).visitDate(date).locationName(locationName)
                .latitude(new BigDecimal("33.51111111")).longitude(new BigDecimal("126.49222222")).build();
    }

    private TravelSharedPlan sharedPlan(String token, Instant expiresAt, boolean active) {
        return TravelSharedPlan.builder().travelPlan(plan).shareToken(token).expiresAt(expiresAt).active(active).build();
    }

    private TravelCourse course(LocalDate date, int order, String name) {
        Place place = mock(Place.class);
        given(place.getName()).willReturn(name);
        given(place.getAddress()).willReturn("주소");
        given(place.getLatitude()).willReturn(new BigDecimal("33.12345678"));
        given(place.getLongitude()).willReturn(new BigDecimal("126.12345678"));
        TravelCourse course = mock(TravelCourse.class);
        given(course.getVisitDate()).willReturn(date);
        given(course.getSequenceOrder()).willReturn(order);
        given(course.getPlace()).willReturn(place);
        return course;
    }

    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, PlanErrorCode code) {
        assertThatThrownBy(callable)
                .isInstanceOf(GeneralException.class)
                .satisfies(error -> assertThat(((GeneralException) error).getCode()).isEqualTo(code));
    }
}
