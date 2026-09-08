package com.example.jejugilmoa.domain.recommendation.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.recommendation.dto.SaveCourseRequest;
import com.example.jejugilmoa.domain.recommendation.dto.SavedCourseDetailResponse;
import com.example.jejugilmoa.domain.recommendation.dto.SavedCourseListItemResponse;
import com.example.jejugilmoa.domain.recommendation.service.SavedCourseService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "담은 코스", description = "코스 담기 / 담은 코스 목록 조회 API")
@RestController
@RequestMapping("/api/courses/saved")
@RequiredArgsConstructor
public class SavedCourseController {

    private final SavedCourseService savedCourseService;

    @Operation(
            summary = "코스 담기",
            description = """
                    추천 코스 또는 기록 코스를 담은 코스 목록에 저장합니다.

                    **sourceType에 따라 sourceId의 의미가 달라집니다.**
                    - `RECOMMENDED` → sourceId = 추천 코스 ID (`GET /api/courses/recommended` 응답의 `courseId`)
                    - `RECORD` → sourceId = 여행 기록 ID (`TravelRecord.id`). 본인 기록이거나 공개(`PUBLIC`) 기록만 담을 수 있습니다.

                    같은 숫자라도 RECOMMENDED와 RECORD는 별개의 테이블을 참조합니다.

                    **에러 코드**
                    - `COURSE404_1` : sourceId에 해당하는 코스/기록이 존재하지 않음
                    - `COURSE409_1` : 이미 담은 코스
                    - `COURSE403_1` : 비공개 기록 코스에 접근 불가
                    """
    )
    @PostMapping
    public ApiResponse<Void> saveCourse(
            @Valid @RequestBody SaveCourseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        savedCourseService.saveCourse(principal.userId(), request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }

    @Operation(summary = "담은 코스 삭제", description = "담은 코스 목록에서 해당 코스를 삭제합니다.")
    @DeleteMapping("/{savedCourseId}")
    public ApiResponse<Void> removeSavedCourse(
            @Parameter(description = "담은 코스 ID") @PathVariable Long savedCourseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        savedCourseService.removeSavedCourse(principal.userId(), savedCourseId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @Operation(summary = "담은 코스 목록 조회", description = "사용자가 담은 코스 목록을 최신순으로 반환합니다.")
    @GetMapping
    public ApiResponse<List<SavedCourseListItemResponse>> getSavedCourses(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                savedCourseService.getSavedCourses(principal.userId()));
    }

    @Operation(summary = "담은 코스 상세 조회", description = "담은 코스의 상세 정보(장소 목록 포함)를 반환합니다.")
    @GetMapping("/{savedCourseId}/detail")
    public ApiResponse<SavedCourseDetailResponse> getSavedCourseDetail(
            @Parameter(description = "담은 코스 ID") @PathVariable Long savedCourseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                savedCourseService.getSavedCourseDetail(principal.userId(), savedCourseId));
    }
}
