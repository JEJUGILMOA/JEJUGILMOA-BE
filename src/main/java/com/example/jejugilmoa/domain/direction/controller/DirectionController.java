package com.example.jejugilmoa.domain.direction.controller;

import com.example.jejugilmoa.domain.direction.controller.docs.DirectionControllerDocs;
import com.example.jejugilmoa.domain.direction.dto.DirectionResponse;
import com.example.jejugilmoa.domain.direction.service.DirectionService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "길찾기", description = "네이버 지도 기반 경로 조회")
@RestController
@RequestMapping("/api/directions")
@RequiredArgsConstructor
public class DirectionController implements DirectionControllerDocs {

    private final DirectionService directionService;

    @GetMapping("/driving")
    public ApiResponse<DirectionResponse> getDriving(
            @RequestParam double startLat,
            @RequestParam double startLng,
            @RequestParam double goalLat,
            @RequestParam double goalLng,
            @RequestParam(required = false) String waypoints,
            @RequestParam(defaultValue = "traoptimal") String option) {
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND,
            directionService.getDriving(startLat, startLng, goalLat, goalLng, waypoints, option));
    }
}
