package com.example.jejugilmoa.domain.record.entity;

import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "travel_record", indexes = {
        @Index(name = "idx_record_user_visibility", columnList = "user_id,visibility"),
        @Index(name = "idx_record_plan", columnList = "plan_id"),
        @Index(name = "idx_record_created_at", columnList = "created_at")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TravelRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "plan_id"
    )
    private TravelPlan travelPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(length = 50, nullable = false)
    private String title;  // 기록 제목

    @Column(columnDefinition = "TEXT")
    private String description;  // 한줄 소개

    @Column
    private LocalDate actualStartDate;  // 실제 시작 날짜

    @Column
    private LocalDate actualEndDate;  // 실제 종료 날짜

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility = Visibility.PRIVATE;  // 공개 여부

    @Builder.Default
    @Column(nullable = false)
    private int likeCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int dislikeCount = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;  // 소프트 삭제

    @OneToMany(
            mappedBy = "travelRecord",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<TravelRecordImage> images = new ArrayList<>();

    public void updateContent(String title, String description, Visibility visibility) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (visibility != null) {
            this.visibility = visibility;
        }
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
