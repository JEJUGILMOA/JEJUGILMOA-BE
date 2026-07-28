package com.example.jejugilmoa.domain.plan.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.controller.docs.TravelPlanControllerDocs;
import com.example.jejugilmoa.domain.plan.dto.*;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.service.RecommendationService;
import com.example.jejugilmoa.domain.plan.service.TravelPlanService;
import com.example.jejugilmoa.domain.plan.service.WaypointService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Tag(name = "여행 계획", description = "여행 계획 생성 및 관리")
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class TravelPlanController implements TravelPlanControllerDocs {

    private final TravelPlanService travelPlanService;
    private final RecommendationService recommendationService;
    private final WaypointService waypointService;

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

    @GetMapping("/{planId}/recommendations")
    public ApiResponse<RecommendationResponse> getRecommendations(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                recommendationService.recommend(planId, principal.userId(), Collections.emptyList()));
    }

    @PostMapping("/{planId}/recommendations")
    public ApiResponse<RecommendationResponse> reRecommend(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @Valid @RequestBody RecommendationSkipRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                recommendationService.recommend(planId, principal.userId(), request.excludedPlaceIds()));
    }

    @PostMapping("/{planId}/waypoints")
    public ApiResponse<List<WaypointResponse>> addWaypoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @Valid @RequestBody WaypointAddRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.CREATED,
                waypointService.addWaypoint(planId, principal.userId(), request));
    }

    @DeleteMapping("/{planId}/waypoints/{waypointId}")
    public ApiResponse<List<WaypointResponse>> removeWaypoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @PathVariable Long waypointId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                waypointService.removeWaypoint(planId, principal.userId(), waypointId));
    }
}
