package com.example.jejugilmoa.domain.user.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.user.controller.docs.UserBlockControllerDocs;
import com.example.jejugilmoa.domain.user.dto.BlockedUserResponse;
import com.example.jejugilmoa.domain.user.service.UserBlockService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사용자 차단", description = "사용자 차단 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserBlockController implements UserBlockControllerDocs {

    private final UserBlockService userBlockService;

    @Override
    @PostMapping("/{targetUserId}/block")
    public ApiResponse<Void> blockUser(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userBlockService.blockUser(principal.userId(), targetUserId);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }

    @Override
    @DeleteMapping("/{targetUserId}/block")
    public ApiResponse<Void> unblockUser(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userBlockService.unblockUser(principal.userId(), targetUserId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @Override
    @GetMapping("/blocks")
    public ApiResponse<List<BlockedUserResponse>> getBlockedUsers(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK,
                userBlockService.getBlockedUsers(principal.userId()));
    }
}
