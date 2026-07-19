package com.example.jejugilmoa.domain.auth.dto;

import com.example.jejugilmoa.domain.user.enums.Role;

public record DevAuthResponse(
    Long userId,
    String email,
    String nickname,
    Role role,
    boolean newUser,
    String accessToken
) {
}
