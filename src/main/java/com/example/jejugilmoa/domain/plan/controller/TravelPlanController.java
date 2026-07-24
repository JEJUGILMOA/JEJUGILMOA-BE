package com.example.jejugilmoa.domain.plan.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.controller.docs.TravelPlanControllerDocs;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanCreateRequest;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanCreateResponse;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanListResponse;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.service.TravelPlanService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "여행 계획", description = "여행 계획 생성 및 관리")
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class TravelPlanController implements TravelPlanControllerDocs {

    private final TravelPlanService travelPlanService;

    @GetMapping
    public ApiResponse<List<TravelPlanListResponse>> getMyPlans(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) TravelPlanStatus status) {

        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                travelPlanService.getMyPlans(principal.userId(), status));
    }

    @PostMapping
    public ApiResponse<TravelPlanCreateResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TravelPlanCreateRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.CREATED,
                travelPlanService.create(principal.userId(), request));
    }
}
