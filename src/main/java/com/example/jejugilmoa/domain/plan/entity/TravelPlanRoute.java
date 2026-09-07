package com.example.jejugilmoa.domain.plan.entity;

import com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus;
import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "travel_plan_route", indexes = {
        @Index(name = "uk_plan_route_date", columnList = "plan_id,route_date", unique = true)
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TravelPlanRoute extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TravelPlan travelPlan;

    @Column(nullable = false)
    private LocalDate routeDate;

    // Directions DTO의 lat/lng 객체와 달리 [경도, 위도] 배열로 저장한다.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<List<Double>> path;

    private Integer totalDistance;

    private Long totalDuration;

    @Column(nullable = false, length = 20)
    private String routeOption;

    @Column(nullable = false, length = 64)
    private String routeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TravelPlanRouteStatus status;

    @Column(length = 100)
    private String failureCode;

    private Instant calculatedAt;

    public void begin(String hash) {
        routeHash = hash;
        routeOption = "traoptimal";
        finish(TravelPlanRouteStatus.CALCULATING, null, null, null, null);
    }

    public void finish(TravelPlanRouteStatus status, String failureCode, List<List<Double>> path,
                       Integer distance, Long duration) {
        this.status = status;
        this.failureCode = failureCode;
        this.path = path;
        this.totalDistance = distance;
        this.totalDuration = duration;
        this.calculatedAt = status == TravelPlanRouteStatus.READY ? Instant.now() : null;
    }
}
