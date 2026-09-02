package com.example.jejugilmoa.domain.badge.entity;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;

/**
 * 코스 완료형(COURSE) 뱃지 조건의 경유지 한 곳. stepOrder 순서대로 전부 방문 인증해야 지급된다.
 */
@Entity
@Table(
        name = "badge_condition_course_stop",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_course_stop_condition_order", columnNames = {"badge_condition_id", "step_order"})
        },
        indexes = {
                @Index(name = "idx_course_stop_place", columnList = "place_id")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BadgeConditionCourseStop extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_condition_id", nullable = false)
    private BadgeCondition badgeCondition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;
}
