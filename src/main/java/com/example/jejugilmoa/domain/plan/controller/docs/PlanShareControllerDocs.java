package com.example.jejugilmoa.domain.plan.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.dto.ShareLinkResponse;
import com.example.jejugilmoa.domain.plan.dto.SharedPlanResponse;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

public interface PlanShareControllerDocs {

    @Operation(summary = "여행 계획 공유 링크 생성", description = """
            본인 여행 계획의 공유 토큰을 발급합니다. 유효한 링크가 있으면 토큰과 만료 시각을 그대로 반환하고,
            만료되었거나 비활성 상태이면 새 토큰을 발급해 현재 시각부터 30일간 유효하게 만듭니다.
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "공유 링크 생성 또는 기존 링크 반환",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":true,"code":"COMMON201","message":"성공적으로 응답이 생성되었습니다.","result":{"planId":1,"shareToken":"550e8400-e29b-41d4-a716-446655440000","expiresAt":"2026-09-03T00:00:00Z"}}
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "계획 소유권 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "계획 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "공유 링크 저장 실패")
    })
    ApiResponse<ShareLinkResponse> createShareLink(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행 계획 ID") Long planId);

    @Operation(summary = "공유 여행 계획 조회", description = """
            인증 없이 활성 상태이며 만료되지 않은 공유 토큰으로 현재 여행 계획과 경유지를 조회합니다.
            응답에는 공개 가능한 계획·장소·예산 정보만 포함됩니다.
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공유 계획 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "토큰이 없거나 비활성 상태",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN404_4","message":"유효한 여행 계획 공유 링크를 찾을 수 없습니다.","result":null}
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "410", description = "공유 링크 만료",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN410_1","message":"여행 계획 공유 링크가 만료되었습니다.","result":null}
                            """)))
    })
    ApiResponse<SharedPlanResponse> getSharedPlan(
            @Parameter(description = "공유 토큰") String shareToken);
}
