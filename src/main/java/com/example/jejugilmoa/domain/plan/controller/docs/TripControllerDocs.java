package com.example.jejugilmoa.domain.plan.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.dto.TripCompleteResponse;
import com.example.jejugilmoa.domain.plan.dto.TripResponse;
import com.example.jejugilmoa.domain.plan.dto.TripStartRequest;
import com.example.jejugilmoa.domain.plan.dto.VisitCheckRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointAddRequest;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
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

import java.util.List;

public interface TripControllerDocs {

    @Operation(
            summary = "여행 시작",
            description = """
                    계획중(DRAFT) 상태의 여행 계획을 시작해 진행중(IN_PROGRESS) 상태로 전환합니다.

                    - tripId는 별도로 발급되지 않고 여행 계획 ID를 그대로 사용합니다.
                    - 이미 시작했거나 완료된 여행은 `PLAN400_8` 오류가 반환됩니다.
                    - 응답의 `waypoints`는 방문 인증 상태를 포함한 전체 경유지 목록입니다.
                    """
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TripStartRequest.class),
                    examples = @ExampleObject(value = """
                            { "planId": 1 }
                            """)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "여행 시작 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON201",
                              "message": "성공적으로 응답이 생성되었습니다.",
                              "result": {
                                "tripId": 1,
                                "title": "제주 3박4일",
                                "status": "IN_PROGRESS",
                                "actualStartedAt": "2026-07-31T09:00:00",
                                "waypoints": [
                                  {
                                    "waypointId": 7,
                                    "visitDate": "2026-08-15",
                                    "sequenceOrder": 1,
                                    "placeId": 42,
                                    "placeName": "애월 카페거리",
                                    "categoryName": "카페",
                                    "imageUrl": "https://cdn.example.com/42.jpg",
                                    "address": "제주시 애월읍",
                                    "visited": false,
                                    "visitedAt": null
                                  }
                                ]
                              }
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "이미 시작했거나 완료된 여행",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN400_8","message":"계획중 상태의 여행만 시작할 수 있습니다.","result":null}
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
    ApiResponse<TripResponse> start(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @org.springframework.web.bind.annotation.RequestBody TripStartRequest request
    );

    @Operation(
            summary = "진행중인 여행 조회",
            description = """
                    로그인한 유저의 현재 진행중(IN_PROGRESS)인 여행을 조회합니다.

                    - 유저당 진행중인 여행은 최대 1건이라는 규칙을 전제로 tripId 없이 조회합니다.
                    - 진행중인 여행이 없으면 `PLAN404_6` 오류가 반환됩니다.
                    - 응답의 `waypoints`는 방문 인증 상태를 포함한 전체 경유지 목록입니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "진행중인 여행 조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 처리했습니다.",
                              "result": {
                                "tripId": 1,
                                "title": "제주 3박4일",
                                "status": "IN_PROGRESS",
                                "actualStartedAt": "2026-07-31T09:00:00",
                                "waypoints": [
                                  {
                                    "waypointId": 7,
                                    "visitDate": "2026-08-15",
                                    "sequenceOrder": 1,
                                    "placeId": 42,
                                    "placeName": "애월 카페거리",
                                    "categoryName": "카페",
                                    "imageUrl": "https://cdn.example.com/42.jpg",
                                    "address": "제주시 애월읍",
                                    "visited": false,
                                    "visitedAt": null,
                                    "isStart": false,
                                    "isDestination": false
                                  }
                                ]
                              }
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "진행중인 여행 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN404_6","message":"진행중인 여행이 없습니다.","result":null}
                            """))
            )
    })
    ApiResponse<TripResponse> getCurrentTrip(
            @AuthenticationPrincipal UserPrincipal principal
    );

    @Operation(
            summary = "경유지 방문 인증",
            description = """
                    진행중인 여행의 경유지를 방문 완료로 인증합니다. GPS 기반 위치 인증을 거칩니다.

                    - 경유지는 `sequenceOrder` 순서대로만 인증할 수 있습니다. 아직 방문하지 않은
                      경유지 중 순번이 가장 빠른 것이 아니면 `PLAN400_13` 오류가 반환됩니다.
                    - 요청의 `latitude`/`longitude`(현재 위치)가 경유지 장소로부터 반경 100m 이내가
                      아니면 `PLAN400_12` 오류가 반환됩니다 (일반적인 GPS 오차를 감안한 값).
                    - 여행이 진행중(IN_PROGRESS)이 아니면 `PLAN400_10` 오류가 반환됩니다.
                    - 이미 방문 인증된 경유지를 다시 인증하면 `PLAN400_11` 오류가 반환됩니다.
                    - 응답으로 방문 인증 후 전체 경유지 목록(순서 오름차순)을 반환합니다.
                    """
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = VisitCheckRequest.class),
                    examples = @ExampleObject(value = """
                            { "waypointId": 7, "latitude": 33.39397, "longitude": 126.24066 }
                            """)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "방문 인증 성공",
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
                                  "address": "제주시 애월읍",
                                  "visited": true,
                                  "visitedAt": "2026-07-31T09:12:34"
                                }
                              ]
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "진행중이 아닌 여행, 이미 방문 인증된 경유지, 순서 위반, 또는 위치 인증 실패",
                    content = @Content(examples = {
                            @ExampleObject(name = "진행중이 아닌 여행", value = """
                                    {"isSuccess":false,"code":"PLAN400_10","message":"진행중인 여행이 아닙니다.","result":null}
                                    """),
                            @ExampleObject(name = "이미 방문 인증됨", value = """
                                    {"isSuccess":false,"code":"PLAN400_11","message":"이미 방문 인증된 경유지입니다.","result":null}
                                    """),
                            @ExampleObject(name = "위치 인증 실패", value = """
                                    {"isSuccess":false,"code":"PLAN400_12","message":"방문할 경유지와 거리가 멉니다.","result":null}
                                    """),
                            @ExampleObject(name = "순서 위반", value = """
                                    {"isSuccess":false,"code":"PLAN400_13","message":"이전 경유지를 먼저 방문 인증해야 합니다.","result":null}
                                    """)
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "접근 권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN403_1","message":"해당 여행 계획에 접근할 권한이 없습니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "여행 또는 경유지 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN404_3","message":"존재하지 않는 경유지입니다.","result":null}
                            """))
            )
    })
    ApiResponse<List<WaypointResponse>> checkVisit(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행(Trip) ID — 여행 계획 ID와 동일") Long tripId,
            @Valid @org.springframework.web.bind.annotation.RequestBody VisitCheckRequest request
    );

    @Operation(
            summary = "여행 중 경유지 추가",
            description = """
                    진행중(IN_PROGRESS)인 여행에 경유지(방문할 장소)를 추가합니다.

                    - 여행이 진행중이 아니면 `PLAN400_10` 오류가 반환됩니다.
                    - `visitDate`는 여행 계획의 시작일~종료일 범위 내여야 하며, 범위를 벗어나면 `PLAN400_9` 오류가 반환됩니다.
                    - 같은 여행에 이미 담긴 장소를 다시 추가하면 `PLAN400_6` 오류가 반환됩니다.
                    - 추가된 경유지는 해당 날짜의 마지막 순번으로 배치됩니다.
                    - 응답으로 추가 후 전체 경유지 목록(방문 인증 상태 포함, 순서 오름차순)을 반환합니다.
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
                                  "address": "제주시 애월읍",
                                  "visited": false,
                                  "visitedAt": null
                                }
                              ]
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "진행중이 아닌 여행, 날짜 범위 초과, 또는 이미 담긴 장소",
                    content = @Content(examples = {
                            @ExampleObject(name = "진행중이 아닌 여행", value = """
                                    {"isSuccess":false,"code":"PLAN400_10","message":"진행중인 여행이 아닙니다.","result":null}
                                    """),
                            @ExampleObject(name = "날짜 범위 초과", value = """
                                    {"isSuccess":false,"code":"PLAN400_9","message":"여행 날짜 범위에 포함되지 않는 날짜입니다.","result":null}
                                    """),
                            @ExampleObject(name = "이미 담긴 장소", value = """
                                    {"isSuccess":false,"code":"PLAN400_6","message":"이미 추가된 장소입니다.","result":null}
                                    """)
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "접근 권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN403_1","message":"해당 여행 계획에 접근할 권한이 없습니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "여행 또는 장소 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN404_1","message":"존재하지 않는 여행 계획입니다.","result":null}
                            """))
            )
    })
    ApiResponse<List<WaypointResponse>> addWaypoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행(Trip) ID — 여행 계획 ID와 동일") Long tripId,
            @Valid @org.springframework.web.bind.annotation.RequestBody WaypointAddRequest request
    );

    @Operation(
            summary = "여행 중 경유지 삭제",
            description = """
                    진행중(IN_PROGRESS)인 여행에서 경유지를 삭제합니다.

                    - 여행이 진행중이 아니면 `PLAN400_10` 오류가 반환됩니다.
                    - 출발지 또는 목적지는 삭제할 수 없습니다 — `PLAN400_16` 오류가 반환됩니다.
                    - 이미 방문 인증된 경유지는 삭제할 수 없습니다 — `PLAN400_15` 오류가 반환됩니다.
                    - 삭제 후 같은 날짜의 이후 경유지 순번이 자동으로 당겨집니다.
                    - 응답으로 삭제 후 전체 경유지 목록(순서 오름차순)을 반환합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "경유지 삭제 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 처리했습니다.",
                              "result": []
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "진행중이 아닌 여행, 출발지·목적지, 또는 이미 방문한 경유지",
                    content = @Content(examples = {
                            @ExampleObject(name = "진행중이 아닌 여행", value = """
                                    {"isSuccess":false,"code":"PLAN400_10","message":"진행중인 여행이 아닙니다.","result":null}
                                    """),
                            @ExampleObject(name = "출발지·목적지 삭제 시도", value = """
                                    {"isSuccess":false,"code":"PLAN400_16","message":"출발지 또는 목적지는 삭제할 수 없습니다.","result":null}
                                    """),
                            @ExampleObject(name = "이미 방문한 경유지", value = """
                                    {"isSuccess":false,"code":"PLAN400_15","message":"이미 방문한 경유지는 삭제할 수 없습니다.","result":null}
                                    """)
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "접근 권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN403_1","message":"해당 여행 계획에 접근할 권한이 없습니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "여행 또는 경유지 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"PLAN404_3","message":"존재하지 않는 경유지입니다.","result":null}
                            """))
            )
    })
    ApiResponse<List<WaypointResponse>> removeWaypoint(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행(Trip) ID — 여행 계획 ID와 동일") Long tripId,
            @Parameter(description = "경유지(TravelCourse) ID") Long waypointId
    );

    @Operation(
            summary = "여행 완료 처리",
            description = """
                    진행중(IN_PROGRESS)인 여행을 완료(COMPLETED) 상태로 전환합니다.

                    - 모든 경유지가 방문 인증되어 있어야 완료할 수 있습니다. 하나라도 미방문이면
                      `PLAN400_14` 오류가 반환됩니다.
                    - 진행중 상태가 아니면 `PLAN400_10` 오류가 반환됩니다.
                    - 응답에는 체류일수, 담은 장소 수, 방문 순서를 따라 계산한 총 이동거리(km)가 포함됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "여행 완료 처리 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 처리했습니다.",
                              "result": {
                                "tripId": 1,
                                "title": "제주 3박4일",
                                "status": "COMPLETED",
                                "startDate": "2026-07-20",
                                "endDate": "2026-07-23",
                                "durationDays": 4,
                                "placeCount": 7,
                                "totalDistanceKm": 86.0,
                                "actualStartedAt": "2026-07-20T09:00:00",
                                "actualCompletedAt": "2026-07-23T17:30:00"
                              }
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "진행중이 아닌 여행 또는 미방문 경유지 존재",
                    content = @Content(examples = {
                            @ExampleObject(name = "진행중이 아닌 여행", value = """
                                    {"isSuccess":false,"code":"PLAN400_10","message":"진행중인 여행이 아닙니다.","result":null}
                                    """),
                            @ExampleObject(name = "미방문 경유지 존재", value = """
                                    {"isSuccess":false,"code":"PLAN400_14","message":"모든 경유지를 방문해야 여행을 완료할 수 있습니다.","result":null}
                                    """)
                    })
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
    ApiResponse<TripCompleteResponse> complete(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "여행(Trip) ID — 여행 계획 ID와 동일") Long tripId
    );
}
