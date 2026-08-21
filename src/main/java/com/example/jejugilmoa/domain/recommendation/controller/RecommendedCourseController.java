package com.example.jejugilmoa.domain.recommendation.controller;

import com.example.jejugilmoa.domain.plan.enums.TravelTheme;
import com.example.jejugilmoa.domain.recommendation.dto.RecommendedCourseResponse;
import com.example.jejugilmoa.domain.recommendation.service.RecommendedCourseService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "추천 코스", description = "큐레이션 코스 목록 조회")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class RecommendedCourseController {

    private final RecommendedCourseService recommendedCourseService;

    @Operation(
            summary = "추천 코스 목록 조회",
            description = "테마 필터 없이 호출하면 전체 코스를 담기 횟수 내림차순으로 반환합니다. " +
                    "themes 파라미터를 지정하면 해당 테마의 코스만 반환합니다."
    )
    @GetMapping("/recommended")
    public ApiResponse<List<RecommendedCourseResponse>> getRecommended(
            @Parameter(description = "테마 필터 (복수 가능, 미입력 시 전체)", example = "FOOD,NATURE")
            @RequestParam(required = false) List<TravelTheme> themes,
            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                recommendedCourseService.getRecommendedCourses(themes));
    }
}
