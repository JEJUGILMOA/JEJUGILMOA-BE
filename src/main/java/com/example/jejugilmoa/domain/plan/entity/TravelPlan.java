package com.example.jejugilmoa.domain.plan.entity;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.plan.enums.TravelCompanion;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.enums.TravelStyle;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_place_id")
    private Place departurePlace;  // 출발지 (nullable — 직접 입력 시 null)

    @Column(length = 200)
    private String departureLocationName;  // 출발지 텍스트 (지도 미선택 시 사용)

    @Column(precision = 10, scale = 8)
    private BigDecimal departureLatitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal departureLongitude;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TravelCompanion companion;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TravelStyle travelStyle = TravelStyle.RELAXED;  // 여행 페이스 — 계획 생성 시 기본값 RELAXED, 이후 수정 가능

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TravelPlanStatus status = TravelPlanStatus.DRAFT;  // DRAFT, IN_PROGRESS, COMPLETED

    @Column(name = "actual_started_at")
    private LocalDateTime actualStartedAt;  // 실제 여행 시작 시각 (여행 시작 API 호출 시각)

    @Column(name = "actual_completed_at")
    private LocalDateTime actualCompletedAt;  // 실제 여행 완료 시각 (여행 완료 API 호출 시각)

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

    public void start(LocalDateTime startedAt) {
        this.status = TravelPlanStatus.IN_PROGRESS;
        this.actualStartedAt = startedAt;
    }

    public void complete(LocalDateTime completedAt) {
        this.status = TravelPlanStatus.COMPLETED;
        this.actualCompletedAt = completedAt;
    }

    private Integer budgetTransportation;

    private Integer budgetAccommodation;

    private Integer budgetFood;

    private Integer budgetEtc;

    public void updateBudget(Integer transportation, Integer accommodation, Integer food, Integer etc) {
        this.budgetTransportation = transportation;
        this.budgetAccommodation = accommodation;
        this.budgetFood = food;
        this.budgetEtc = etc;
    }

    public void updatePlanInfo(String title, Place departure, String departureName) {
        this.title = title;
        this.departurePlace = departure;
        this.departureLocationName = departureName;
    }

}
