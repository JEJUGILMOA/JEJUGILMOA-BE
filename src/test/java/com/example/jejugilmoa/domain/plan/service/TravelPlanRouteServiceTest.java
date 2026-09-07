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
}
