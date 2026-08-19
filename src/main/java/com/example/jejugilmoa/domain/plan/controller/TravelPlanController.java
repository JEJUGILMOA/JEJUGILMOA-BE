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
import org.springframework.http.HttpStatus;
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

    @GetMapping("/{planId}")
    public ApiResponse<TravelPlanDetailResponse> getPlanDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                travelPlanService.getPlanDetail(planId, principal.userId()));
    }

    @GetMapping
    public ApiResponse<List<TravelPlanListResponse>> getMyPlans(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) TravelPlanStatus status) {

        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                travelPlanService.getMyPlans(principal.userId(), status));
    }

    @DeleteMapping("/{planId}")
    public ApiResponse<Void> deletePlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId) {
        travelPlanService.deletePlan(planId, principal.userId());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TravelPlanDetailResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TravelPlanCreateRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.CREATED,
                travelPlanService.create(principal.userId(), request));
    }

    @PatchMapping("/{planId}/waypoints/{waypointId}/preferred")
    public ApiResponse<List<WaypointResponse>> togglePreferred(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @PathVariable Long waypointId,
            @RequestBody TogglePreferredRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                waypointService.togglePreferred(planId, principal.userId(), waypointId, request.isPreferred()));
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
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<WaypointResponse>> addWaypoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @Valid @RequestBody WaypointAddRequest request) {
        travelPlanService.assertPlanEditable(planId, principal.userId());
        return ApiResponse.onSuccess(
                GeneralSuccessCode.CREATED,
                waypointService.addWaypoint(planId, principal.userId(), request));
    }

    @DeleteMapping("/{planId}/waypoints/{waypointId}")
    public ApiResponse<List<WaypointResponse>> removeWaypoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @PathVariable Long waypointId) {
        travelPlanService.assertPlanEditable(planId, principal.userId());
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                waypointService.removeWaypoint(planId, principal.userId(), waypointId));
    }

    @PatchMapping("/{planId}/waypoints/reorder")
    public ApiResponse<List<WaypointResponse>> reorderWaypoints(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @Valid @RequestBody WaypointReorderRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                waypointService.reorderWaypoints(planId, principal.userId(), request));
    }

    @PostMapping("/{planId}/recommend/places")
    public ApiResponse<NearbyPlaceRecommendResponse> recommendNearbyPlaces(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long planId,
        @Valid @RequestBody NearbyPlaceRecommendRequest request) {
        return ApiResponse.onSuccess(
            GeneralSuccessCode.REQUEST_OK,
            recommendationService.recommendNearby(planId, principal.userId(), request));
    }

    @PatchMapping("/{planId}")
    public ApiResponse<TravelPlanDetailResponse> updatePlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @Valid @RequestBody TravelPlanUpdateRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                travelPlanService.updatePlan(planId, principal.userId(), request));
    }

    @PatchMapping("/{planId}/budget")
    public ApiResponse<BudgetUpdateResponse> updateBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @Valid @RequestBody BudgetUpdateRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                travelPlanService.updateBudget(planId, principal.userId(), request));
    }
}
