package com.example.jejugilmoa.domain.plan.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.controller.docs.TripControllerDocs;
import com.example.jejugilmoa.domain.plan.dto.TripCompleteResponse;
import com.example.jejugilmoa.domain.plan.dto.TripResponse;
import com.example.jejugilmoa.domain.plan.dto.TripStartRequest;
import com.example.jejugilmoa.domain.plan.dto.VisitCheckRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointAddRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
import com.example.jejugilmoa.domain.plan.service.TripService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "여행 진행", description = "여행 시작 및 진행 중 활동(방문 인증 등)")
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController implements TripControllerDocs {

    private final TripService tripService;

    @PostMapping
    public ApiResponse<TripResponse> start(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TripStartRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.CREATED,
                tripService.start(principal.userId(), request));
    }

    @GetMapping("/current")
    public ApiResponse<TripResponse> getCurrentTrip(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                tripService.getCurrentTrip(principal.userId()));
    }

    @PostMapping("/{tripId}/visits")
    public ApiResponse<List<WaypointResponse>> checkVisit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId,
            @Valid @RequestBody VisitCheckRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.CREATED,
                tripService.checkVisit(tripId, principal.userId(), request));
    }

    @PostMapping("/{tripId}/waypoints")
    public ApiResponse<List<WaypointResponse>> addWaypoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId,
            @Valid @RequestBody WaypointAddRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.CREATED,
                tripService.addWaypoint(tripId, principal.userId(), request));
    }

    @DeleteMapping("/{tripId}/waypoints/{waypointId}")
    public ApiResponse<List<WaypointResponse>> removeWaypoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId,
            @PathVariable Long waypointId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                tripService.removeWaypoint(tripId, principal.userId(), waypointId));
    }

    @PostMapping("/{tripId}/complete")
    public ApiResponse<TripCompleteResponse> complete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                tripService.complete(tripId, principal.userId()));
    }
}
