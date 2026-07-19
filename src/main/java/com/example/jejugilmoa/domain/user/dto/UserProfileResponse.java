package com.example.jejugilmoa.domain.user.dto;

import java.time.Instant;

public record UserProfileResponse(
        String nickname,
        String profileImageUrl,
        String bio,
        long completedTripCount,
        long favoriteCount,
        long badgeCount,
        String email,
        Instant joinedAt
) {}
