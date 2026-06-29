package com.example.jejugilmoa.domain.user.entity;


import com.example.jejugilmoa.domain.badge.entity.Badge;
import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "user_badge",
        indexes = {
                @Index(name = "idx_user_badge_user", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_badge",
                        columnNames = {"user_id", "badge_id"}
                )
        }
)
public class UserBadge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "badge_id",
            nullable = false
    )
    private Badge badge;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime acquiredAt;
}
