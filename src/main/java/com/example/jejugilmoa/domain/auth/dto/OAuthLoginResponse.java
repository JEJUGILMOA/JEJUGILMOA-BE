package com.example.jejugilmoa.domain.auth.dto;

import com.example.jejugilmoa.domain.user.enums.Role;

public record OAuthLoginResponse(
    Long userId,
    String nickname,
    String profileImageUrl,
    Role role,
    boolean newUser
) {
}
