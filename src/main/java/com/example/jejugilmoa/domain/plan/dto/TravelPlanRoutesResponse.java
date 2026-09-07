package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus;
import com.example.jejugilmoa.domain.plan.enums.RouteGenerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TravelPlanRoutesResponse(
        @Schema(description = "여행 계획 ID") Long planId,
        @Schema(description = "계획 단위 경로 갱신 작업 상태. 날짜 필터와 무관하게 계획 전체 기준") Generation generation,
        List<Route> routes) {
    public record Generation(
            @Schema(description = "NOT_REQUESTED: job 없음, PENDING: 생성/재시도/만료 lease 재처리 대기, "
                    + "RUNNING: 유효 lease로 처리 중, DONE: 예약 작업 완료. DONE이 모든 날짜 READY를 의미하지 않음. "
                    + "PENDING/RUNNING 중에는 이전 READY 경로가 포함될 수 있음")
            RouteGenerationStatus status) {}

    public record Route(LocalDate date,
                        @Schema(description = "날짜별 경로 계산 결과. READY만으로 최신 계획 반영 여부를 판단하지 말고 generation도 확인")
                        TravelPlanRouteStatus status, String option,
                        @Schema(description = "총 거리(meter), READY 이외 null") Integer distance,
                        @Schema(description = "총 소요 시간(millisecond), READY 이외 null") Long duration,
                        Instant calculatedAt,
                        @Schema(description = "[longitude, latitude] 배열 목록, READY 이외 빈 배열")
                        List<List<Double>> path, String failureCode) {}
}
