package com.example.jejugilmoa.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OAuthLoginRequest(
    @NotBlank(message = "인가코드는 필수입니다.")
    String authorizationCode,

    @Size(max = 500, message = "리다이렉트 URI는 500자를 초과할 수 없습니다.")
    String redirectUri,

    @Size(max = 255, message = "state 값은 255자를 초과할 수 없습니다.")
    String state
) {
}
