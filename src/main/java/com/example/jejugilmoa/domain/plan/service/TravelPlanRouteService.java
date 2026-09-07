package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.direction.service.DirectionService;
import com.example.jejugilmoa.domain.plan.converter.TravelPlanRouteConverter;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanRoutesResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus;
import com.example.jejugilmoa.domain.plan.event.PlanRouteChanged;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.*;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.*;
import java.time.LocalDate;
import java.util.List;
import static com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanRouteService {
    private final TravelPlanRouteStore store;
    private final DirectionService directions;
    private final TravelPlanRepository plans;
    private final TravelPlanRouteRepository routes;

    // AFTER_COMMIT의 원래 영속성 컨텍스트도 중단하여 외부 호출 중 DB 트랜잭션을 유지하지 않는다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void refresh(PlanRouteChanged event) {
        try {
            for (PlanRouteInput input : store.prepare(event.planId())) {
                Result result = calculate(input);
                try {
                    store.complete(event.planId(), input, result);
                } catch (RuntimeException e) {
                    log.error("계획 경로 결과 저장 실패: planId={}, date={}", event.planId(), input.date(), e);
                }
            }
        } catch (RuntimeException e) {
            // 이미 커밋된 계획에 대해 저장 API가 실패 응답을 반환하지 않게 격리한다.
            log.error("계획 경로 입력 준비 실패: planId={}", event.planId(), e);
        }
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
