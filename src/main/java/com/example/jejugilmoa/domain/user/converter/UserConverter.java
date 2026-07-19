package com.example.jejugilmoa.domain.user.converter;

import com.example.jejugilmoa.domain.user.dto.UserProfileResponse;
import com.example.jejugilmoa.domain.user.entity.User;

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
}
