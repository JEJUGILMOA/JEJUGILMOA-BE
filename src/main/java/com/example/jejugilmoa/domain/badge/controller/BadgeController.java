package com.example.jejugilmoa.domain.badge.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.badge.controller.docs.BadgeControllerDocs;
import com.example.jejugilmoa.domain.badge.dto.BadgeGroupResponse;
import com.example.jejugilmoa.domain.badge.service.BadgeService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "배지", description = "배지 조회 API")
@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController implements BadgeControllerDocs {

    private final BadgeService badgeService;

    @GetMapping("/me")
    public ApiResponse<List<BadgeGroupResponse>> getMyBadges(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                badgeService.getMyBadges(principal.userId()));
    }
}
