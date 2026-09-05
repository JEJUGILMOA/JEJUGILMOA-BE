package com.example.jejugilmoa.domain.direction.controller.docs;

import com.example.jejugilmoa.domain.direction.dto.DirectionResponse;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.RequestParam;

public interface DirectionControllerDocs {

    @Operation(
        summary = "자동차 길찾기",
        description = """
            네이버 지도 Directions 5 API를 통해 출발지-목적지 간 자동차 경로를 조회합니다.

            - 좌표는 위도(lat)/경도(lng) 순서로 입력
            - `waypoints`: 경유지, `위도,경도|위도,경도` 형식으로 최대 5개
            - `option`: 경로 탐색 옵션 (미입력 시 `traoptimal`)

            **탐색 옵션**: `trafast`(실시간 빠른 길), `tracomfort`(실시간 편한 길),
            `traoptimal`(실시간 최적), `traavoidtoll`(무료 우선), `traavoidcaronly`(자동차 전용 도로 회피)

            응답의 `path`는 지도에 경로를 그리기 위한 좌표 목록이며,
            `summary.distance`는 미터, `summary.duration`은 밀리초 단위입니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": true,
                          "code": "FOUND200",
                          "message": "조회에 성공했습니다.",
                          "result": {
                            "option": "traoptimal",
                            "summary": {
                              "distance": 45231,
                              "duration": 3841000,
                              "tollFare": 0,
                              "taxiFare": 52000,
                              "fuelPrice": 4780
                            },
                            "path": [
                              {"lat": 33.5104135, "lng": 126.4913534},
                              {"lat": 33.5100244, "lng": 126.4927323}
                            ]
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 좌표/옵션/경유지",
            content = @Content(
                examples = @ExampleObject(value = """
                    {"isSuccess":false,"code":"DIRECTION400_1","message":"유효하지 않은 좌표값입니다.","result":null}
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "경로를 찾을 수 없음",
            content = @Content(
                examples = @ExampleObject(value = """
                    {"isSuccess":false,"code":"DIRECTION404_1","message":"경로를 찾을 수 없습니다.","result":null}
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "502",
            description = "네이버 지도 API 호출 실패",
            content = @Content(
                examples = @ExampleObject(value = """
                    {"isSuccess":false,"code":"DIRECTION502_1","message":"네이버 지도 API 호출에 실패했습니다.","result":null}
                    """)
            )
        )
    })
    ApiResponse<DirectionResponse> getDriving(
        @Parameter(description = "출발지 위도", example = "33.5104135", required = true)
        @RequestParam double startLat,

        @Parameter(description = "출발지 경도", example = "126.4913534", required = true)
        @RequestParam double startLng,

        @Parameter(description = "목적지 위도", example = "33.4619478", required = true)
        @RequestParam double goalLat,

        @Parameter(description = "목적지 경도", example = "126.9425362", required = true)
        @RequestParam double goalLng,

        @Parameter(description = "경유지 (위도,경도|위도,경도 형식, 최대 5개)", example = "33.4996213,126.5311884")
        @RequestParam(required = false) String waypoints,

        @Parameter(description = "경로 탐색 옵션", example = "traoptimal")
        @RequestParam(defaultValue = "traoptimal") String option
    );
}
