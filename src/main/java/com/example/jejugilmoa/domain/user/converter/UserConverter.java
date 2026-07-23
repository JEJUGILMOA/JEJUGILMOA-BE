package com.example.jejugilmoa.domain.user.converter;

import com.example.jejugilmoa.domain.user.dto.TravelPreferenceResponse;
import com.example.jejugilmoa.domain.user.dto.UserProfileResponse;
import com.example.jejugilmoa.domain.user.dto.UserSettingsResponse;
import com.example.jejugilmoa.domain.user.entity.NotificationSetting;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.entity.UserPreference;

public final class UserConverter {

    private UserConverter() {}

    public static UserProfileResponse toProfileResponse(
            User user,
            long completedTripCount,
            long favoriteCount,
            long badgeCount
    ) {
        return new UserProfileResponse(
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getBio(),
                completedTripCount,
                favoriteCount,
                badgeCount,
                user.getEmail(),
                user.getCreatedAt()
        );
    }

    public static TravelPreferenceResponse toPreferenceResponse(UserPreference preference) {
        return new TravelPreferenceResponse(
                preference.getNature(),
                preference.getFood(),
                preference.getCafe(),
                preference.getTraditionMarket(),
                preference.getHistory(),
                preference.getExperience(),
                preference.getTravelStyle()
        );
    }

    public static UserSettingsResponse toSettingsResponse(NotificationSetting setting) {
        return new UserSettingsResponse(
                setting.getNotifyPlanStart(),
                setting.getNotifyRecordWriting(),
                setting.getNotifyBadgeAcquired(),
                setting.getNotifyNextPlace(),
                setting.getNotifyPlaceArrival(),
                setting.getNotifyMarketing(),
                setting.getLocationPermission()
        );
    }
}
