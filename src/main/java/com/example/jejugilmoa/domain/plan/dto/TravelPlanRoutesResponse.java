package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TravelPlanRoutesResponse(List<Route> routes) {
    public record Route(LocalDate date, TravelPlanRouteStatus status, String option,
                        @Schema(description = "총 거리(meter), READY 이외 null") Integer distance,
                        @Schema(description = "총 소요 시간(millisecond), READY 이외 null") Long duration,
                        Instant calculatedAt,
                        @Schema(description = "[longitude, latitude] 배열 목록, READY 이외 빈 배열")
                        List<List<Double>> path, String failureCode) {}
}
