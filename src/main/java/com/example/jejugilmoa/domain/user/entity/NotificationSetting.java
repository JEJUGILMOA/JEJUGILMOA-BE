package com.example.jejugilmoa.domain.user.entity;

import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;
import jakarta.persistence.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "notification_setting")
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    private Boolean notifyPlanStart = true;

    @Builder.Default
    private Boolean notifyRecordWriting = true;

    @Builder.Default
    private Boolean notifyBadgeAcquired = true;

    @Builder.Default
    private Boolean notifyNextPlace = true;

    @Builder.Default
    private Boolean notifyPlaceArrival = true;

    @Builder.Default
    private Boolean locationPermission = false;
}
