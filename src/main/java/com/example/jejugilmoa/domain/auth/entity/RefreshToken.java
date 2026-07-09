package com.example.jejugilmoa.domain.auth.entity;

import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "refresh_token",
        indexes = {
                @Index(name = "idx_refresh_token_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refresh_token_token_id", columnNames = "token_id")
        })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_id", nullable = false, length = 36)
    private String tokenId;  // 리프레시 토큰 JWT의 jti 클레임. 회전/재사용 탐지의 식별자로 사용

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;

    // 동시에 같은 리프레시 토큰으로 재발급이 경합할 때 둘 다 성공하는 걸 막기 위한 낙관적 락.
    // 먼저 커밋한 요청만 성공하고, 뒤늦은 요청은 OptimisticLockingFailureException으로 걸러진다.
    @Version
    private Long version;

    public void revoke() {
        this.revoked = true;
    }
}
