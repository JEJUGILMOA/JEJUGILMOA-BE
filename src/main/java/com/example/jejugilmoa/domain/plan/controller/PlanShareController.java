package com.example.jejugilmoa.domain.plan.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.controller.docs.PlanShareControllerDocs;
import com.example.jejugilmoa.domain.plan.dto.ShareLinkResponse;
import com.example.jejugilmoa.domain.plan.dto.SharedPlanResponse;
import com.example.jejugilmoa.domain.plan.service.PlanShareService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "여행 계획 공유", description = "여행 계획 공유 링크 생성 및 공개 조회")
@RestController
@RequiredArgsConstructor
public class PlanShareController implements PlanShareControllerDocs {

    private final PlanShareService planShareService;

    @PostMapping("/api/plans/{planId}/share-link")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShareLinkResponse> createShareLink(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.CREATED,
                planShareService.createShareLink(planId, principal.userId()));
    }

    @GetMapping("/api/shared/plans/{shareToken}")
    public ApiResponse<SharedPlanResponse> getSharedPlan(@PathVariable String shareToken) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                planShareService.getSharedPlan(shareToken));
    }
}
