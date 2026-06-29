package com.example.jejugilmoa.domain.plan.entity;

import com.example.jejugilmoa.domain.place.entity.Category;
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
                        name = "uk_plan_preference_category",
                        columnNames = {"plan_id", "category_id"}
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;  // 이번 여행에서 선택한 선호 유형 (전역 UserPreference와 별개)
}
