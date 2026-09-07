package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.direction.service.DirectionService;
import com.example.jejugilmoa.domain.plan.converter.TravelPlanRouteConverter;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanRoutesResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanRouteJobClaim;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.*;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanRouteService {
    private final TravelPlanRouteJobService jobs;
    private final TravelPlanRouteStore store;
    private final DirectionService directions;
    private final TravelPlanRepository plans;
    private final TravelPlanRouteRepository routes;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean refresh(TravelPlanRouteJobClaim claim, AtomicBoolean lost) {
        boolean success = true;
        for (PlanRouteInput input : store.prepare(claim)) {
            if (lost.get() || !jobs.renew(claim)) {
                lost.set(true);
                return false;
            }
            Result result = calculate(input);
            if (lost.get()) return false;
            // 입력이 바뀌어 결과를 버린 경우도 완료로 간주하지 않고 최신 입력을 다시 판단한다.
            if (!store.complete(claim, input, result) || result.status() == FAILED) success = false;
        }
        return success;
    }

    Result calculate(PlanRouteInput input) {
        if (input.points().size() < 2) return Result.empty(NOT_REQUIRED, null);
        if (input.points().size() > DirectionService.MAX_WAYPOINTS + 2)
            return Result.empty(UNSUPPORTED, "TOO_MANY_POINTS");
        if (input.points().stream().anyMatch(p -> !p.valid()))
            return Result.empty(FAILED, "INVALID_COORDINATE");
        try {
            var start = input.points().getFirst();
            var goal = input.points().getLast();
            var response = directions.getDriving(start.latitude(), start.longitude(), goal.latitude(),
                    goal.longitude(), input.waypoints(), PlanRouteInput.OPTION);
            if (response.path() == null || response.path().size() < 2 || response.summary() == null)
                return Result.empty(FAILED, "INVALID_DIRECTION_RESPONSE");
            return new Result(READY, null, TravelPlanRouteConverter.toStoredPath(response),
                    response.summary().distance(), response.summary().duration());
        } catch (GeneralException e) {
            return Result.empty(FAILED, e.getCode().getCode());
        } catch (RuntimeException e) {
            log.warn("계획 경로 계산 실패: date={}", input.date(), e);
            return Result.empty(FAILED, "DIRECTION_CALL_FAILED");
        }
    }

    @Transactional(readOnly = true)
    public TravelPlanRoutesResponse getRoutes(Long planId, Long userId, LocalDate date) {
        TravelPlan plan = plans.findByIdWithPreferences(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId))
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        return new TravelPlanRoutesResponse(routes.findAllByTravelPlanIdOrderByRouteDateAsc(planId).stream()
                .filter(route -> date == null || date.equals(route.getRouteDate()))
                .map(TravelPlanRouteConverter::toResponse).toList());
    }

    public record Result(TravelPlanRouteStatus status, String failureCode, List<List<Double>> path,
                         Integer distance, Long duration) {
        static Result empty(TravelPlanRouteStatus status, String code) {
            return new Result(status, code, null, null, null);
        }
    }
}
