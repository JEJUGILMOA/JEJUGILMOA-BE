package com.example.jejugilmoa.domain.user.entity;

import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_block",
        indexes = {
                @Index(name = "idx_user_block_blocker", columnList = "blocker_id"),
                @Index(name = "idx_user_block_blocked", columnList = "blocked_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_block_pair", columnNames = {"blocker_id", "blocked_id"})
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserBlock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;
}
