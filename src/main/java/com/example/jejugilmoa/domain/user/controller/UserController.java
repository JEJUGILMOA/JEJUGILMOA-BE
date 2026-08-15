package com.example.jejugilmoa.domain.user.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.notification.dto.DeviceTokenRegisterRequest;
import com.example.jejugilmoa.domain.notification.service.DeviceTokenService;
import com.example.jejugilmoa.domain.user.controller.docs.UserControllerDocs;
import com.example.jejugilmoa.domain.user.dto.TravelPreferenceResponse;
import com.example.jejugilmoa.domain.user.dto.TravelPreferenceUpdateRequest;
import com.example.jejugilmoa.domain.user.dto.UserProfileResponse;
import com.example.jejugilmoa.domain.user.dto.UserSettingsResponse;
import com.example.jejugilmoa.domain.user.dto.UserSettingsUpdateRequest;
import com.example.jejugilmoa.domain.user.dto.UserUpdateRequest;
import com.example.jejugilmoa.domain.user.service.UserService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "유저", description = "유저 관련 API")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserService userService;
    private final DeviceTokenService deviceTokenService;

    @Override
    @Operation(summary = "내 프로필 조회", description = "내 프로필을 조회합니다.")
    @GetMapping
    public ApiResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, userService.getMyProfile(principal.userId()));
    }

    @Override
    @Operation(summary = "내 프로필 수정", description = "프로필 내 닉네임, 한줄 소개, 프로필 이미지를 변경합니다.")
    @PatchMapping
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, userService.updateMyProfile(principal.userId(), request));
    }

    @Override
    @Operation(summary = "여행 선호도 설정", description = "여행 선호도를 설정합니다.")
    @PatchMapping("/preferences")
    public ApiResponse<TravelPreferenceResponse> updateTravelPreferences(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody TravelPreferenceUpdateRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, userService.updateTravelPreferences(principal.userId(), request));
    }

    @Override
    @Operation(summary = "디바이스 토큰 등록", description = "FCM 푸시 알림용 디바이스 토큰을 등록하거나 갱신합니다.")
    @PostMapping("/device-token")
    public ApiResponse<Void> registerDeviceToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeviceTokenRegisterRequest request
    ) {
        deviceTokenService.registerOrUpdateToken(principal.userId(), request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @Override
    @Operation(summary = "디바이스 토큰 삭제", description = "로그아웃 시 FCM 디바이스 토큰을 삭제합니다.")
    @DeleteMapping("/device-token/{deviceId}")
    public ApiResponse<Void> deleteDeviceToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String deviceId
    ) {
        deviceTokenService.deleteToken(principal.userId(), deviceId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @Override
    @Operation(summary = "설정 조회", description = "알림 및 위치 권한 설정을 조회합니다.")
    @GetMapping("/settings")
    public ApiResponse<UserSettingsResponse> getSettings(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, userService.getSettings(principal.userId()));
    }

    @Override
    @Operation(summary = "설정 수정", description = "알림 및 위치 권한 설정을 수정합니다.")
    @PatchMapping("/settings")
    public ApiResponse<UserSettingsResponse> updateSettings(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody UserSettingsUpdateRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, userService.updateSettings(principal.userId(), request));
    }
}
