package com.example.jejugilmoa.domain.plan.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.dto.*;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    // ──────────────────────────────────────────────────────────────────────────
    // 경유지 추천
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "경유지 추천 (최초)",
            description = """
                    여행 계획의 출발지-목적지 경로에서 선호 카테고리 기반으로 경유지를 최대 5개 추천합니다.

                    - 출발지·목적지 모두 Place 엔티티를 가진 경우 PostGIS corridor 쿼리(경로 좌우 5km 이내)로 추천합니다.
                    - 텍스트 입력 출발지/목적지만 있는 경우 방문자 수 내림차순 인기 장소로 폴백합니다.
                    - 이미 담은 경유지는 자동으로 제외됩니다.
                    - `estimatedTravelMinutes`는 출발지로부터의 직선거리 기반 이동 시간 추정치입니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "추천 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공입니다.",
                              "result": {
                                "departureLocationName": "제주국제공항",
                                "destinationLocationName": "성산일출봉",
                                "recommendations": [
                                  {
                                    "placeId": 10,
                                    "name": "애월 카페거리",
                                    "categoryName": "카페",
                                    "imageUrl": "https://cdn.example.com/10.jpg",
                                    "address": "제주시 애월읍",
                                    "latitude": 33.46281,
                                    "longitude": 126.31466,
                                    "estimatedTravelMinutes": 8
                                  }
                                ]
                              }
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "접근 권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN403_1","message":"해당 여행 계획에 접근할 권한이 없습니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "여행 계획 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN404_1","message":"존재하지 않는 여행 계획입니다.","result":null}
                            """))
            )
    })
    ApiResponse<RecommendationResponse> getRecommendations(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행 계획 ID") Long planId
    );

    @Operation(
            summary = "경유지 재추천 (건너뛰기)",
            description = """
                    지정한 장소를 제외하고 새로운 경유지를 다시 추천합니다.

                    - `excludedPlaceIds`에 건너뛸 장소 ID를 1개 이상 전달하세요.
                    - 이미 담긴 경유지는 자동으로 제외됩니다.
                    """
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RecommendationSkipRequest.class),
                    examples = @ExampleObject(value = """
                            { "excludedPlaceIds": [10, 23] }
                            """)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "재추천 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공입니다.",
                              "result": {
                                "departureLocationName": "제주국제공항",
                                "destinationLocationName": "성산일출봉",
                                "recommendations": [
                                  {
                                    "placeId": 42,
                                    "name": "광치기해변",
                                    "categoryName": "자연",
                                    "imageUrl": "https://cdn.example.com/42.jpg",
                                    "address": "서귀포시 성산읍",
                                    "latitude": 33.45691,
                                    "longitude": 126.93024,
                                    "estimatedTravelMinutes": 5
                                  }
                                ]
                              }
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "excludedPlaceIds 누락 또는 빈 배열",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"COMMON400_1","message":"잘못된 요청입니다.","result":null}
                            """))
            )
    })
    ApiResponse<RecommendationResponse> reRecommend(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행 계획 ID") Long planId,
            @Valid @org.springframework.web.bind.annotation.RequestBody RecommendationSkipRequest request
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 경유지 추가 / 제거
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "경유지 추가 (담기)",
            description = """
                    추천 경유지를 여행 코스에 추가합니다.

                    - 같은 장소를 중복 추가하면 `PLAN400_6` 오류가 반환됩니다.
                    - 응답으로 추가 후 전체 경유지 목록(순서 오름차순)을 반환합니다.
                    """
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = WaypointAddRequest.class),
                    examples = @ExampleObject(value = """
                            { "placeId": 42 }
                            """)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "경유지 추가 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON201",
                              "message": "성공적으로 응답이 생성되었습니다.",
                              "result": [
                                {
                                  "waypointId": 7,
                                  "sequenceOrder": 1,
                                  "placeId": 42,
                                  "placeName": "애월 카페거리",
                                  "categoryName": "카페",
                                  "imageUrl": "https://cdn.example.com/42.jpg",
                                  "address": "제주시 애월읍"
                                }
                              ]
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "이미 추가된 장소",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN400_6","message":"이미 추가된 장소입니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "장소 또는 계획 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLACE404_1","message":"존재하지 않는 장소입니다.","result":null}
                            """))
            )
    })
    ApiResponse<List<WaypointResponse>> addWaypoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행 계획 ID") Long planId,
            @Valid @org.springframework.web.bind.annotation.RequestBody WaypointAddRequest request
    );

    @Operation(
            summary = "경유지 제거",
            description = """
                    담은 경유지를 여행 코스에서 제거합니다.

                    - 제거 후 뒤 순번 경유지들의 `sequenceOrder`가 자동으로 재정렬됩니다.
                    - 응답으로 제거 후 전체 경유지 목록(순서 오름차순)을 반환합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "경유지 제거 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공입니다.",
                              "result": []
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "경유지 없음 (planId 불일치 포함)",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN404_3","message":"존재하지 않는 경유지입니다.","result":null}
                            """))
            )
    })
    ApiResponse<List<WaypointResponse>> removeWaypoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행 계획 ID") Long planId,
            @Parameter(description = "경유지(TravelCourse) ID") Long waypointId
    );
}
