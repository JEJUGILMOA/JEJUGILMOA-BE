package com.example.jejugilmoa.domain.user.service;

import com.example.jejugilmoa.domain.plan.repository.FavoriteRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.user.converter.UserConverter;
import com.example.jejugilmoa.domain.user.dto.TravelPreferenceResponse;
import com.example.jejugilmoa.domain.user.dto.TravelPreferenceUpdateRequest;
import com.example.jejugilmoa.domain.user.dto.UserProfileResponse;
import com.example.jejugilmoa.domain.user.dto.UserSettingsResponse;
import com.example.jejugilmoa.domain.user.dto.UserSettingsUpdateRequest;
import com.example.jejugilmoa.domain.user.dto.UserUpdateRequest;
import com.example.jejugilmoa.domain.user.entity.NotificationSetting;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.entity.UserPreference;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.repository.NotificationSettingRepository;
import com.example.jejugilmoa.domain.user.repository.UserPreferenceRepository;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TravelRecordRepository travelRecordRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    public UserProfileResponse getMyProfile(Long userId) {
        User user = getUser(userId);

        return createProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UserUpdateRequest request) {
        User user = getUser(userId);

        user.updateProfile(
            request.nickname(),
            request.profileImageUrl(),
            request.bio()
        );

        return createProfileResponse(user);
    }

    @Transactional
    public TravelPreferenceResponse updateTravelPreferences(Long userId, TravelPreferenceUpdateRequest request) {
        UserPreference preference = getOrCreatePreference(userId);

        preference.updatePreference(
            request.nature(),
            request.food(),
            request.cafe(),
            request.traditionMarket(),
            request.history(),
            request.experience(),
            request.travelStyle()
        );

        return UserConverter.toPreferenceResponse(preference);
    }

    @Transactional
    public UserSettingsResponse getSettings(Long userId) {
        NotificationSetting setting = getOrCreateSetting(userId);

        return UserConverter.toSettingsResponse(setting);
    }

    @Transactional
    public UserSettingsResponse updateSettings(Long userId, UserSettingsUpdateRequest request) {
        NotificationSetting setting = getOrCreateSetting(userId);

        setting.updateSettings(
            request.notifyPlanStart(),
            request.notifyRecordWriting(),
            request.notifyBadgeAcquired(),
            request.notifyNextPlace(),
            request.notifyPlaceArrival(),
            request.notifyMarketing(),
            request.locationPermission()
        );

        return UserConverter.toSettingsResponse(setting);
    }

    private UserPreference getOrCreatePreference(Long userId) {
        return userPreferenceRepository.findByUserId(userId)
            .orElseGet(() -> {
                User user = getUserForUpdate(userId);
                return userPreferenceRepository.findByUserId(userId)
                    .orElseGet(() -> userPreferenceRepository.save(UserPreference.createDefault(user)));
            });
    }

    private NotificationSetting getOrCreateSetting(Long userId) {
        return notificationSettingRepository.findByUserId(userId)
            .orElseGet(() -> {
                User user = getUserForUpdate(userId);
                return notificationSettingRepository.findByUserId(userId)
                    .orElseGet(() -> notificationSettingRepository.save(NotificationSetting.createDefault(user)));
            });
    }

    private User getUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    private User getUserForUpdate(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNullForUpdate(userId)
            .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    private UserProfileResponse createProfileResponse(User user) {
        Long userId = user.getId();

        long completedTripCount =
            travelRecordRepository.countByUserIdAndDeletedAtIsNull(userId);

        long favoriteCount =
            favoriteRepository.countByUserId(userId);

        long badgeCount =
            userRepository.countBadgesByUserId(userId);

        return UserConverter.toProfileResponse(
            user,
            completedTripCount,
            favoriteCount,
            badgeCount
        );
    }
}
