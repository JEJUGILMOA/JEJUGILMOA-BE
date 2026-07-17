package com.example.jejugilmoa.domain.user.dto;


public record UserUpdateResponse(
    String nickname,
    String profileImageUrl,
    String bio,
    String email
) {}
