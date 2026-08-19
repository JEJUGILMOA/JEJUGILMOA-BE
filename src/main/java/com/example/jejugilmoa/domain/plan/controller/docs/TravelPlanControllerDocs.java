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
                                "planId": 1,
                                "title": "제주 여름 휴가",
                                "startDate": "2026-08-15",
                                "endDate": "2026-08-16",
                                "nights": 1,
                                "days": 2,
                                "status": "DRAFT",
                                "region": "JEJU_ALL",
                                "transportMode": "CAR",
                                "travelStyle": "RELAXED",
                                "departureLocationName": "제주국제공항",
                                "destinationLocationName": "성산일출봉",
                                "categories": ["자연", "카페"],
                                "itinerary": [
                                  {
                                    "date": "2026-08-15",
                                    "dayNumber": 1,
                                    "waypoints": [
                                      {"waypointId": 7, "visitDate": "2026-08-15", "sequenceOrder": 1, "placeId": 42, "placeName": "애월 카페거리", "categoryName": "카페", "imageUrl": null, "address": "제주시 애월읍"},
                                      {"waypointId": 8, "visitDate": "2026-08-15", "sequenceOrder": 2, "placeId": 10, "placeName": "협재해수욕장", "categoryName": "자연", "imageUrl": null, "address": "제주시 한림읍"}
                                    ]
                                  },
                                  {
                                    "date": "2026-08-16",
                                    "dayNumber": 2,
                                    "waypoints": [
                                      {"waypointId": 9, "visitDate": "2026-08-16", "sequenceOrder": 1, "placeId": 5, "placeName": "성산일출봉", "categoryName": "자연", "imageUrl": null, "address": "서귀포시 성산읍"}
                                    ]
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
                                        "categories": ["자연", "음식"],
                                        "departureLocationName": "제주국제공항",
                                        "destinationLocationName": "성산일출봉"
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
    // 경유지 근처 추천 장소 (TourAPI)
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "경유지 근처 추천 장소 조회",
        description = """
                    여행 계획에 추가된 경유지 좌표를 기준으로 TourAPI(`locationBasedList2`)를 호출해 근처 관광정보 3개를 추천합니다.
                    경유지 좌표는 서버에서 자동으로 추출하므로 별도 전달이 불필요합니다.

                    **추천 우선순위**
                    - 현재 코스에 없는 카테고리 장소를 우선 추천합니다.
                    - 다른 카테고리가 3개 미만이면 같은 카테고리로 보충합니다.
                    - 각 경유지 반경 5km 내 결과를 거리 오름차순으로 정렬합니다.

                    **다시 추천**
                    - 이미 노출된 장소의 `contentId`를 `excludeContentIds`에 누적 전달하면 새로운 3개를 반환합니다.
                    - `hasMore: false`이면 TourAPI 후보가 소진된 것이므로 프론트에서 Naver API fallback으로 전환하세요.
                    """
    )
    @RequestBody(
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = NearbyPlaceRecommendRequest.class),
            examples = {
                @ExampleObject(
                    name = "최초 추천",
                    summary = "처음 추천 요청 (excludeContentIds 빈 배열)",
                    value = """
                                            {
                                              "excludeContentIds": []
                                            }
                                            """
                ),
                @ExampleObject(
                    name = "다시 추천",
                    summary = "이전에 노출된 contentId를 누적해서 전달",
                    value = """
                                            {
                                              "excludeContentIds": ["126508", "264570", "987654"]
                                            }
                                            """
                )
            }
        )
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "추천 성공",
            content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 처리했습니다.",
                              "result": {
                                "recommendations": [
                                  {
                                    "contentId": "126508",
                                    "contentTypeId": 14,
                                    "title": "제주 민속자연사박물관",
                                    "address": "제주특별자치도 제주시 일주동로 17",
                                    "imageUrl": "https://cdn.visitjeju.net/photo/museum.jpg",
                                    "dist": 320,
                                    "mapX": 126.5312,
                                    "mapY": 33.4996
                                  }
                                ],
                                "hasMore": true
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
    ApiResponse<NearbyPlaceRecommendResponse> recommendNearbyPlaces(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "여행 계획 ID") Long planId,
        @Valid @org.springframework.web.bind.annotation.RequestBody NearbyPlaceRecommendRequest request
    );


    // ──────────────────────────────────────────────────────────────────────────
    // 경유지 추가 / 제거
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "경유지 추가 (담기)",
            description = """
                    추천 경유지를 여행 코스의 특정 날짜에 추가합니다.

                    - `visitDate`는 여행 계획의 시작일~종료일 범위 내 날짜여야 합니다. 범위 초과 시 `PLAN400_9`.
                    - 같은 장소를 중복 추가하면 `PLAN400_6` 오류가 반환됩니다.
                    - 해당 날짜 내 마지막 순번으로 자동 배정됩니다.
                    - 응답으로 추가 후 전체 경유지 목록(날짜·순서 오름차순)을 반환합니다.
                    """
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = WaypointAddRequest.class),
                    examples = @ExampleObject(value = """
                            { "placeId": 42, "visitDate": "2026-08-15" }
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
                                  "visitDate": "2026-08-15",
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
                    responseCode = "400", description = "날짜 범위 초과 또는 이미 추가된 장소",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN400_9","message":"여행 날짜 범위에 포함되지 않는 날짜입니다.","result":null}
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

    // ──────────────────────────────────────────────────────────────────────────
    // 경유지 순서 변경
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "경유지 순서 변경 (Day 단위)",
            description = """
                    특정 날짜의 경유지 목록 순서를 변경합니다.

                    - `visitDate`와 `waypointIds`를 함께 전달합니다.
                    - `waypointIds`는 해당 날짜에 속한 모든 경유지 ID를 원하는 순서로 포함해야 합니다.
                    - 전달한 ID 집합이 해당 날짜의 경유지 집합과 다르면 `PLAN400_7` 오류가 반환됩니다.
                    - 응답으로 재정렬된 전체 경유지 목록(날짜·순서 오름차순)을 반환합니다.
                    """
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = WaypointReorderRequest.class),
                    examples = @ExampleObject(value = """
                            { "visitDate": "2026-08-15", "waypointIds": [3, 1, 2] }
                            """)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "순서 변경 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 처리했습니다.",
                              "result": [
                                {"waypointId": 3, "visitDate": "2026-08-15", "sequenceOrder": 1, "placeId": 5, "placeName": "협재해수욕장", "categoryName": "자연", "imageUrl": null, "address": "제주시 한림읍"},
                                {"waypointId": 1, "visitDate": "2026-08-15", "sequenceOrder": 2, "placeId": 1, "placeName": "제주국제공항", "categoryName": "교통", "imageUrl": null, "address": "제주시 용담2동"},
                                {"waypointId": 2, "visitDate": "2026-08-16", "sequenceOrder": 1, "placeId": 3, "placeName": "애월 카페거리", "categoryName": "카페", "imageUrl": null, "address": "제주시 애월읍"}
                              ]
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "경유지 ID 목록이 해당 날짜의 경유지와 불일치",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN400_7","message":"경유지 순서 목록이 올바르지 않습니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "접근 권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN403_1","message":"해당 여행 계획에 접근할 권한이 없습니다.","result":null}
                            """))
            )
    })
    ApiResponse<List<WaypointResponse>> reorderWaypoints(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행 계획 ID") Long planId,
            @Valid @org.springframework.web.bind.annotation.RequestBody WaypointReorderRequest request
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 예산
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "예산 입력/수정",
            description = """
                    여행 계획의 예산 항목(교통비·숙박비·식비·기타)을 입력하거나 수정합니다.
                    - 4개 항목 중 원하는 항목만 선택적으로 입력할 수 있습니다.
                    - 특정 항목을 `null`로 전달하면 해당 항목 예산이 삭제됩니다.
                    - 4개 항목 모두 `null`인 경우 `totalBudget`은 `null`로 반환됩니다.
                    """
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BudgetUpdateRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "전체 항목 입력",
                                    summary = "4개 항목 모두 입력하는 케이스",
                                    value = """
                                            {
                                              "budgetTransportation": 50000,
                                              "budgetAccommodation": 150000,
                                              "budgetFood": 80000,
                                              "budgetEtc": 30000
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "일부 항목만 입력",
                                    summary = "교통비·식비만 입력, 나머지는 null로 삭제하는 케이스",
                                    value = """
                                            {
                                              "budgetTransportation": 50000,
                                              "budgetAccommodation": null,
                                              "budgetFood": 80000,
                                              "budgetEtc": null
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "전체 예산 삭제",
                                    summary = "모든 항목을 null로 전달해 예산을 초기화하는 케이스",
                                    value = """
                                            {
                                              "budgetTransportation": null,
                                              "budgetAccommodation": null,
                                              "budgetFood": null,
                                              "budgetEtc": null
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "예산 업데이트 성공",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "일부 항목 입력 시",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공적으로 요청을 처리했습니다.",
                                              "result": {
                                                "planId": 1,
                                                "budgetTransportation": 50000,
                                                "budgetAccommodation": 150000,
                                                "budgetFood": 80000,
                                                "budgetEtc": null,
                                                "totalBudget": 280000
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "전체 예산 삭제 시",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공적으로 요청을 처리했습니다.",
                                              "result": {
                                                "planId": 1,
                                                "budgetTransportation": null,
                                                "budgetAccommodation": null,
                                                "budgetFood": null,
                                                "budgetEtc": null,
                                                "totalBudget": null
                                              }
                                            }
                                            """
                            )
                    })
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
    ApiResponse<BudgetUpdateResponse> updateBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행 계획 ID") Long planId,
            @Valid @org.springframework.web.bind.annotation.RequestBody BudgetUpdateRequest request
    );
}
