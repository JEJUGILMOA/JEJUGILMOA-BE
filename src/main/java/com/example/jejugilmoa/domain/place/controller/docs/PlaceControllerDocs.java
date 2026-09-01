package com.example.jejugilmoa.domain.place.controller.docs;

import com.example.jejugilmoa.domain.place.dto.PlaceDetailDto;
import com.example.jejugilmoa.domain.place.dto.PlaceSummaryDto;
import com.example.jejugilmoa.domain.place.dto.PlaceSyncResponse;
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

public interface PlaceControllerDocs {

    @Operation(
        summary = "인기 관광지 목록",
        description = """
            방문 수 기준으로 상위 관광지를 페이지네이션으로 반환합니다.

            - 카테고리 미입력 시 전체 인기 장소 조회
            - 카테고리 입력 시 해당 카테고리 내 인기 장소 조회

            **사용 가능한 카테고리**: `자연`, `음식`, `카페`, `전통시장`, `역사`, `체험`
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
                                "placeId": 1,
                                "name": "성산일출봉",
                                "imageUrl": "https://example.com/seongsan.jpg",
                                "visitCount": 500,
                                "region": "서귀포시 성산읍",
                                "hashtags": ["자연"],
                                "imageUrls": ["https://example.com/img1.jpg"]
                              }
                            ],
                            "page": 0,
                            "size": 20,
                            "totalElements": 120,
                            "totalPages": 6,
                            "last": false
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 페이지 파라미터",
            content = @Content(
                examples = @ExampleObject(value = """
                    {"isSuccess":false,"code":"PLACE400_2","message":"페이지 번호는 0 이상이어야 합니다.","result":null}
                    """)
            )
        )
    })
    ApiResponse<PageResponse<PopularPlaceDto>> getPopular(
        @Parameter(description = "카테고리 필터 (미입력 시 전체 조회, 예: 자연·음식·카페·전통시장·역사·체험)", example = "자연")
        @RequestParam(required = false) String category,

        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "페이지 크기 (1~100)", example = "20")
        @RequestParam(defaultValue = "20") int size
    );

    @Operation(
        summary = "장소 검색 / 카테고리 목록",
        description = """
            등록된 관광지를 키워드 검색 또는 카테고리 필터로 조회합니다.

            **파라미터 조합**
            - `keyword`만 입력 → 장소명·주소에서 키워드 포함 검색
            - `category`만 입력 → 해당 카테고리 전체 조회
            - `keyword` + `category` → 카테고리 내 키워드 검색
            - 둘 다 미입력 → 전체 장소 조회

            **검색 대상 필드**: 장소명(`name`), 주소(`address`) — 대소문자 무시

            **사용 가능한 카테고리**: `자연`, `음식`, `카페`, `전통시장`, `역사`, `체험`, `쇼핑`, `사진명소`
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                examples = {
                    @ExampleObject(
                        name = "키워드 검색 결과",
                        summary = "keyword=애월 검색 시",
                        value = """
                            {
                              "isSuccess": true,
                              "code": "FOUND200",
                              "message": "조회에 성공했습니다.",
                              "result": {
                                "content": [
                                  {
                                    "id": 10,
                                    "name": "애월 카페거리",
                                    "address": "제주특별자치도 제주시 애월읍",
                                    "imageUrl": "https://example.com/aewol.jpg",
                                    "categoryName": "카페"
                                  },
                                  {
                                    "id": 11,
                                    "name": "애월해안도로",
                                    "address": "제주특별자치도 제주시 애월읍 애월리",
                                    "imageUrl": null,
                                    "categoryName": "자연"
                                  }
                                ],
                                "page": 0,
                                "size": 20,
                                "totalElements": 2,
                                "totalPages": 1,
                                "last": true
                              }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "카테고리 필터 결과",
                        summary = "category=자연 필터 시",
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
                                    "address": "제주특별자치도 서귀포시 성산읍",
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
                }
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 페이지 파라미터",
            content = @Content(
                examples = @ExampleObject(value = """
                    {"isSuccess":false,"code":"PLACE400_2","message":"페이지 번호는 0 이상이어야 합니다.","result":null}
                    """)
            )
        )
    })
    ApiResponse<PageResponse<PlaceSummaryDto>> browse(
        @Parameter(description = "검색 키워드 — 장소명·주소 대상 (미입력 시 전체 조회)", example = "애월")
        @RequestParam(required = false) String keyword,

        @Parameter(description = "카테고리명 필터 (미입력 시 전체 조회)", example = "자연")
        @RequestParam(required = false) String category,

        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "페이지 크기 (1~100)", example = "20")
        @RequestParam(defaultValue = "20") int size
    );

    @Operation(
        summary = "관광지 세부정보",
        description = """
            관광지 ID로 상세 정보를 조회합니다.

            결과는 30분간 Redis에 캐싱됩니다. (캐시 키: `placeDetail::{id}`)

            `images`에는 DB에 저장된 갤러리 이미지 URL 목록이 포함됩니다.
            이미지가 없거나 3장 미만이면 TourAPI를 통해 자동 보강 후 반환합니다.
            `overview`가 없으면 TourAPI detailCommon2를 호출해 실시간 보강합니다.
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
                            "imageUrl": "https://example.com/seongsan.jpg",
                            "images": [
                              "https://example.com/img1.jpg",
                              "https://example.com/img2.jpg"
                            ],
                            "categoryName": "자연",
                            "overview": "성산일출봉은 제주도 동쪽 끝..."
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

    @Operation(
        summary = "TourAPI 장소 동기화 (개발용)",
        description = """
            TourAPI에서 제주시·서귀포시 관광지 데이터를 동기화합니다.
            DB에 장소 데이터가 없을 때 추천 API 테스트를 위해 사용합니다.

            - 시군구별 성공/실패 결과를 반환합니다.
            - 모든 시군구가 실패하면 `PLACE503_1` 오류를 반환합니다.
            - 일부 실패 시에도 성공한 결과와 함께 200을 반환합니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "동기화 완료 (부분 실패 포함)",
            content = @Content(
                examples = {
                    @ExampleObject(
                        name = "전체 성공",
                        value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 처리했습니다.",
                              "result": {
                                "succeededCount": 2,
                                "failedCount": 0,
                                "results": [
                                  {"signguCd": "11", "succeeded": true, "errorMessage": null},
                                  {"signguCd": "13", "succeeded": true, "errorMessage": null}
                                ]
                              }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "부분 실패",
                        value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공적으로 요청을 처리했습니다.",
                              "result": {
                                "succeededCount": 1,
                                "failedCount": 1,
                                "results": [
                                  {"signguCd": "11", "succeeded": true, "errorMessage": null},
                                  {"signguCd": "13", "succeeded": false, "errorMessage": "TourAPI 호출 오류"}
                                ]
                              }
                            }
                            """
                    )
                }
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "503",
            description = "전체 시군구 동기화 실패",
            content = @Content(
                examples = @ExampleObject(value = """
                    {"isSuccess":false,"code":"PLACE503_1","message":"모든 시군구 동기화에 실패했습니다.","result":null}
                    """)
            )
        )
    })
    ApiResponse<PlaceSyncResponse> syncPlaces();
}
