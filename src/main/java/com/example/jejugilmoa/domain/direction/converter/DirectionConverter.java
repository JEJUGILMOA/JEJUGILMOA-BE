package com.example.jejugilmoa.domain.direction.converter;

import com.example.jejugilmoa.domain.direction.dto.DirectionResponse;
import com.example.jejugilmoa.global.external.navermap.dto.NaverDirectionsResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DirectionConverter {

    /** 네이버 path 좌표는 [경도, 위도] 순서 — 응답에서는 위도/경도 필드로 변환한다. */
    public DirectionResponse toResponse(String option, NaverDirectionsResponse.NaverRoute route) {
        NaverDirectionsResponse.Summary summary = route.summary();
        List<DirectionResponse.Coordinate> path = route.path() == null
            ? List.of()
            : deduplicateConsecutive(route.path().stream()
                .map(point -> new DirectionResponse.Coordinate(point.get(1), point.get(0)))
                .toList());

        return new DirectionResponse(
            option,
            new DirectionResponse.RouteSummary(
                summary.distance(),
                summary.duration(),
                summary.tollFare(),
                summary.taxiFare(),
                summary.fuelPrice()
            ),
            path
        );
    }

    private List<DirectionResponse.Coordinate> deduplicateConsecutive(List<DirectionResponse.Coordinate> points) {
        List<DirectionResponse.Coordinate> result = new ArrayList<>();
        for (DirectionResponse.Coordinate point : points) {
            if (result.isEmpty() || !result.getLast().equals(point)) {
                result.add(point);
            }
        }
        return result;
    }
}
