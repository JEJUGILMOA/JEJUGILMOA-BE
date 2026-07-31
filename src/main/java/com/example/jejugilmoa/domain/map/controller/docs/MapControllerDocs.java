package com.example.jejugilmoa.domain.map.controller.docs;

import com.example.jejugilmoa.domain.map.dto.HeatmapCellDto;
import com.example.jejugilmoa.domain.map.dto.MapPlaceDto;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface MapControllerDocs {

    @Operation(
        summary = "지도 영역 내 장소 조회",
        description = """
            지도 뷰포트(minLat/maxLat/minLng/maxLng)에 포함된 장소를 마커용으로 반환합니다.

            - `category` 미입력 시 전체 카테고리를 반환합니다.
            - 결과는 방문자 수(visitorCount) 내림차순으로 정렬 후 `limit`만큼 잘립니다. (기본 200, 최대 500)
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
                          "result": [
                            {
                              "id": 1,
                              "name": "성산일출봉",
                              "categoryName": "자연",
                              "imageUrl": "https://example.com/seongsan.jpg",
                              "latitude": 33.4589,
                              "longitude": 126.9425
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "유효하지 않은 지도 영역 또는 조회 한도",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "MAP400_1",
                          "message": "유효하지 않은 지도 영역입니다.",
                          "result": null
                        }
                        """
                )
            )
        )
    })
    ApiResponse<List<MapPlaceDto>> getPlaces(
        @Parameter(description = "최소 위도", example = "33.2") @RequestParam double minLat,
        @Parameter(description = "최대 위도", example = "33.6") @RequestParam double maxLat,
        @Parameter(description = "최소 경도", example = "126.1") @RequestParam double minLng,
        @Parameter(description = "최대 경도", example = "126.9") @RequestParam double maxLng,
        @Parameter(description = "카테고리명 (미입력 시 전체 조회)", example = "자연") @RequestParam(required = false) String category,
        @Parameter(description = "최대 반환 개수 (기본 200, 최대 500)", example = "200") @RequestParam(defaultValue = "200") int limit
    );

    @Operation(
        summary = "인기 지역 혼잡도 히트맵",
        description = """
            지도 뷰포트를 gridSize x gridSize 격자로 나눠 혼잡도를 집계합니다.

            - 혼잡도는 최근 90일 실제 방문 기록(TravelRecordPlace)을 우선 사용하고,
              방문 기록이 없는 격자는 인기 지표(PopularPlace.visitCount)로 보완합니다.
            - 혼잡도가 낮은 격자는 응답에서 제외됩니다 (CROWDED/MODERATE 2단계만 반환).
            - `intensity`(0~1)는 원의 크기·투명도 표현에 사용하세요.
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
                          "result": [
                            {
                              "latitude": 33.45,
                              "longitude": 126.92,
                              "level": "CROWDED",
                              "intensity": 1.0
                            },
                            {
                              "latitude": 33.48,
                              "longitude": 126.55,
                              "level": "MODERATE",
                              "intensity": 0.3
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "유효하지 않은 지도 영역 또는 격자 크기",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "MAP400_2",
                          "message": "격자 크기는 1 이상 20 이하이어야 합니다.",
                          "result": null
                        }
                        """
                )
            )
        )
    })
    ApiResponse<List<HeatmapCellDto>> getHeatmap(
        @Parameter(description = "최소 위도", example = "33.2") @RequestParam double minLat,
        @Parameter(description = "최대 위도", example = "33.6") @RequestParam double maxLat,
        @Parameter(description = "최소 경도", example = "126.1") @RequestParam double minLng,
        @Parameter(description = "최대 경도", example = "126.9") @RequestParam double maxLng,
        @Parameter(description = "격자 크기 (한 변당 셀 개수, 기본 10, 최대 20)", example = "10") @RequestParam(defaultValue = "10") int gridSize
    );
}
