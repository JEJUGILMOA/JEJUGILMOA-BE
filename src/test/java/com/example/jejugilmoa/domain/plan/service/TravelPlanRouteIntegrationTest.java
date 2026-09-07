package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.direction.dto.DirectionResponse;
import com.example.jejugilmoa.domain.direction.service.DirectionService;
import com.example.jejugilmoa.domain.plan.dto.*;
import com.example.jejugilmoa.domain.plan.entity.*;
import com.example.jejugilmoa.domain.plan.event.PlanRouteChanged;
import com.example.jejugilmoa.domain.plan.repository.*;
import com.example.jejugilmoa.domain.place.entity.*;
import com.example.jejugilmoa.domain.place.repository.*;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import org.locationtech.jts.geom.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;
import static com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties = {"app.sync.run-on-startup=false", "spring.jpa.show-sql=false"})
class TravelPlanRouteIntegrationTest {
    @Autowired TravelPlanService service;
    @Autowired TravelPlanRouteService routeService;
    @Autowired TravelPlanRouteStore store;
    @Autowired TripService trips;
    @Autowired TravelPlanRepository plans;
    @Autowired TravelPlanRouteRepository routes;
    @Autowired TravelCourseRepository courses;
    @Autowired DayDepartureRepository departures;
    @Autowired PlaceRepository places;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ApplicationEventPublisher events;
    @Autowired JdbcClient jdbc;
    @MockitoBean DirectionService directions;
    User user;
    Category category;
    List<Place> stops;
    List<Long> planIds = new ArrayList<>();
    LocalDate date = LocalDate.now().plusDays(2);
    TransactionTemplate tx;

    @BeforeEach void setup() {
        tx = new TransactionTemplate(transactionManager);
        user = users.saveAndFlush(User.builder().nickname("경로 검증").build());
        category = categories.saveAndFlush(Category.builder().name("route-" + UUID.randomUUID()).build());
        var geometry = new GeometryFactory(new PrecisionModel(), 4326);
        stops = IntStream.range(0, 7).mapToObj(i -> places.saveAndFlush(Place.builder()
                .name("경로 장소 " + i).address("제주시").category(category)
                .latitude(BigDecimal.valueOf(33.5 + i * .01)).longitude(BigDecimal.valueOf(126.5 + i * .01))
                .geom(geometry.createPoint(new Coordinate(126.5 + i * .01, 33.5 + i * .01))).build())).toList();
        when(directions.getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString()))
                .thenAnswer(call -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    // 커밋된 계획 잠금을 별도 DB 트랜잭션에서 즉시 획득할 수 있어야 한다.
                    tx.executeWithoutResult(status -> {
                        jdbc.sql("SET LOCAL lock_timeout = '500ms'").update();
                        jdbc.sql("SELECT id FROM travel_plan WHERE user_id = :id FOR UPDATE")
                                .param("id", user.getId()).query(Long.class).list();
                    });
                    return response();
                });
    }

    @AfterEach void cleanup() {
        tx.executeWithoutResult(status -> {
            planIds.forEach(plans::deleteById);
            plans.flush();
            stops.forEach(places::delete);
            places.flush();
            categories.delete(category);
            users.deleteById(user.getId());
        });
    }

    private DirectionResponse response() {
        return new DirectionResponse("traoptimal", new DirectionResponse.RouteSummary(18342, 2421000, 0, 0, 0),
                List.of(new DirectionResponse.Coordinate(33.5, 126.5), new DirectionResponse.Coordinate(33.6, 126.6)));
    }
    private TravelPlanCreateRequest request(String title, int... indexes) {
        return new TravelPlanCreateRequest(title, date, date.plusDays(1), null, null,
                List.of(new DayPlanRequest(date, null, "출발지", new BigDecimal("33.49"), new BigDecimal("126.49"),
                        Arrays.stream(indexes).mapToObj(i -> new WaypointCreateRequest(stops.get(i).getId(), false)).toList())), null);
    }
    private Long create(int... indexes) {
        Long id = service.create(user.getId(), request("경로 테스트", indexes)).planId();
        planIds.add(id);
        return id;
    }
    private TravelPlanRoute route(Long id) { return routes.findByTravelPlanIdAndRouteDate(id, date).orElseThrow(); }

    @Test void createAndReplaceUseFinalInputOnceAndReuseReadyHash() {
        Long id = create(0, 1);
        var saved = route(id);
        assertThat(saved.getStatus()).isEqualTo(READY);
        assertThat(saved.getPath()).containsExactly(List.of(126.5, 33.5), List.of(126.6, 33.6));
        assertThat(routes.findByTravelPlanIdAndRouteDate(id, date.plusDays(1)).orElseThrow().getStatus()).isEqualTo(NOT_REQUIRED);
        service.replace(id, user.getId(), request("제목만 변경", 0, 1));
        assertThat(route(id).getCalculatedAt()).isEqualTo(saved.getCalculatedAt());
        verify(directions, times(1)).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
        service.replace(id, user.getId(), request("순서 변경", 1, 0));
        assertThat(route(id).getRouteHash()).isNotEqualTo(saved.getRouteHash());
        verify(directions, times(2)).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
        assertThat(jdbc.sql("SELECT jsonb_typeof(path -> 0) FROM travel_plan_route WHERE id = :id")
                .param("id", saved.getId()).query(String.class).single()).isEqualTo("array");
    }

    @Test void failureDoesNotRollbackAndSameInputCanRetry() {
        when(directions.getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString()))
                .thenThrow(new IllegalStateException("외부 연결 실패"));
        Long id = create(0);
        assertThat(plans.existsById(id)).isTrue();
        assertThat(route(id).getStatus()).isEqualTo(FAILED);
        doReturn(response()).when(directions).getDriving(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
        service.replace(id, user.getId(), request("재시도", 0));
        assertThat(route(id).getStatus()).isEqualTo(READY);
    }

    @Test void overLimitAndRemovedCoursesClearOldGeometry() {
        Long id = create(0);
        service.replace(id, user.getId(), request("초과", 0, 1, 2, 3, 4, 5, 6));
        assertThat(route(id).getStatus()).isEqualTo(UNSUPPORTED);
        assertThat(route(id).getPath()).isNull();
        assertThat(route(id).getCalculatedAt()).isNull();
        service.replace(id, user.getId(), request("장소 없음"));
        assertThat(route(id).getStatus()).isEqualTo(NOT_REQUIRED);
        verify(directions, times(1)).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
    }

    @Test void inProgressAddRemoveAndReorderRefreshRoutes() {
        Long id = create(0, 1, 2);
        tx.executeWithoutResult(status -> plans.findByIdForUpdate(id).orElseThrow().start(LocalDateTime.now()));
        String oldHash = route(id).getRouteHash();
        trips.addWaypoint(id, user.getId(), new WaypointAddRequest(stops.get(3).getId(), date, false));
        assertThat(route(id).getRouteHash()).isNotEqualTo(oldHash);
        var ordered = courses.findAllByTravelPlanIdAndVisitDateOrderBySequenceOrderAsc(id, date);
        trips.removeWaypoint(id, user.getId(), ordered.get(1).getId());
        String removedHash = route(id).getRouteHash();
        var remaining = courses.findAllByTravelPlanIdAndVisitDateOrderBySequenceOrderAsc(id, date);
        trips.reorderWaypoints(id, user.getId(), new WaypointReorderRequest(date,
                List.of(remaining.get(2).getId(), remaining.get(0).getId(), remaining.get(1).getId())));
        assertThat(route(id).getRouteHash()).isNotEqualTo(removedHash);
        verify(directions, times(4)).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
    }

    @Test void legacyDepartureFallbackAndStaleResultProtection() {
        Long id = create(0, 1);
        tx.executeWithoutResult(status -> departures.deleteAll(departures.findAllByTravelPlanIdOrderByVisitDateAsc(id)));
        var input = store.prepare(id).getFirst();
        assertThat(input.points()).hasSize(2);
        assertThat(input.points().getFirst().longitude()).isEqualTo(126.5);
        service.replace(id, user.getId(), request("최신 입력", 2, 3));
        String latestHash = route(id).getRouteHash();
        store.complete(id, input, TravelPlanRouteService.Result.empty(FAILED, "OLD_FAILURE"));
        assertThat(route(id).getRouteHash()).isEqualTo(latestHash);
        assertThat(route(id).getStatus()).isEqualTo(READY);
    }

    @Test void departureCoordinateChangeRecalculatesAndVisitedStopCannotMove() {
        Long id = create(0, 1);
        String oldHash = route(id).getRouteHash();
        var original = request("출발지 변경", 0, 1);
        var changed = new DayPlanRequest(date, null, "다른 출발지", new BigDecimal("33.48"),
                new BigDecimal("126.48"), original.days().getFirst().waypoints());
        service.replace(id, user.getId(), new TravelPlanCreateRequest(original.title(), original.startDate(),
                original.endDate(), null, null, List.of(changed), null));
        assertThat(route(id).getRouteHash()).isNotEqualTo(oldHash);
        var ordered = courses.findAllByTravelPlanIdAndVisitDateOrderBySequenceOrderAsc(id, date);
        tx.executeWithoutResult(status -> {
            plans.findByIdForUpdate(id).orElseThrow().start(LocalDateTime.now());
            courses.findById(ordered.getFirst().getId()).orElseThrow().skip(LocalDateTime.now());
        });
        assertThatThrownBy(() -> trips.reorderWaypoints(id, user.getId(), new WaypointReorderRequest(date,
                List.of(ordered.getLast().getId(), ordered.getFirst().getId()))))
                .isInstanceOf(com.example.jejugilmoa.global.apiPayload.exception.GeneralException.class);
        verify(directions, times(2)).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
    }

    @Test void rolledBackPlanDoesNotCallDirections() {
        tx.executeWithoutResult(status -> {
            service.create(user.getId(), request("롤백", 0));
            status.setRollbackOnly();
        });
        verifyNoInteractions(directions);
    }

    @Test void queryChecksOwnerAndFiltersDateAndDeletionCascades() {
        Long id = create(0);
        assertThat(routeService.getRoutes(id, user.getId(), date).routes()).hasSize(1);
        assertThatThrownBy(() -> routeService.getRoutes(id, -1L, null))
                .isInstanceOf(com.example.jejugilmoa.global.apiPayload.exception.GeneralException.class);
        service.deletePlan(id, user.getId());
        assertThat(routes.findAllByTravelPlanIdOrderByRouteDateAsc(id)).isEmpty();
        planIds.remove(id);
    }
}
