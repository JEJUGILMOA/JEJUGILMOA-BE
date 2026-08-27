package com.example.jejugilmoa.domain.user.entity;

import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "`user`",
        indexes = {
                @Index(name = "idx_user_provider_external_id", columnList = "external_provider,external_id"),
                @Index(name = "idx_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_provider_external_id",
                        columnNames = {"external_provider", "external_id"})
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

    @Column(length = 255)
    private String externalId;  // 외부 계정 ID

    @Column(length = 50, nullable = false)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(length = 255)
    private String email; // 소셜 로그인 후 추출 (마이페이지 조회 시 사용)

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

    @Column(name = "anonymized_at")
    private LocalDateTime anonymizedAt;  // 탈퇴 30일 경과 후 개인정보 익명화 시각

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateProfile(String nickname, String profileImageUrl, String bio) {
        if (nickname != null) this.nickname = nickname;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
        if (bio != null) this.bio = bio;
    }

    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    // 탈퇴 후 30일이 지난 계정을 유령계정으로 전환한다. 다른 도메인의 FK가 user_id를
    // 참조하므로 행 자체는 남기고 개인정보만 제거한다. provider/externalId를 비우면
    // 같은 소셜 계정으로 재가입 시 새 계정으로 취급된다.
    public void anonymize() {
        this.externalProvider = null;
        this.externalId = null;
        this.nickname = "탈퇴한 사용자";
        this.profileImageUrl = null;
        this.email = null;
        this.bio = null;
        this.anonymizedAt = LocalDateTime.now();
    }
}
