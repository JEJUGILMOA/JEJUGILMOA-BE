package com.example.jejugilmoa.domain.plan.entity;

import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shared_plan", indexes = {
        @Index(name = "idx_shared_plan_share_token_active", columnList = "share_token,active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_shared_plan_travel_plan", columnNames = "travel_plan_id"),
        @UniqueConstraint(name = "uk_shared_plan_share_token", columnNames = "share_token")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TravelSharedPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_plan_id", nullable = false)
    private TravelPlan travelPlan;

    @Column(name = "share_token", nullable = false, length = 36)
    private String shareToken;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean active;

    public static TravelSharedPlan issue(TravelPlan travelPlan, String shareToken, Instant expiresAt) {
        return TravelSharedPlan.builder()
                .travelPlan(travelPlan)
                .shareToken(shareToken)
                .expiresAt(expiresAt)
                .active(true)
                .build();
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isAvailable(Instant now) {
        return active && !isExpired(now);
    }

    public void reissue(String shareToken, Instant expiresAt) {
        this.shareToken = shareToken;
        this.expiresAt = expiresAt;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
