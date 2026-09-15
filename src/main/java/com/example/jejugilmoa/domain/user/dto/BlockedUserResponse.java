package com.example.jejugilmoa.domain.user.dto;

public record BlockedUserResponse(
        Long userId,
        String nickname,
        String profileImageUrl
) {}
