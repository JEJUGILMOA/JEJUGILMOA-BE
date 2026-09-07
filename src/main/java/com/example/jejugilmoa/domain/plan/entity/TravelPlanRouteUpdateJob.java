package com.example.jejugilmoa.domain.plan.entity;

import com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteJobStatus;
import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** 원자적 병합/claim은 repository SQL로만 수행한다. */
@Entity
@Table(name = "travel_plan_route_update_job", indexes = {
        @Index(name = "uk_route_job_plan", columnList = "plan_id", unique = true),
        @Index(name = "idx_route_job_pending", columnList = "status,next_attempt_at"),
        @Index(name = "idx_route_job_lease", columnList = "status,lease_until")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TravelPlanRouteUpdateJob extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TravelPlan travelPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TravelPlanRouteJobStatus status;

    // RUNNING 동안 들어온 변경을 완료 처리로 유실시키지 않는다.
    @Column(nullable = false)
    private boolean dirty;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    private Instant leaseUntil;

    private UUID leaseToken;

    @Column(length = 100)
    private String lastError;
}
