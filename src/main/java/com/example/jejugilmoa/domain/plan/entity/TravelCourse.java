package com.example.jejugilmoa.domain.plan.entity;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "travel_course", indexes = {
        @Index(name = "idx_course_plan", columnList = "plan_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_plan_sequence", columnNames = {"plan_id", "sequence_order"}),
        @UniqueConstraint(name = "uk_course_plan_place", columnNames = {"plan_id", "place_id"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TravelCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "plan_id",
            nullable = false
    )
    private TravelPlan travelPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "place_id",
            nullable = false
    )
    private Place place;

    @Builder.Default
    @Column(name = "is_start", nullable = false)
    private boolean start = false; // 출발지 여부

    @Builder.Default
    @Column(name = "is_destination", nullable = false)
    private boolean destination = false; // 목적지 여부

    @Builder.Default
    @Column(name = "is_visited", nullable = false)
    private boolean visited = false; // 방문 완료 여부

    @Column
    private LocalDateTime visitedAt; // 실제 방문 체크 시각

    private int sequenceOrder; // 방문 순서

    private int recommendedDuration; // 추천 체류 시간 (분)

    private int distanceFromPrevious; // 이전 경유지로부터의 거리 (미터)

    private int travelTimeFromPrevious; // 이전 경유지로부터의 이동 시간 (분)

}
