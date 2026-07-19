package com.example.jejugilmoa.domain.user.service;

import com.example.jejugilmoa.domain.plan.repository.FavoriteRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.user.converter.UserConverter;
import com.example.jejugilmoa.domain.user.dto.UserProfileResponse;
import com.example.jejugilmoa.domain.user.dto.UserUpdateRequest;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
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

    private User getUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
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
