package com.example.jejugilmoa.domain.plan.controller;

import com.example.jejugilmoa.domain.plan.dto.TravelPlanRoutesResponse;
import com.example.jejugilmoa.domain.plan.service.TravelPlanRouteService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@Tag(name = "여행 계획", description = "여행 계획 생성 및 관리")
@RestController
@RequestMapping("/api/plans/{planId}/routes")
@RequiredArgsConstructor
public class TravelPlanRouteController {
    private final TravelPlanRouteService service;

    @Operation(summary = "날짜별 저장 경로 조회", description = "본인 계획만 조회 가능. date 생략 시 날짜 오름차순 전체 조회. "
            + "기존 미계산 계획은 빈 목록. READY 경로는 [경도, 위도] 배열, 거리 meter, 시간 millisecond. "
            + "routes[].status는 날짜별 결과: CALCULATING은 계산 중, FAILED는 계산 실패, UNSUPPORTED는 전체 7지점 초과, NOT_REQUIRED는 2지점 미만. "
            + "generation은 날짜 필터와 무관한 계획 단위 갱신 작업 상태: NOT_REQUESTED는 job 없음, PENDING은 생성/재시도 대기, "
            + "RUNNING은 유효한 lease로 처리 중, DONE은 예약 작업 완료. 만료된 RUNNING job은 PENDING으로 반환. "
            + "DONE이 모든 route READY를 의미하지 않으며, PENDING/RUNNING 중에는 기존 READY route가 일시적으로 포함될 수 있음.")
    @GetMapping
    public ApiResponse<TravelPlanRoutesResponse> getRoutes(
            @PathVariable Long planId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, service.getRoutes(planId, date));
    }
}
