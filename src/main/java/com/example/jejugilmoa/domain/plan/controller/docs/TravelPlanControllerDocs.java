package com.example.jejugilmoa.domain.plan.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanCreateRequest;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanCreateResponse;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

public interface TravelPlanControllerDocs {

    @Operation(summary = "여행 계획 생성", description = """
            새로운 여행 계획을 생성합니다.

            - `departurePlaceId`와 `departureLocationName` 중 하나는 필수입니다.
            - `destinationPlaceId`와 `destinationLocationName` 중 하나는 필수입니다.
            - `categoryIds`는 1개 이상이어야 합니다.
            - `startDate`는 오늘 이후여야 합니다.
            - `endDate`는 `startDate` 이후여야 합니다.
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "계획 생성 성공",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON201",
                                      "message": "성공적으로 응답이 생성되었습니다.",
                                      "result": {
                                        "planId": 1,
                                        "title": "제주 여름 휴가",
                                        "startDate": "2026-07-15",
                                        "endDate": "2026-07-17",
                                        "region": "JEJU_ALL",
                                        "transportMode": "WALK",
                                        "status": "DRAFT",
                                        "categories": ["자연", "카페"]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (날짜 오류, 출발지/목적지 누락 등)",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "PLAN400_3",
                                      "message": "출발지 정보를 입력해주세요.",
                                      "result": null
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 카테고리 또는 장소",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "PLAN404_2",
                                      "message": "존재하지 않는 카테고리입니다.",
                                      "result": null
                                    }
                                    """)
                    )
            )
    })
    ApiResponse<TravelPlanCreateResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TravelPlanCreateRequest request
    );
}
