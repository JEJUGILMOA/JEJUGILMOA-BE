package com.example.jejugilmoa.domain.badge.entity;

import com.example.jejugilmoa.domain.badge.enums.BadgeGroup;
import com.example.jejugilmoa.domain.badge.enums.BadgeType;
import com.example.jejugilmoa.domain.user.entity.UserBadge;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "badge")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Badge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;  // 배지명

    @Column(columnDefinition = "TEXT")
    private String description;  // 설명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BadgeType badgeType; // PLACE, CATEGORY, REGION, HIDDEN

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BadgeGroup displayGroup; // 탐험/미식/소셜 - 배지함 화면 표시용 그룹

    @Column(length = 500)
    private String imageUrl;  // 배지 이미지 URL

    @OneToMany(mappedBy = "badge")
    private List<UserBadge> userBadges = new ArrayList<>();
}
