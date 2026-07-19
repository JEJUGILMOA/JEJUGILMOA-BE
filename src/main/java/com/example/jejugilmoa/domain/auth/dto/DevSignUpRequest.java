package com.example.jejugilmoa.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevSignUpRequest(
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다.")
    String email,

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 20, message = "닉네임은 20자를 초과할 수 없습니다.")
    String nickname
) {
}
