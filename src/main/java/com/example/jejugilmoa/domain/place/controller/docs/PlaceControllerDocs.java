package com.example.jejugilmoa.domain.place.controller.docs;

import com.example.jejugilmoa.domain.place.dto.PlaceDetailDto;
import com.example.jejugilmoa.domain.place.dto.PlaceSummaryDto;
import com.example.jejugilmoa.domain.place.dto.PopularPlaceDto;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface PlaceControllerDocs {

    @Operation(
        summary = "인기 관광지 목록",
        description = """
            방문 수 기준으로 상위 관광지를 반환합니다.

            - 홈 화면 TOP3: `limit=3`
            - 인기 장소 전체 페이지: `limit=20` (기본값)

            결과는 30분간 Redis에 캐싱됩니다. (캐시 키: `popularPlaces::{limit}`)
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
                              "placeId": 1,
                              "name": "성산일출봉",
                              "imageUrl": "https://example.com/seongsan.jpg",
                              "visitCount": 500
                            },
                            {
                              "placeId": 2,
                              "name": "한라산",
                              "imageUrl": "https://example.com/hallasan.jpg",
                              "visitCount": 480
                            }
                          ]
                        }
                        """
                )
            )
        )
    })
    ApiResponse<List<PopularPlaceDto>> getPopular(
        @Parameter(description = "반환할 최대 개수 (홈 화면: 3, 인기 장소 페이지: 20)", example = "3")
        @RequestParam(defaultValue = "20") int limit
    );

    @Operation(
        summary = "카테고리별 장소 목록",
        description = """
            등록된 관광지를 페이지네이션으로 조회합니다.

            - `category` 미입력 시 전체 장소를 반환합니다.
            - 사용 가능한 카테고리: `자연`, `음식`, `카페`, `전통시장`, `역사`, `체험`, `쇼핑`, `사진명소`
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
                            "content": [
                              {
                                "id": 1,
                                "name": "성산일출봉",
                                "address": "제주특별자치도 서귀포시",
                                "imageUrl": "https://example.com/seongsan.jpg",
                                "categoryName": "자연"
                              }
                            ],
                            "page": 0,
                            "size": 20,
                            "totalElements": 1,
                            "totalPages": 1,
                            "last": true
                          }
                        }
                        """
                )
            )
        )
    })
    ApiResponse<PageResponse<PlaceSummaryDto>> browse(
        @Parameter(description = "카테고리명 (미입력 시 전체 조회)", example = "자연")
        @RequestParam(required = false) String category,

        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") int size
    );

    @Operation(
        summary = "관광지 세부정보",
        description = """
            관광지 ID로 상세 정보를 조회합니다.

            결과는 30분간 Redis에 캐싱됩니다. (캐시 키: `placeDetail::{id}`)

            현재 `homepage`, `tel`은 TarRlteTarService1 API 미제공으로 항상 `null`입니다.
            `images`는 추후 PlaceImage 테이블 연동 예정으로 현재 빈 배열입니다.
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
                            "id": 1,
                            "name": "성산일출봉",
                            "address": "제주특별자치도 서귀포시",
                            "latitude": 33.4589,
                            "longitude": 126.9425,
                            "description": null,
                            "imageUrl": null,
                            "images": [],
                            "categoryName": "자연",
                            "homepage": null,
                            "tel": null,
                            "overview": null
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 관광지",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "PLACE404_1",
                          "message": "존재하지 않는 관광지입니다.",
                          "result": null
                        }
                        """
                )
            )
        )
    })
    ApiResponse<PlaceDetailDto> getDetail(
        @Parameter(description = "관광지 ID", example = "1")
        @PathVariable Long id
    );
}
