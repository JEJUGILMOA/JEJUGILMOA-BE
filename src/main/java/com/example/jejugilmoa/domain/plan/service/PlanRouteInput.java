package com.example.jejugilmoa.domain.plan.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/** DB에서 확정된 좌표를 Directions가 사용하는 double 값으로 정규화한다. */
public record PlanRouteInput(LocalDate date, List<Point> points) {
    public static final String OPTION = "traoptimal";

    public PlanRouteInput {
        points = List.copyOf(points);
    }

    public record Point(Double longitude, Double latitude) {
        public static Point of(BigDecimal longitude, BigDecimal latitude) {
            return new Point(normalize(longitude), normalize(latitude));
        }
        private static Double normalize(BigDecimal value) {
            if (value == null) return null;
            double number = value.doubleValue();
            return number == 0 ? 0.0 : number;
        }
        public boolean valid() {
            return longitude != null && latitude != null && Double.isFinite(longitude)
                    && Double.isFinite(latitude) && longitude >= -180 && longitude <= 180
                    && latitude >= -90 && latitude <= 90;
        }
        String naverCoordinate() { return longitude + "," + latitude; }
        String directionCoordinate() { return latitude + "," + longitude; }
    }

    public String hash() {
        String normalized = OPTION + "\n" + points.stream().map(Point::naverCoordinate)
                .collect(Collectors.joining("|"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    public String waypoints() {
        if (points.size() <= 2) return null;
        return points.subList(1, points.size() - 1).stream().map(Point::directionCoordinate)
                .collect(Collectors.joining("|"));
    }
}
