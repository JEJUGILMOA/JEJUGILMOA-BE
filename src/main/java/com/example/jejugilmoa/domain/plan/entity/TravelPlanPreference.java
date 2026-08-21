package com.example.jejugilmoa.domain.plan.entity;

import com.example.jejugilmoa.domain.plan.enums.TravelTheme;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "travel_plan_preference",
        indexes = {
                @Index(name = "idx_plan_preference_plan", columnList = "plan_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_plan_preference_theme",
                        columnNames = {"plan_id", "theme"}
                )
        }
)
public class TravelPlanPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private TravelPlan travelPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false, length = 30)
    private TravelTheme theme;
}
