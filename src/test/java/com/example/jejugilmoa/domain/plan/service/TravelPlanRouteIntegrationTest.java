package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.direction.dto.DirectionResponse;
import com.example.jejugilmoa.domain.direction.service.DirectionService;
import com.example.jejugilmoa.domain.plan.dto.*;
import com.example.jejugilmoa.domain.plan.entity.*;
import com.example.jejugilmoa.domain.plan.repository.*;
import com.example.jejugilmoa.domain.place.entity.*;
import com.example.jejugilmoa.domain.place.repository.*;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import org.locationtech.jts.geom.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties = {"app.sync.run-on-startup=false", "spring.jpa.show-sql=false", "app.plan-route.worker.enabled=false"})
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
    @Autowired TravelPlanRouteWorker worker;
    @Autowired TravelPlanRouteJobService jobs;
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
        assertThat(jobStatus(id)).isEqualTo("PENDING");
        assertThat(routes.findAllByTravelPlanIdOrderByRouteDateAsc(id)).isEmpty();
        assertThat(worker.runOnce()).isTrue();
        return id;
    }
    private void replace(Long id, TravelPlanCreateRequest request) {
        service.replace(id, user.getId(), request);
        makeDue(id);
        assertThat(worker.runOnce()).isTrue();
    }
    private String jobStatus(Long id) {
        return jdbc.sql("SELECT status FROM travel_plan_route_update_job WHERE plan_id = :id")
                .param("id", id).query(String.class).single();
    }
    private void makeDue(Long id) {
        jdbc.sql("UPDATE travel_plan_route_update_job SET next_attempt_at = now() - interval '1 second' WHERE plan_id = :id")
                .param("id", id).update();
    }
    private void expire(Long id) {
        jdbc.sql("UPDATE travel_plan_route_update_job SET lease_until = now() - interval '1 second' WHERE plan_id = :id")
                .param("id", id).update();
    }
    private TravelPlanRoute route(Long id) { return routes.findByTravelPlanIdAndRouteDate(id, date).orElseThrow(); }

    @Test void createAndReplaceUseFinalInputOnceAndReuseReadyHash() {
        Long id = create(0, 1);
        var saved = route(id);
        assertThat(saved.getStatus()).isEqualTo(READY);
        assertThat(saved.getPath()).containsExactly(List.of(126.5, 33.5), List.of(126.6, 33.6));
        assertThat(routes.findByTravelPlanIdAndRouteDate(id, date.plusDays(1)).orElseThrow().getStatus()).isEqualTo(NOT_REQUIRED);
        replace(id, request("제목만 변경", 0, 1));
        assertThat(route(id).getCalculatedAt()).isEqualTo(saved.getCalculatedAt());
        verify(directions, times(1)).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
        replace(id, request("순서 변경", 1, 0));
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
        replace(id, request("재시도", 0));
        assertThat(route(id).getStatus()).isEqualTo(READY);
    }

    @Test void overLimitAndRemovedCoursesClearOldGeometry() {
        Long id = create(0);
        replace(id, request("초과", 0, 1, 2, 3, 4, 5, 6));
        assertThat(route(id).getStatus()).isEqualTo(UNSUPPORTED);
        assertThat(route(id).getPath()).isNull();
        assertThat(route(id).getCalculatedAt()).isNull();
        replace(id, request("장소 없음"));
        assertThat(route(id).getStatus()).isEqualTo(NOT_REQUIRED);
        verify(directions, times(1)).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
    }

    @Test void inProgressAddRemoveAndReorderRefreshRoutes() {
        Long id = create(0, 1, 2);
        tx.executeWithoutResult(status -> plans.findByIdForUpdate(id).orElseThrow().start(LocalDateTime.now()));
        String oldHash = route(id).getRouteHash();
        trips.addWaypoint(id, user.getId(), new WaypointAddRequest(stops.get(3).getId(), date, false));
        worker.runOnce();
        assertThat(route(id).getRouteHash()).isNotEqualTo(oldHash);
        var ordered = courses.findAllByTravelPlanIdAndVisitDateOrderBySequenceOrderAsc(id, date);
        trips.removeWaypoint(id, user.getId(), ordered.get(1).getId());
        worker.runOnce();
        String removedHash = route(id).getRouteHash();
        var remaining = courses.findAllByTravelPlanIdAndVisitDateOrderBySequenceOrderAsc(id, date);
        trips.reorderWaypoints(id, user.getId(), new WaypointReorderRequest(date,
                List.of(remaining.get(2).getId(), remaining.get(0).getId(), remaining.get(1).getId())));
        worker.runOnce();
        assertThat(route(id).getRouteHash()).isNotEqualTo(removedHash);
        verify(directions, times(4)).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
    }

    @Test void legacyDepartureFallbackAndStaleResultProtection() {
        Long id = create(0, 1);
        tx.executeWithoutResult(status -> departures.deleteAll(departures.findAllByTravelPlanIdOrderByVisitDateAsc(id)));
        tx.executeWithoutResult(status -> jobs.enqueue(id));
        var claim = jobs.claim().orElseThrow();
        var input = store.prepare(claim).getFirst();
        assertThat(input.points()).hasSize(2);
        assertThat(input.points().getFirst().longitude()).isEqualTo(126.5);
        service.replace(id, user.getId(), request("최신 입력", 2, 3));
        // 현재 입력과 다른 결과는 소유권이 유효해도 반영하지 않는다.
        store.complete(claim, input, TravelPlanRouteService.Result.empty(FAILED, "OLD_FAILURE"));
        assertThat(route(id).getStatus()).isEqualTo(CALCULATING);
        expire(id);
        worker.runOnce();
        String latestHash = route(id).getRouteHash();
        assertThatThrownBy(() -> store.complete(claim, input, TravelPlanRouteService.Result.empty(FAILED, "OLD_FAILURE")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(route(id).getRouteHash()).isEqualTo(latestHash);
        assertThat(route(id).getStatus()).isEqualTo(READY);
    }

    @Test void departureCoordinateChangeRecalculatesAndVisitedStopCannotMove() {
        Long id = create(0, 1);
        String oldHash = route(id).getRouteHash();
        var original = request("출발지 변경", 0, 1);
        var changed = new DayPlanRequest(date, null, "다른 출발지", new BigDecimal("33.48"),
                new BigDecimal("126.48"), original.days().getFirst().waypoints());
        replace(id, new TravelPlanCreateRequest(original.title(), original.startDate(),
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
            Long id = service.create(user.getId(), request("롤백", 0)).planId();
            assertThat(jobStatus(id)).isEqualTo("PENDING");
            status.setRollbackOnly();
        });
        assertThat(jdbc.sql("SELECT count(*) FROM travel_plan_route_update_job j JOIN travel_plan p ON p.id=j.plan_id WHERE p.user_id=:id")
                .param("id", user.getId()).query(Long.class).single()).isZero();
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
    private Long enqueueOnly() {
        Long id = service.create(user.getId(), request("내구성 검증", 0, 1)).planId();
        planIds.add(id);
        return id;
    }

    @Test void committedJobSurvivesWithoutListenerAndWorkerConsumesIt() {
        Long id = enqueueOnly();
        assertThat(jobStatus(id)).isEqualTo("PENDING");
        assertThat(routes.findAllByTravelPlanIdOrderByRouteDateAsc(id)).isEmpty();
        verifyNoInteractions(directions);
        assertThat(worker.runOnce()).isTrue();
        assertThat(jobStatus(id)).isEqualTo("DONE");
        assertThat(route(id).getStatus()).isEqualTo(READY);
        assertThat(worker.runOnce()).isFalse();
    }

    @Test void uncommittedJobIsInvisibleAndRollbackOfReplacementRestoresDoneJob() {
        Long id = create(0);
        String hash = route(id).getRouteHash();
        tx.executeWithoutResult(status -> {
            service.replace(id, user.getId(), request("롤백할 수정", 1));
            assertThat(jobStatus(id)).isEqualTo("PENDING");
            var independent = new TransactionTemplate(transactionManager);
            independent.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            String committedStatus = independent.execute(other -> jobStatus(id));
            assertThat(committedStatus).isEqualTo("DONE");
            status.setRollbackOnly();
        });
        assertThat(jobStatus(id)).isEqualTo("DONE");
        assertThat(route(id).getRouteHash()).isEqualTo(hash);
    }

    @Test void concurrentClaimHasExactlyOneOwner() throws Exception {
        Long id = enqueueOnly();
        CyclicBarrier start = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Optional<TravelPlanRouteJobClaim>> claim = () -> {
                start.await(5, TimeUnit.SECONDS);
                return jobs.claim();
            };
            var first = pool.submit(claim);
            var second = pool.submit(claim);
            var claims = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            assertThat(claims.stream().filter(Optional::isPresent).count()).isOne();
            assertThat(jobStatus(id)).isEqualTo("RUNNING");
        }
    }

    @Test void activeCalculatingAndRepeatedChangesProduceOnlyOneDirectionsCall() throws Exception {
        Long id = enqueueOnly();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(call -> {
            entered.countDown();
            assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            return response();
        }).when(directions).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(worker::runOnce);
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(route(id).getStatus()).isEqualTo(CALCULATING);
                service.replace(id, user.getId(), request("메타 변경1", 0, 1));
                service.replace(id, user.getId(), request("메타 변경2", 0, 1));
                assertThat(pool.submit(worker::runOnce).get(5, TimeUnit.SECONDS)).isFalse();
                assertThat(jdbc.sql("SELECT count(*) FROM travel_plan_route_update_job WHERE plan_id=:id")
                        .param("id", id).query(Long.class).single()).isOne();
            } finally {
                release.countDown();
            }
            assertThat(first.get(5, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(jobStatus(id)).isEqualTo("PENDING");
        worker.runOnce();
        assertThat(jobStatus(id)).isEqualTo("DONE");
        verify(directions, times(1)).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
    }

    @Test void abandonedCalculatingLeaseIsReclaimedAndOldTokenCannotRenewOrWrite() {
        Long id = enqueueOnly();
        var abandoned = jobs.claim().orElseThrow();
        var inputs = store.prepare(abandoned);
        assertThat(route(id).getStatus()).isEqualTo(CALCULATING);
        expire(id);
        assertThat(jobs.renew(abandoned)).isFalse();
        assertThat(worker.runOnce()).isTrue();
        assertThat(route(id).getStatus()).isEqualTo(READY);
        assertThat(jobStatus(id)).isEqualTo("DONE");
        assertThatThrownBy(() -> store.complete(abandoned, inputs.getFirst(),
                TravelPlanRouteService.Result.empty(FAILED, "STALE"))).isInstanceOf(IllegalStateException.class);
        jobs.finish(abandoned, false, "STALE");
        assertThat(jobStatus(id)).isEqualTo("DONE");
        assertThat(route(id).getStatus()).isEqualTo(READY);
    }

    @Test void heartbeatRenewalExtendsLeaseAndBlocksAnotherClaim() {
        Long id = enqueueOnly();
        var claim = jobs.claim().orElseThrow();
        jdbc.sql("UPDATE travel_plan_route_update_job SET lease_until=now()+interval '5 seconds' WHERE plan_id=:id")
                .param("id", id).update();
        assertThat(jobs.renew(claim)).isTrue();
        assertThat(jdbc.sql("SELECT lease_until > now()+interval '100 seconds' FROM travel_plan_route_update_job WHERE plan_id=:id")
                .param("id", id).query(Boolean.class).single()).isTrue();
        assertThat(jobs.claim()).isEmpty();
    }

    @Test void failedDirectionsRetryWithBackoffWithoutAnotherPlanChange() {
        doThrow(new IllegalStateException("외부 실패")).when(directions)
                .getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
        Long id = enqueueOnly();
        worker.runOnce();
        assertThat(route(id).getStatus()).isEqualTo(FAILED);
        assertThat(jobStatus(id)).isEqualTo("PENDING");
        assertThat(jdbc.sql("SELECT next_attempt_at > now()+interval '20 seconds' FROM travel_plan_route_update_job WHERE plan_id=:id")
                .param("id", id).query(Boolean.class).single()).isTrue();
        assertThat(worker.runOnce()).isFalse();
        doReturn(response()).when(directions).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
        makeDue(id);
        worker.runOnce();
        assertThat(jobStatus(id)).isEqualTo("DONE");
        assertThat(route(id).getStatus()).isEqualTo(READY);
        verify(directions, times(2)).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
    }

    @Test void expiredWorkerCannotOverwriteNewOwnerEvenWithSameHash() throws Exception {
        Long id = enqueueOnly();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        doAnswer(call -> {
            if (calls.incrementAndGet() == 1) {
                entered.countDown();
                assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
                return new DirectionResponse("traoptimal", new DirectionResponse.RouteSummary(1, 1, 0, 0, 0), response().path());
            }
            return response();
        }).when(directions).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(worker::runOnce);
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                expire(id);
                assertThat(pool.submit(worker::runOnce).get(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                release.countDown();
            }
            first.get(5, TimeUnit.SECONDS);
        }
        assertThat(route(id).getTotalDistance()).isEqualTo(18342);
        assertThat(jobStatus(id)).isEqualTo("DONE");
    }

    @Test void rejectedStaleInputIsRetriedEvenWithoutAnotherEnqueue() {
        Long id = enqueueOnly();
        AtomicInteger calls = new AtomicInteger();
        doAnswer(call -> {
            if (calls.incrementAndGet() == 1) {
                // Place 좌표 동기화와 경로 계산이 겹친 상황. 좌표와 geom은 함께 갱신한다.
                jdbc.sql("UPDATE place SET longitude=126.9, geom=ST_SetSRID(ST_MakePoint(126.9, latitude),4326) WHERE id=:id")
                        .param("id", stops.getFirst().getId()).update();
            }
            return response();
        }).when(directions).getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), nullable(String.class), anyString());
        worker.runOnce();
        assertThat(jobStatus(id)).isEqualTo("PENDING");
        assertThat(route(id).getStatus()).isEqualTo(CALCULATING);
        makeDue(id);
        worker.runOnce();
        assertThat(jobStatus(id)).isEqualTo("DONE");
        assertThat(route(id).getStatus()).isEqualTo(READY);
    }

}
