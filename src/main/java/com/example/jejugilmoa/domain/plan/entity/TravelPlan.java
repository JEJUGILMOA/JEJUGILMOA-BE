package com.example.jejugilmoa.domain.plan.entity;

import com.example.jejugilmoa.domain.plan.enums.TransportMode;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "travel_plan", indexes = {
        @Index(name = "idx_plan_user_status", columnList = "user_id,status"),
        @Index(name = "idx_plan_start_date", columnList = "start_date")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TravelPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(nullable = false, length = 200)
    private String title;  // 여행 제목

    @Column(nullable = false)
    private LocalDate startDate;  // 시작 날짜

    @Column(nullable = false)
    private LocalDate endDate;  // 종료 날짜

    @Builder.Default
    @Column(nullable = false)
    private boolean sameDay = false;  // 당일 여행 여부

    @Column
    private LocalTime startTime;  // 출발 시간

    @Column
    private LocalTime endTime;  // 종료 시간

    @Column(nullable = false)
    private int totalAvailableTime;  // 전체 여행 가능 시간 (분)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransportMode transportMode;  // 도보, 대중교통, 차량

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TravelPlanStatus status = TravelPlanStatus.DRAFT;  // DRAFT, IN_PROGRESS, COMPLETED

    @OneToMany(
            mappedBy = "travelPlan",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<TravelCourse> travelCourses = new ArrayList<>();

    @OneToMany(
            mappedBy = "travelPlan",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<TravelPlanPreference> preferredCategories = new ArrayList<>();  // 이번 여행의 선호 유형(여행 스타일)

}
