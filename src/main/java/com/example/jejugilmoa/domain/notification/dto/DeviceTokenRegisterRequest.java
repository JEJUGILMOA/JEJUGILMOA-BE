package com.example.jejugilmoa.domain.notification.dto;

import com.example.jejugilmoa.domain.notification.enums.FcmPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceTokenRegisterRequest(
        @NotBlank String token,
        @NotNull FcmPlatform platform,
        @NotBlank String deviceId
) {}
