package com.example.jejugilmoa.domain.user.service;

import com.example.jejugilmoa.domain.auth.repository.RefreshTokenRepository;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TravelRecordRepository travelRecordRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

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

    @Transactional
    public void withdraw(Long userId) {
        User user = getUserForUpdate(userId);
        user.withdraw(LocalDateTime.now(clock));
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional
    public void restore(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNotNullForUpdate(userId)
            .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_WITHDRAWN));

        if (user.getAnonymizedAt() != null) {
            throw new GeneralException(UserErrorCode.USER_PERMANENTLY_WITHDRAWN);
        }

        user.restore();
    }

    // 복구 경로(restoreByExternalAccount, UserService.restore)와 같은 행 잠금으로 직렬화한다.
    // 잠금 대기 중 복구가 커밋되면 deletedAt이 NULL이 되어 잠금 후 재평가에서 제외되므로,
    // 복구된 계정을 익명화가 덮어쓰는 일이 없다.
    @Transactional
    public int anonymizeExpiredWithdrawals(LocalDateTime cutoff) {
        List<User> expiredUsers = userRepository.findByDeletedAtBeforeAndAnonymizedAtIsNullForUpdate(cutoff);
        LocalDateTime now = LocalDateTime.now(clock);
        expiredUsers.forEach(user -> user.anonymize(now));
        return expiredUsers.size();
    }

    // 소셜 로그인에서 같은 provider/externalId로 남아있는 탈퇴 계정을 잠그고 복구한다.
    // 익명화 스케줄러와 같은 행 잠금으로 직렬화되며, 잠금 대기 중 익명화가 커밋되면
    // provider/externalId가 비워지고, 다른 로그인이 먼저 복구를 커밋하면 deletedAt이 NULL이
    // 되어 어느 쪽이든 조회에서 제외된다 — 호출부는 빈 Optional을 받아 신규 가입으로 이어진다.
    @Transactional
    public Optional<User> restoreByExternalAccount(String externalProvider, String externalId) {
        return userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNotNullForUpdate(externalProvider, externalId)
            .map(user -> {
                user.restore();
                return user;
            });
    }

    private UserPreference getOrCreatePreference(Long userId) {
        return userPreferenceRepository.findByUserIdAndUserDeletedAtIsNull(userId)
            .orElseGet(() -> {
                User user = getUserForUpdate(userId);
                return userPreferenceRepository.findByUserIdAndUserDeletedAtIsNull(userId)
                    .orElseGet(() -> userPreferenceRepository.save(UserPreference.createDefault(user)));
            });
    }

    private NotificationSetting getOrCreateSetting(Long userId) {
        return notificationSettingRepository.findByUserIdAndUserDeletedAtIsNull(userId)
            .orElseGet(() -> {
                User user = getUserForUpdate(userId);
                return notificationSettingRepository.findByUserIdAndUserDeletedAtIsNull(userId)
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
