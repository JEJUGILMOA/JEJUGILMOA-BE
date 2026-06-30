package com.example.jejugilmoa.domain.user.entity;

import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "`user`", indexes = {
        @Index(name = "idx_external_id", columnList = "external_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String externalProvider;  // kakao, naver, google

    @Column(length = 255, unique = true)
    private String externalId;  // 외부 계정 ID

    @Column(length = 50, nullable = false)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;  // 자기소개

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;  // USER, ADMIN

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private UserPreference userPreference;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private NotificationSetting notificationSetting;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<UserBadge> userBadges = new ArrayList<>();

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<UserRestriction> restrictions = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;  // 소프트 삭제
}
