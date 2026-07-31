package com.example.jejugilmoa.domain.plan.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.dto.TripResponse;
import com.example.jejugilmoa.domain.plan.dto.TripStartRequest;
import com.example.jejugilmoa.domain.plan.dto.VisitCheckRequest;
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
                    - 응답의 `waypoints`는 방문 체크 상태를 포함한 전체 경유지 목록입니다.
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
                                    "sequenceOrder": 1,
                                    "placeId": 42,
                                    "placeName": "애월 카페거리",
                                    "categoryName": "카페",
                                    "imageUrl": "https://cdn.example.com/42.jpg",
                                    "address": "제주시 애월읍",
                                    "memo": null,
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
            summary = "경유지 방문 체크",
            description = """
                    진행중인 여행의 경유지를 방문 완료로 체크합니다. GPS 기반 위치 인증을 거칩니다.

                    - 경유지는 `sequenceOrder` 순서대로만 체크할 수 있습니다. 아직 방문하지 않은
                      경유지 중 순번이 가장 빠른 것이 아니면 `PLAN400_12` 오류가 반환됩니다.
                    - 요청의 `latitude`/`longitude`(현재 위치)가 경유지 장소로부터 반경 100m 이내가
                      아니면 `PLAN400_11` 오류가 반환됩니다 (일반적인 GPS 오차를 감안한 값).
                    - 여행이 진행중(IN_PROGRESS)이 아니면 `PLAN400_9` 오류가 반환됩니다.
                    - 이미 방문 체크된 경유지를 다시 체크하면 `PLAN400_10` 오류가 반환됩니다.
                    - 응답으로 방문 체크 후 전체 경유지 목록(순서 오름차순)을 반환합니다.
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
                    responseCode = "201", description = "방문 체크 성공",
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
                                  "address": "제주시 애월읍",
                                  "memo": null,
                                  "visited": true,
                                  "visitedAt": "2026-07-31T09:12:34"
                                }
                              ]
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "진행중이 아닌 여행, 이미 방문 체크된 경유지, 순서 위반, 또는 위치 인증 실패",
                    content = @Content(examples = {
                            @ExampleObject(name = "진행중이 아닌 여행", value = """
                                    {"isSuccess":false,"code":"PLAN400_9","message":"진행중인 여행이 아닙니다.","result":null}
                                    """),
                            @ExampleObject(name = "이미 방문 체크됨", value = """
                                    {"isSuccess":false,"code":"PLAN400_10","message":"이미 방문 체크된 경유지입니다.","result":null}
                                    """),
                            @ExampleObject(name = "위치 인증 실패", value = """
                                    {"isSuccess":false,"code":"PLAN400_11","message":"현재 위치가 장소와 너무 멀리 떨어져 있어 방문 체크할 수 없습니다.","result":null}
                                    """),
                            @ExampleObject(name = "순서 위반", value = """
                                    {"isSuccess":false,"code":"PLAN400_12","message":"이전 경유지를 먼저 방문 체크해야 합니다.","result":null}
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
}
