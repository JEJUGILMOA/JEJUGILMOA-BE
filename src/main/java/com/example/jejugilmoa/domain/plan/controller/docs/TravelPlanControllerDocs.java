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

    @Operation(summary = "여행 계획 상세 조회", description = """
            여행 계획 1건의 전체 정보를 반환합니다.

            - 날짜별로 그룹핑된 경유지 목록(`itinerary`)을 포함합니다.
            - 계획 기간의 모든 날짜를 포함하며, 경유지가 없는 날도 빈 `waypoints` 배열로 반환합니다.
            - 예산을 하나도 입력하지 않은 경우 `totalBudget`은 `null`입니다.
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "상세 조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 처리했습니다.",
                              "result": {
                                "planId": 26,
                                "title": "제주 가을 여행",
                                "startDate": "2027-08-15",
                                "endDate": "2027-08-17",
                                "nights": 2,
                                "days": 3,
                                "status": "DRAFT",
                                "travelStyle": "RELAXED",
                                "companion": "COUPLE",
                                "departureLocationName": "새별오름",
                                "departureLatitude": 33.36050,
                                "departureLongitude": 126.40860,
                                "categories": ["NATURE", "CAFE"],
                                "itinerary": [
                                  {
                                    "date": "2027-08-15",
                                    "dayNumber": 1,
                                    "waypoints": [
                                      {"waypointId": 7, "visitDate": "2027-08-15", "sequenceOrder": 1, "placeId": 42, "placeName": "협재해수욕장", "categoryName": "자연", "imageUrl": null, "address": "제주시 한림읍"},
                                      {"waypointId": 8, "visitDate": "2027-08-15", "sequenceOrder": 2, "placeId": 10, "placeName": "애월 카페거리", "categoryName": "카페", "imageUrl": null, "address": "제주시 애월읍"}
                                    ]
                                  },
                                  {
                                    "date": "2027-08-16",
                                    "dayNumber": 2,
                                    "waypoints": [
                                      {"waypointId": 9, "visitDate": "2027-08-16", "sequenceOrder": 1, "placeId": 5, "placeName": "성산일출봉", "categoryName": "자연", "imageUrl": null, "address": "서귀포시 성산읍"}
                                    ]
                                  },
                                  {
                                    "date": "2027-08-17",
                                    "dayNumber": 3,
                                    "waypoints": []
                                  }
                                ],
                                "budgetTransportation": 50000,
                                "budgetAccommodation": 150000,
                                "budgetFood": 80000,
                                "budgetEtc": null,
                                "totalBudget": 280000
                              }
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "타인의 계획 접근",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN403_1","message":"해당 여행 계획에 접근할 권한이 없습니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않는 계획",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN404_1","message":"존재하지 않는 여행 계획입니다.","result":null}
                            """))
            )
    })
    ApiResponse<TravelPlanDetailResponse> getPlanDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행 계획 ID") Long planId
    );

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
            새로운 여행 계획을 단일 요청으로 생성합니다. 일정(날짜별 경유지), 예산, 동반자를 한 번에 저장할 수 있습니다.

            **출발지**
            - `departureLatitude` / `departureLongitude`: 항상 필수
            - `departurePlaceId` 또는 `departureLocationName` 중 하나 추가 필수 (둘 다 없으면 `PLAN400_3`)

            **날짜별 경유지 (`days`, 선택)**
            - 생략하거나 빈 배열을 전달하면 경유지 없이 계획만 생성됩니다.
            - 각 경유지에 `isPreferred: true`를 설정하면 추천 기준점(앵커)으로 지정됩니다.
            - `visitDate`는 `startDate`~`endDate` 범위 내여야 합니다.

            **기타**
            - `categories`는 전달 시 1개 이상이어야 합니다. 유효한 값: `FOOD NATURE ACTIVITY CAFE CULTURE SHOPPING FESTIVAL`.
            - `startDate`는 오늘 포함 이후여야 합니다.
            - `endDate`는 `startDate`와 같거나 이후여야 합니다.
            """)
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TravelPlanCreateRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "title": "제주 가을 여행",
                              "startDate": "2027-08-15",
                              "endDate": "2027-08-17",
                              "departurePlaceId": null,
                              "departureLocationName": "제주국제공항",
                              "departureLatitude": 33.5070,
                              "departureLongitude": 126.4927,
                              "companion": "COUPLE",
                              "categories": ["NATURE", "CAFE"],
                              "days": [
                                {
                                  "visitDate": "2027-08-15",
                                  "waypoints": [
                                    { "placeId": 42, "isPreferred": true },
                                    { "placeId": 10, "isPreferred": false }
                                  ]
                                },
                                {
                                  "visitDate": "2027-08-16",
                                  "waypoints": []
                                }
                              ],
                              "budget": {
                                "budgetTransportation": 50000,
                                "budgetAccommodation": 150000,
                                "budgetFood": 80000,
                                "budgetEtc": null
                              }
                            }
                            """)
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
                                        "title": "제주 가을 여행",
                                        "startDate": "2027-08-15",
                                        "endDate": "2027-08-17",
                                        "nights": 2,
                                        "days": 3,
                                        "status": "DRAFT",
                                        "travelStyle": "RELAXED",
                                        "companion": "COUPLE",
                                        "departureLocationName": "제주국제공항",
                                        "departureLatitude": 33.50720,
                                        "departureLongitude": 126.49290,
                                        "categories": ["NATURE", "CAFE"],
                                        "itinerary": [
                                          {
                                            "date": "2027-08-15",
                                            "dayNumber": 1,
                                            "waypoints": [
                                              {"waypointId": 1, "visitDate": "2027-08-15", "sequenceOrder": 1, "placeId": 42, "placeName": "협재해수욕장", "isPreferred": true, "isStart": true, "isDestination": false, "visited": false, "visitedAt": null},
                                              {"waypointId": 2, "visitDate": "2027-08-15", "sequenceOrder": 2, "placeId": 10, "placeName": "애월 카페거리", "isPreferred": false, "isStart": false, "isDestination": true, "visited": false, "visitedAt": null}
                                            ]
                                          },
                                          {"date": "2027-08-16", "dayNumber": 2, "waypoints": []},
                                          {"date": "2027-08-17", "dayNumber": 3, "waypoints": []}
                                        ],
                                        "budgetTransportation": 50000,
                                        "budgetAccommodation": 150000,
                                        "budgetFood": 80000,
                                        "budgetEtc": null,
                                        "totalBudget": 280000
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (날짜 오류, 출발지 누락 등)",
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
                    description = "존재하지 않는 장소",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "PLACE404_1",
                                      "message": "존재하지 않는 관광지입니다.",
                                      "result": null
                                    }
                                    """)
                    )
            )
    })
    ApiResponse<TravelPlanDetailResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @org.springframework.web.bind.annotation.RequestBody TravelPlanCreateRequest request
    );

    @Operation(
            summary = "여행 계획 전체 수정 (덮어쓰기)",
            description = """
                    DRAFT 상태의 여행 계획을 전체 교체(full overwrite)합니다.

                    - **계획 중(DRAFT) 상태**에서만 수정할 수 있습니다. 진행중·완료 상태는 `PLAN400_17`.
                    - 요청 본문은 계획 생성(`POST /api/plans`)과 동일한 형식입니다.
                    - 날짜별 경유지, 카테고리, 예산을 한 번에 교체합니다.
                    - `budget: null` 전달 시 예산 정보를 모두 삭제합니다.
                    - `days`를 생략하거나 빈 배열로 전달하면 기존 경유지가 모두 제거됩니다.
                    - **`startDate` / `endDate`는 수정 불가**합니다. 기존 계획의 날짜와 다른 값을 전달하면 `PLAN400_20`.
                    """
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TravelPlanCreateRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "title": "제주 가을 여행 (수정)",
                              "startDate": "2027-08-15",
                              "endDate": "2027-08-17",
                              "departurePlaceId": null,
                              "departureLocationName": "제주국제공항",
                              "departureLatitude": 33.5070,
                              "departureLongitude": 126.4927,
                              "companion": "COUPLE",
                              "categories": ["NATURE", "CAFE"],
                              "days": [
                                {
                                  "visitDate": "2027-08-15",
                                  "waypoints": [
                                    { "placeId": 42, "isPreferred": true }
                                  ]
                                }
                              ],
                              "budget": {
                                "budgetTransportation": 50000,
                                "budgetAccommodation": 150000,
                                "budgetFood": 80000,
                                "budgetEtc": null
                              }
                            }
                            """)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "수정 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 처리했습니다.",
                              "result": {
                                "planId": 26,
                                "title": "제주 가을 여행 (수정)",
                                "startDate": "2027-08-15",
                                "endDate": "2027-08-17",
                                "nights": 2,
                                "days": 3,
                                "status": "DRAFT",
                                "travelStyle": "RELAXED",
                                "companion": "COUPLE",
                                "departureLocationName": "제주국제공항",
                                "departureLatitude": 33.50720,
                                "departureLongitude": 126.49290,
                                "categories": ["NATURE", "CAFE"],
                                "itinerary": [
                                  {
                                    "date": "2027-08-15",
                                    "dayNumber": 1,
                                    "waypoints": [
                                      {"waypointId": 15, "visitDate": "2027-08-15", "sequenceOrder": 1, "placeId": 42, "placeName": "협재해수욕장", "isPreferred": true, "isStart": true, "isDestination": true, "visited": false, "visitedAt": null}
                                    ]
                                  },
                                  {"date": "2027-08-16", "dayNumber": 2, "waypoints": []},
                                  {"date": "2027-08-17", "dayNumber": 3, "waypoints": []}
                                ],
                                "budgetTransportation": 50000,
                                "budgetAccommodation": 150000,
                                "budgetFood": 80000,
                                "budgetEtc": null,
                                "totalBudget": 280000
                              }
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "날짜 변경 시도, 출발지 누락, DRAFT 상태 아님",
                    content = @Content(examples = {
                            @ExampleObject(name = "날짜 변경 시도", value = """
                                    {"isSuccess":false,"code":"PLAN400_20","message":"여행 날짜는 수정할 수 없습니다.","result":null}
                                    """),
                            @ExampleObject(name = "DRAFT 상태 아님", value = """
                                    {"isSuccess":false,"code":"PLAN400_17","message":"계획 중 상태의 여행만 수정할 수 있습니다.","result":null}
                                    """)
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "타인의 여행 계획에 접근",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN403_1","message":"해당 여행 계획에 접근할 권한이 없습니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않는 계획 또는 장소",
                    content = @Content(examples = {
                            @ExampleObject(name = "계획 미존재", value = """
                                    {"isSuccess":false,"code":"PLAN404_1","message":"존재하지 않는 여행 계획입니다.","result":null}
                                    """),
                            @ExampleObject(name = "장소 미존재 (departurePlaceId 또는 days[].placeId)", value = """
                                    {"isSuccess":false,"code":"PLACE404_1","message":"존재하지 않는 관광지입니다.","result":null}
                                    """)
                    })
            )
    })
    ApiResponse<TravelPlanDetailResponse> replacePlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행 계획 ID") Long planId,
            @Valid @org.springframework.web.bind.annotation.RequestBody TravelPlanCreateRequest request
    );

    @Operation(
            summary = "여행 계획 삭제",
            description = """
                    여행 계획을 삭제합니다.

                    **삭제 범위**
                    - 경유지 목록 (TravelCourse)
                    - 선호 카테고리 설정

                    이미 생성된 여행 기록(TravelRecord)은 삭제되지 않고 보존됩니다.
                    보존된 여행 기록의 계획 참조는 해제됩니다.

                    본인의 계획만 삭제할 수 있습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "삭제 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":true,"code":"COMMON200","message":"성공적으로 요청을 처리했습니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "타인의 여행 계획에 접근",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN403_1","message":"해당 여행 계획에 접근할 권한이 없습니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "존재하지 않는 여행 계획",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN404_1","message":"존재하지 않는 여행 계획입니다.","result":null}
                            """))
            )
    })
    ApiResponse<Void> deletePlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행 계획 ID") Long planId
    );

}
