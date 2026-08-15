package com.example.jejugilmoa.domain.notification.entity;

import com.example.jejugilmoa.domain.notification.enums.FcmPlatform;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "device_token",
        indexes = {
                @Index(name = "idx_device_token_user", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_device_token_user_device", columnNames = {"user_id", "device_id"}),
                @UniqueConstraint(name = "uk_device_token_token", columnNames = {"token"})
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FcmPlatform platform;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    public void updateToken(String newToken) {
        this.token = newToken;
        this.lastUsedAt = Instant.now();
    }
}
