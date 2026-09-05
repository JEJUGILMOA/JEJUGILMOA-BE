package com.example.jejugilmoa.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppleLoginRequest(
    @NotBlank(message = "Apple 인증 토큰은 필수입니다.")
    @Size(max = 16384, message = "Apple 인증 토큰은 16384자를 초과할 수 없습니다.")
    String identityToken,

    @NotBlank(message = "rawNonce는 필수입니다.")
    @Size(min = 32, max = 256, message = "rawNonce는 32~256자여야 합니다.")
    String rawNonce
) {
}
