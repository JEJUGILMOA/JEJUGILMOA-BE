package com.example.jejugilmoa.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @Schema(description = "닉네임 (null이면 수정 안 함, 최대 50자)", example = "제주여행자")
        @Size(max = 50, message = "닉네임은 50자를 초과할 수 없습니다.")
        String nickname,

        @Schema(description = "프로필 이미지 URL (null이면 수정 안 함)", example = "https://example.com/profile.jpg")
        @Size(max = 500, message = "프로필 이미지 URL은 500자를 초과할 수 없습니다.")
        String profileImageUrl,

        @Schema(description = "자기소개 (null이면 수정 안 함)", example = "제주도를 사랑하는 여행자입니다.")
        String bio

) {}
