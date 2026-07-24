package com.example.jejugilmoa.domain.plan.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanCreateRequest;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanCreateResponse;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanListResponse;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface TravelPlanControllerDocs {

    @Operation(summary = "내 여행 계획 목록 조회", description = """
            로그인한 사용자의 여행 계획 목록을 최신순으로 반환합니다.

            - `status` 미지정 시 전체(전체 탭), 지정 시 해당 상태만 반환합니다.
            - DRAFT=계획중, IN_PROGRESS=진행중, COMPLETED=완료
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "목록 조회 성공",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "성공적으로 요청을 처리했습니다.",
                                      "result": [
                                        {
                                          "planId": 1,
                                          "title": "제주 3박4일",
                                          "startDate": "2026-07-15",
                                          "endDate": "2026-07-18",
                                          "status": "IN_PROGRESS",
                                          "waypointCount": 2,
                                          "nights": 3,
                                          "days": 4,
                                          "dDay": -9
                                        },
                                        {
                                          "planId": 2,
                                          "title": "제주 당일치기",
                                          "startDate": "2026-08-02",
                                          "endDate": "2026-08-02",
                                          "status": "DRAFT",
                                          "waypointCount": 3,
                                          "nights": 0,
                                          "days": 1,
                                          "dDay": 9
                                        }
                                      ]
                                    }
                                    """)
                    )
            )
    })
    ApiResponse<List<TravelPlanListResponse>> getMyPlans(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) TravelPlanStatus status
    );

    @Operation(summary = "여행 계획 생성", description = """
            새로운 여행 계획을 생성합니다.

            - `departurePlaceId`와 `departureLocationName` 중 하나는 필수입니다.
            - `destinationPlaceId`와 `destinationLocationName` 중 하나는 필수입니다.
            - `categoryIds`는 1개 이상이어야 합니다. 유효한 ID는 DB `category` 테이블에서 확인하세요.
            - `startDate`는 오늘 이후여야 합니다.
            - `endDate`는 `startDate`와 같거나 이후여야 합니다.
            """)
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TravelPlanCreateRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "텍스트로 출발지·목적지 입력",
                                    summary = "Place ID 없이 텍스트로 출발지·목적지를 입력하는 케이스",
                                    value = """
                                            {
                                              "title": "제주 여름 휴가",
                                              "startDate": "2026-08-15",
                                              "endDate": "2026-08-17",
                                              "region": "JEJU_ALL",
                                              "departurePlaceId": null,
                                              "departureLocationName": "제주국제공항",
                                              "destinationPlaceId": null,
                                              "destinationLocationName": "성산일출봉",
                                              "transportMode": "WALK",
                                              "categoryIds": [1, 2]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Place ID로 출발지 지정",
                                    summary = "등록된 Place를 출발지로 선택하는 케이스",
                                    value = """
                                            {
                                              "title": "서귀포 당일치기",
                                              "startDate": "2026-08-20",
                                              "endDate": "2026-08-20",
                                              "region": "SEOGWIPO",
                                              "departurePlaceId": 1,
                                              "departureLocationName": null,
                                              "destinationPlaceId": null,
                                              "destinationLocationName": "중문관광단지",
                                              "transportMode": "CAR",
                                              "categoryIds": [1]
                                            }
                                            """
                            )
                    }
            )
    )
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
                                        "startDate": "2026-08-15",
                                        "endDate": "2026-08-17",
                                        "region": "JEJU_ALL",
                                        "transportMode": "WALK",
                                        "status": "DRAFT",
                                        "categories": ["자연", "음식"]
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
            @Valid @org.springframework.web.bind.annotation.RequestBody TravelPlanCreateRequest request
    );
}
