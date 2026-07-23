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
    @JoinColumn(name = "user_id", nullable = false, unique = true)
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
    private Boolean notifyMarketing = true;

    @Builder.Default
    private Boolean locationPermission = false;

    public static NotificationSetting createDefault(User user) {
        return NotificationSetting.builder()
                .user(user)
                .build();
    }

    public void updateSettings(
            Boolean notifyPlanStart,
            Boolean notifyRecordWriting,
            Boolean notifyBadgeAcquired,
            Boolean notifyNextPlace,
            Boolean notifyPlaceArrival,
            Boolean notifyMarketing,
            Boolean locationPermission
    ) {
        if (notifyPlanStart != null) this.notifyPlanStart = notifyPlanStart;
        if (notifyRecordWriting != null) this.notifyRecordWriting = notifyRecordWriting;
        if (notifyBadgeAcquired != null) this.notifyBadgeAcquired = notifyBadgeAcquired;
        if (notifyNextPlace != null) this.notifyNextPlace = notifyNextPlace;
        if (notifyPlaceArrival != null) this.notifyPlaceArrival = notifyPlaceArrival;
        if (notifyMarketing != null) this.notifyMarketing = notifyMarketing;
        if (locationPermission != null) this.locationPermission = locationPermission;
    }
}
