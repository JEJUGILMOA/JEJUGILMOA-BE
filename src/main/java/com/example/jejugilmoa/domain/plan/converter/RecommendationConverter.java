package com.example.jejugilmoa.domain.plan.converter;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.plan.dto.PlaceRecommendationItem;
import com.example.jejugilmoa.domain.plan.enums.TransportMode;

public class RecommendationConverter {

    // 이동 수단별 평균 속도 (미터/분)
    // 도보: ~4.8 km/h = 80 m/min
    // 대중교통: ~20 km/h = 333 m/min (제주 버스 기준)
    // 차량: ~30 km/h = 500 m/min (제주 시내도로 기준)
    private static final int WALK_MPM    = 80;
    private static final int TRANSIT_MPM = 333;
    private static final int CAR_MPM     = 500;

    private RecommendationConverter() {}

    /**
     * Place를 추천 경유지 DTO로 변환합니다.
     *
     * @param place      추천 장소
     * @param deptLngLat 출발지 [경도, 위도] 배열. null이면 이동 시간 0 반환
     * @param mode       이동 수단 (속도 추정에 사용)
     */
    public static PlaceRecommendationItem toItem(Place place, double[] deptLngLat, TransportMode mode) {
        int travelMinutes = 0;
        if (deptLngLat != null) {
            // Haversine 공식으로 직선거리(미터)를 구하고 이동 속도로 나눠 시간 추정
            double distMeters = haversineMeters(
                    deptLngLat[1], deptLngLat[0],
                    place.getLatitude().doubleValue(),
                    place.getLongitude().doubleValue()
            );
            int speedMpm = switch (mode) {
                case TRANSIT -> TRANSIT_MPM;
                case CAR     -> CAR_MPM;
                default      -> WALK_MPM;
            };
            travelMinutes = Math.max(1, (int) Math.round(distMeters / speedMpm));
        }

        return new PlaceRecommendationItem(
                place.getId(),
                place.getName(),
                place.getCategory().getName(),
                place.getImageUrl(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                travelMinutes
        );
    }

    /**
     * Haversine 공식으로 두 위경도 좌표 사이의 직선거리(미터)를 계산합니다.
     * PostGIS ST_Distance와 달리 DB 호출 없이 서비스 레이어에서 빠르게 추정할 때 사용합니다.
     * 지구 곡률을 감안한 공식이므로 수 km 거리에서도 오차가 1% 미만입니다.
     */
    private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6_371_000.0; // 지구 평균 반지름 (미터)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
