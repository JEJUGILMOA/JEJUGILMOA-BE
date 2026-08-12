package com.example.jejugilmoa.domain.plan.entity;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "travel_course", indexes = {
        @Index(name = "idx_course_plan", columnList = "plan_id"),
        @Index(name = "idx_course_plan_date", columnList = "plan_id,visit_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_plan_date_sequence", columnNames = {"plan_id", "visit_date", "sequence_order"}),
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

    @Column(nullable = false)
    private LocalDate visitDate;

    @Builder.Default
    @Column(name = "is_start", nullable = false)
    private boolean start = false;

    @Builder.Default
    @Column(name = "is_destination", nullable = false)
    private boolean destination = false;

    @Builder.Default
    @Column(name = "is_visited", nullable = false)
    private boolean visited = false;

    @Column
    private LocalDateTime visitedAt;

    private int sequenceOrder;

    private int recommendedDuration;

    private int distanceFromPrevious;

    private int travelTimeFromPrevious;

    public void checkVisit(LocalDateTime visitedAt) {
        this.visited = true;
        this.visitedAt = visitedAt;
    }

    public void updateStartDestination(boolean start, boolean destination) {
        this.start = start;
        this.destination = destination;
    }

}
