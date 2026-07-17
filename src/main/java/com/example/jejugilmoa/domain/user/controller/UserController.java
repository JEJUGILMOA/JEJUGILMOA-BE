package com.example.jejugilmoa.domain.user.controller;

import com.example.jejugilmoa.domain.user.controller.docs.UserControllerDocs;
import com.example.jejugilmoa.domain.user.dto.UserProfileResponse;
import com.example.jejugilmoa.domain.user.dto.UserUpdateRequest;
import com.example.jejugilmoa.domain.user.service.UserService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "유저", description = "유저 관련 API")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserService userService;

    @Override
    @Operation(summary = "내 프로필 조회", description = "내 프로필을 조회합니다.")
    @GetMapping
    public ApiResponse<UserProfileResponse> getMyProfile(
            // TODO: 인증 구현 후 SecurityContext에서 userId 추출
            @RequestParam Long userId
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, userService.getMyProfile(userId));
    }

    @Override
    @Operation(summary = "내 프로필 수정", description = "프로필 내 닉네임, 한줄 소개, 프로필 이미지를 변경합니다.")
    @PatchMapping
    public ApiResponse<UserProfileResponse> updateMyProfile(
            // TODO: 인증 구현 후 SecurityContext에서 userId 추출
            @RequestParam Long userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, userService.updateMyProfile(userId, request));
    }
}
