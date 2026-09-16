package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.direction.dto.DirectionResponse;
import com.example.jejugilmoa.domain.direction.exception.DirectionErrorCode;
import com.example.jejugilmoa.domain.direction.service.DirectionService;
import com.example.jejugilmoa.domain.plan.repository.*;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.example.jejugilmoa.domain.plan.entity.TravelPlanRoute;
import com.example.jejugilmoa.domain.plan.enums.RouteGenerationStatus;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteJobStatus;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus.*;

@ExtendWith(MockitoExtension.class)
class TravelPlanRouteServiceTest {
    @Mock TravelPlanRouteStore store;
    @Mock DirectionService directions;
    @Mock TravelPlanRepository plans;
    @Mock TravelPlanRouteRepository routes;
    @Mock TravelPlanRouteJobRepository routeJobs;
    @InjectMocks TravelPlanRouteService service;

    private PlanRouteInput input(int size) {
        return new PlanRouteInput(LocalDate.now(), IntStream.range(0, size)
                .mapToObj(i -> new PlanRouteInput.Point(126.0 + i * .01, 33.0 + i * .01)).toList());
    }

    @Test void hashNormalizesScaleAndExcludesDateButPreservesOrderAndCoordinates() {
        var a = PlanRouteInput.Point.of(new BigDecimal("126.4900"), new BigDecimal("33.50"));
        var b = PlanRouteInput.Point.of(new BigDecimal("126.49"), new BigDecimal("33.5000"));
        var c = new PlanRouteInput.Point(126.6, 33.6);
        var first = new PlanRouteInput(LocalDate.now(), List.of(a, c));
        assertThat(first.hash()).hasSize(64).isEqualTo(new PlanRouteInput(LocalDate.now().plusDays(1), List.of(b, c)).hash());
        assertThat(first.hash()).isNotEqualTo(new PlanRouteInput(LocalDate.now(), List.of(c, a)).hash());
        assertThat(first.hash()).isNotEqualTo(new PlanRouteInput(LocalDate.now(), List.of(a, b)).hash());
    }

    @Test void lessThanTwoAndOverSevenDoNotCallDirections() {
        assertThat(service.calculate(input(0)).status()).isEqualTo(NOT_REQUIRED);
        assertThat(service.calculate(input(1)).status()).isEqualTo(NOT_REQUIRED);
        assertThat(service.calculate(input(8)).status()).isEqualTo(UNSUPPORTED);
        verifyNoInteractions(directions);
    }

    @Test void sevenPointsUsesFiveWaypointsAndExplicitlyConvertsPath() {
        when(directions.getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString(), eq("traoptimal")))
                .thenReturn(new DirectionResponse("traoptimal", new DirectionResponse.RouteSummary(18342, 2421000, 0, 0, 0),
                        List.of(new DirectionResponse.Coordinate(33.5, 126.4), new DirectionResponse.Coordinate(33.6, 126.5))));
        var input = input(7);
        var result = service.calculate(input);
        assertThat(result.status()).isEqualTo(READY);
        assertThat(result.path()).containsExactly(List.of(126.4, 33.5), List.of(126.5, 33.6));
        assertThat(result.duration()).isEqualTo(2421000);
        assertThat(input.waypoints().split("\\|")).hasSize(5);
        verify(directions).getDriving(33.0, 126.0, 33.06, 126.06, input.waypoints(), "traoptimal");
    }

    @Test void externalAndCacheFailuresBecomeFailed() {
        when(directions.getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), isNull(), anyString()))
                .thenThrow(new GeneralException(DirectionErrorCode.NAVER_API_ERROR))
                .thenThrow(new IllegalStateException("캐시 연결 실패"));
        assertThat(service.calculate(input(2)).failureCode()).isEqualTo(DirectionErrorCode.NAVER_API_ERROR.getCode());
        assertThat(service.calculate(input(2)).status()).isEqualTo(FAILED);
    }

    @Test void missingCoordinatesAreFailedWithoutExternalCall() {
        var input = new PlanRouteInput(LocalDate.now(), List.of(new PlanRouteInput.Point(null, null), input(1).points().getFirst()));
        assertThat(service.calculate(input).failureCode()).isEqualTo("INVALID_COORDINATE");
        verifyNoInteractions(directions);
    }
    @Test void retryBackoffIsBoundedAndNeverImmediate() {
        assertThat(TravelPlanRouteJobService.retrySeconds(1)).isEqualTo(30);
        assertThat(TravelPlanRouteJobService.retrySeconds(2)).isEqualTo(60);
        assertThat(TravelPlanRouteJobService.retrySeconds(6)).isEqualTo(900);
        assertThat(TravelPlanRouteJobService.retrySeconds(Integer.MAX_VALUE)).isEqualTo(900);
    }

    private void ownedPlan() {
        var access = mock(TravelPlanRepository.RouteAccess.class);
        when(access.getPlanId()).thenReturn(1L);
        when(plans.findRouteAccessById(1L)).thenReturn(Optional.of(access));
    }

    private TravelPlanRoute readyRoute(LocalDate date) {
        var route = TravelPlanRoute.builder().routeDate(date).build();
        route.begin("internal-hash");
        route.finish(READY, null, List.of(List.of(126.5, 33.5), List.of(126.6, 33.6)), 18342, 2421000L);
        return route;
    }

    @Test void queryWithoutDateLoadsAllRoutesAndAbsentJobIsNotRequested() {
        ownedPlan();
        var date = LocalDate.of(2026, 9, 10);
        when(routes.findAllByTravelPlanIdOrderByRouteDateAsc(1L)).thenReturn(List.of(readyRoute(date)));
        var result = service.getRoutes(1L, null);
        assertThat(result.planId()).isEqualTo(1L);
        assertThat(result.generation().status()).isEqualTo(RouteGenerationStatus.NOT_REQUESTED);
        assertThat(result.routes()).extracting(r -> r.date()).containsExactly(date);
        verify(routes).findAllByTravelPlanIdOrderByRouteDateAsc(1L);
        verify(routes, never()).findByTravelPlanIdAndRouteDate(anyLong(), any());
        verify(plans, never()).findByIdWithPreferences(anyLong());
    }

    @Test void queryWithDateLoadsOnlyRequestedRouteAndPreservesReadyWhilePending() {
        ownedPlan();
        var date = LocalDate.of(2026, 9, 10);
        when(routes.findByTravelPlanIdAndRouteDate(1L, date)).thenReturn(Optional.of(readyRoute(date)));
        when(routeJobs.findStateByPlanId(1L)).thenReturn(Optional.of(
                new TravelPlanRouteJobRepository.JobState(TravelPlanRouteJobStatus.PENDING, false)));
        var result = service.getRoutes(1L, date);
        assertThat(result.generation().status()).isEqualTo(RouteGenerationStatus.PENDING);
        assertThat(result.routes()).singleElement().satisfies(route -> {
            assertThat(route.status()).isEqualTo(READY);
            assertThat(route.path()).containsExactly(List.of(126.5, 33.5), List.of(126.6, 33.6));
            assertThat(route.distance()).isEqualTo(18342);
            assertThat(route.duration()).isEqualTo(2421000L);
        });
        verify(routes).findByTravelPlanIdAndRouteDate(1L, date);
        verify(routes, never()).findAllByTravelPlanIdOrderByRouteDateAsc(anyLong());
    }

    @Test void unmatchedDateStillReturnsEmptyRoutes() {
        ownedPlan();
        var date = LocalDate.of(1900, 1, 1);
        assertThat(service.getRoutes(1L, date).routes()).isEmpty();
        verify(routes).findByTravelPlanIdAndRouteDate(1L, date);
        verify(routes, never()).findAllByTravelPlanIdOrderByRouteDateAsc(anyLong());
    }

    @ParameterizedTest
    @CsvSource({"PENDING,false,PENDING", "RUNNING,true,RUNNING", "RUNNING,false,PENDING", "DONE,false,DONE"})
    void queryMapsJobState(String stored, boolean leaseValid, String expected) {
        ownedPlan();
        when(routeJobs.findStateByPlanId(1L)).thenReturn(Optional.of(
                new TravelPlanRouteJobRepository.JobState(TravelPlanRouteJobStatus.valueOf(stored), leaseValid)));
        assertThat(service.getRoutes(1L, null).generation().status())
                .isEqualTo(RouteGenerationStatus.valueOf(expected));
    }

    @Test void queryRejectsMissingOrFilteredPlanBeforeReadingRoutesOrJob() {
        assertThatThrownBy(() -> service.getRoutes(1L, null))
                .isInstanceOfSatisfying(GeneralException.class,
                        e -> assertThat(e.getCode()).isEqualTo(PlanErrorCode.PLAN_NOT_FOUND));
        verifyNoInteractions(routes, routeJobs);
    }

}
