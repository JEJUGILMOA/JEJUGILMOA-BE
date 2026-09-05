package com.example.jejugilmoa.global.external.navermap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * 네이버 클라우드 Maps Directions 5 API 응답.
 * route는 탐색 옵션(trafast 등)을 키로 하는 경로 배열이다.
 * code 0이 성공, 1~5는 경로 없음 등 조회 불가 사유.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverDirectionsResponse(
    Integer code,
    String message,
    String currentDateTime,
    Map<String, List<NaverRoute>> route
) {

    public boolean isSuccess() {
        return code != null && code == 0;
    }

    /** 요청한 옵션의 첫 번째 경로. 없으면 null. */
    public NaverRoute firstRoute(String option) {
        if (route == null) return null;
        List<NaverRoute> routes = route.get(option);
        return (routes == null || routes.isEmpty()) ? null : routes.get(0);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NaverRoute(Summary summary, List<List<Double>> path) {}

    /** distance는 미터, duration은 밀리초 단위. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(
        int distance,
        long duration,
        int tollFare,
        int taxiFare,
        int fuelPrice,
        List<List<Double>> bbox
    ) {}
}
