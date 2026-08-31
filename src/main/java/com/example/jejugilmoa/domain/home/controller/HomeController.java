package com.example.jejugilmoa.domain.home.controller;

import com.example.jejugilmoa.domain.home.dto.HomeCourseResponse;
import com.example.jejugilmoa.domain.home.dto.HomePlaceResponse;
import com.example.jejugilmoa.domain.home.service.HomeService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "홈", description = "홈 화면 API")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "오늘의 관광지 추천", description = "홈 화면 오늘의 관광지 추천 목록 (최대 5개). TODAY_PICK → TRAVELER_PICK → 인기순.")
    @GetMapping("/places")
    public ApiResponse<List<HomePlaceResponse>> getHomePlaces() {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, homeService.getHomePlaces());
    }

    @Operation(summary = "오늘의 추천 코스", description = "홈 화면 추천 코스 목록 (최대 5개). 담기 횟수 내림차순.")
    @GetMapping("/courses")
    public ApiResponse<List<HomeCourseResponse>> getHomeCourses() {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, homeService.getHomeCourses());
    }
}
