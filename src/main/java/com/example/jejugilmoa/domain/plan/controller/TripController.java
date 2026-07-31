package com.example.jejugilmoa.domain.plan.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.controller.docs.TripControllerDocs;
import com.example.jejugilmoa.domain.plan.dto.TripResponse;
import com.example.jejugilmoa.domain.plan.dto.TripStartRequest;
import com.example.jejugilmoa.domain.plan.dto.VisitCheckRequest;
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

@Tag(name = "여행 진행", description = "여행 시작 및 진행 중 활동(방문 체크 등)")
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

    @PostMapping("/{tripId}/visits")
    public ApiResponse<List<WaypointResponse>> checkVisit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId,
            @Valid @RequestBody VisitCheckRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.CREATED,
                tripService.checkVisit(tripId, principal.userId(), request));
    }
}
