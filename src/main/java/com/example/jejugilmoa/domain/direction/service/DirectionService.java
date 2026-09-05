package com.example.jejugilmoa.domain.direction.service;

import com.example.jejugilmoa.domain.direction.converter.DirectionConverter;
import com.example.jejugilmoa.domain.direction.dto.DirectionResponse;
import com.example.jejugilmoa.domain.direction.enums.RouteOption;
import com.example.jejugilmoa.domain.direction.exception.DirectionErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.navermap.NaverDirectionsClient;
import com.example.jejugilmoa.global.external.navermap.NaverMapException;
import com.example.jejugilmoa.global.external.navermap.dto.NaverDirectionsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectionService {

    private static final int MAX_WAYPOINTS = 5;

    // 네이버 응답 코드: 1=출발지·목적지 동일, 2~5=경로 탐색 불가
    private static final int NAVER_CODE_SAME_START_GOAL = 1;
    private static final int NAVER_CODE_ROUTE_NOT_FOUND_MAX = 5;

    private final NaverDirectionsClient naverDirectionsClient;
    private final DirectionConverter directionConverter;

    /**
     * 자동차 길찾기. waypoints는 "위도,경도|위도,경도" 형식(최대 5개), 없으면 null.
     * 동일 좌표·경유지·옵션 조합은 Redis에 캐싱해 네이버 중복 호출을 막는다 (TTL 30분).
     * 예외 발생 시에는 캐싱되지 않으므로 실패 응답이 재사용될 일은 없다.
     */
    @Cacheable(cacheNames = "directions",
        key = "#startLat + ':' + #startLng + ':' + #goalLat + ':' + #goalLng + ':' + (#waypoints ?: '') + ':' + #optionValue")
    public DirectionResponse getDriving(double startLat, double startLng,
                                        double goalLat, double goalLng,
                                        String waypoints, String optionValue) {
        validateCoordinate(startLat, startLng);
        validateCoordinate(goalLat, goalLng);
        RouteOption option = RouteOption.from(optionValue);
        String naverWaypoints = toNaverWaypoints(waypoints);

        NaverDirectionsResponse response;
        try {
            response = naverDirectionsClient.getDriving(
                toNaverCoordinate(startLat, startLng),
                toNaverCoordinate(goalLat, goalLng),
                naverWaypoints,
                option.getValue());
        } catch (NaverMapException e) {
            log.error("네이버 길찾기 API 호출 실패", e);
            throw new GeneralException(DirectionErrorCode.NAVER_API_ERROR);
        }

        if (!response.isSuccess()) {
            handleFailureCode(response);
        }

        NaverDirectionsResponse.NaverRoute route = response.firstRoute(option.getValue());
        if (route == null || route.summary() == null) {
            log.warn("네이버 길찾기 응답에 경로 없음: option={}, code={}", option.getValue(), response.code());
            throw new GeneralException(DirectionErrorCode.ROUTE_NOT_FOUND);
        }
        return directionConverter.toResponse(option.getValue(), route);
    }

    private void handleFailureCode(NaverDirectionsResponse response) {
        int code = response.code() == null ? -1 : response.code();
        if (code == NAVER_CODE_SAME_START_GOAL) {
            throw new GeneralException(DirectionErrorCode.SAME_START_GOAL);
        }
        if (code >= 2 && code <= NAVER_CODE_ROUTE_NOT_FOUND_MAX) {
            log.info("네이버 길찾기 경로 없음: code={}, message={}", code, response.message());
            throw new GeneralException(DirectionErrorCode.ROUTE_NOT_FOUND);
        }
        log.error("네이버 길찾기 API 오류 응답: code={}, message={}", code, response.message());
        throw new GeneralException(DirectionErrorCode.NAVER_API_ERROR);
    }

    private void validateCoordinate(double lat, double lng) {
        if (Double.isNaN(lat) || Double.isNaN(lng)
                || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new GeneralException(DirectionErrorCode.INVALID_COORDINATE);
        }
    }

    /** 네이버 규격은 "경도,위도" 순서. */
    private String toNaverCoordinate(double lat, double lng) {
        return lng + "," + lat;
    }

    /** "위도,경도|위도,경도" → 네이버 규격 "경도,위도|경도,위도"로 변환. */
    private String toNaverWaypoints(String waypoints) {
        if (!StringUtils.hasText(waypoints)) {
            return null;
        }
        String[] points = waypoints.split("\\|", -1);
        if (points.length > MAX_WAYPOINTS) {
            throw new GeneralException(DirectionErrorCode.TOO_MANY_WAYPOINTS);
        }
        return Arrays.stream(points)
            .map(this::parseWaypoint)
            .collect(Collectors.joining("|"));
    }

    private String parseWaypoint(String point) {
        String[] parts = point.split(",", -1);
        if (parts.length != 2) {
            throw new GeneralException(DirectionErrorCode.INVALID_WAYPOINT_FORMAT);
        }
        double lat;
        double lng;
        try {
            lat = Double.parseDouble(parts[0].trim());
            lng = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new GeneralException(DirectionErrorCode.INVALID_WAYPOINT_FORMAT);
        }
        validateCoordinate(lat, lng);
        return toNaverCoordinate(lat, lng);
    }
}
