package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.plan.entity.*;
import com.example.jejugilmoa.domain.plan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus.*;

/** 계획 잠금은 입력 확정과 결과 반영 시에만 짧게 획득한다. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class TravelPlanRouteStore {
    private final TravelPlanRepository plans;
    private final TravelCourseRepository courses;
    private final DayDepartureRepository departures;
    private final TravelPlanRouteRepository routes;

    public List<PlanRouteInput> prepare(Long planId) {
        TravelPlan plan = plans.findByIdForUpdate(planId).orElse(null);
        if (plan == null) return List.of();
        Map<LocalDate, PlanRouteInput> inputs = inputs(plan);
        List<TravelPlanRoute> existing = routes.findAllByTravelPlanIdOrderByRouteDateAsc(planId);
        Map<LocalDate, TravelPlanRoute> byDate = new HashMap<>();
        for (TravelPlanRoute route : existing) {
            if (!inputs.containsKey(route.getRouteDate())) routes.delete(route);
            else byDate.put(route.getRouteDate(), route);
        }
        List<PlanRouteInput> pending = new ArrayList<>();
        for (PlanRouteInput input : inputs.values()) {
            TravelPlanRoute route = byDate.get(input.date());
            if (route != null && route.getStatus() == READY && input.hash().equals(route.getRouteHash())) continue;
            if (route == null) route = TravelPlanRoute.builder().travelPlan(plan).routeDate(input.date()).build();
            route.begin(input.hash());
            routes.save(route);
            pending.add(input);
        }
        return List.copyOf(pending);
    }

    public void complete(Long planId, PlanRouteInput input, TravelPlanRouteService.Result result) {
        TravelPlan plan = plans.findByIdForUpdate(planId).orElse(null);
        if (plan == null) return;
        PlanRouteInput current = inputs(plan).get(input.date());
        // 외부 호출 중 다른 요청이 경유지를 변경했다면 이전 결과를 버린다.
        if (current == null || !current.hash().equals(input.hash())) return;
        routes.findByTravelPlanIdAndRouteDate(planId, input.date()).ifPresent(route -> {
            if (!input.hash().equals(route.getRouteHash())) return;
            // 동일 입력의 늦게 도착한 실패가 먼저 성공한 경로를 지우지 않게 한다.
            if (route.getStatus() == READY) return;
            route.finish(result.status(), result.failureCode(), result.path(), result.distance(), result.duration());
        });
    }

    private Map<LocalDate, PlanRouteInput> inputs(TravelPlan plan) {
        Map<LocalDate, List<PlanRouteInput.Point>> points = new TreeMap<>();
        plan.getStartDate().datesUntil(plan.getEndDate().plusDays(1))
                .forEach(date -> points.put(date, new ArrayList<>()));
        for (DayDeparture departure : departures.findAllByTravelPlanIdOrderByVisitDateAsc(plan.getId())) {
            points.computeIfAbsent(departure.getVisitDate(), date -> new ArrayList<>())
                    .add(PlanRouteInput.Point.of(departure.getLongitude(), departure.getLatitude()));
        }
        for (TravelCourse course : courses.findAllByTravelPlanIdWithPlaceOrderByVisitDateAscSequenceOrderAsc(plan.getId())) {
            points.computeIfAbsent(course.getVisitDate(), date -> new ArrayList<>())
                    .add(PlanRouteInput.Point.of(course.getPlace().getLongitude(), course.getPlace().getLatitude()));
        }
        Map<LocalDate, PlanRouteInput> result = new TreeMap<>();
        points.forEach((date, coordinates) -> result.put(date, new PlanRouteInput(date, coordinates)));
        return result;
    }
}
