package com.example.jejugilmoa.domain.plan.entity;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "day_departure", indexes = {
        @Index(name = "idx_day_departure_plan", columnList = "plan_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_day_departure_plan_date", columnNames = {"plan_id", "visit_date"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DayDeparture extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TravelPlan travelPlan;

    @Column(nullable = false)
    private LocalDate visitDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(length = 200)
    private String locationName;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
}
