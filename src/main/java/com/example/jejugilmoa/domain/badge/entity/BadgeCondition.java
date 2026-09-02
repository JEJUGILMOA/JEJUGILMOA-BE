package com.example.jejugilmoa.domain.badge.entity;

import com.example.jejugilmoa.domain.badge.enums.BadgeConditionType;
import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "badge_condition",
        indexes = {
                @Index(name = "idx_badge_condition_badge", columnList = "badge_id")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BadgeCondition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BadgeConditionType conditionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 100)
    private String region;  // 지역명

    @Builder.Default
    @Column(nullable = false)
    private Integer visitCount = 1;  // 필요 방문 횟수

    // 시간조건형(일출/일몰/별빛): 인증 시각(visitedAt)의 시각이 이 구간에 들어와야 집계된다.
    // timeStart > timeEnd이면 자정을 넘는 구간(예: 20:00~05:00)으로 해석한다.
    @Column
    private LocalTime timeStart;

    @Column
    private LocalTime timeEnd;

    // 코스 완료형(COURSE): 순서대로 전부 인증해야 하는 경유지 목록
    @OneToMany(mappedBy = "badgeCondition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<BadgeConditionCourseStop> courseStops = new ArrayList<>();
}
