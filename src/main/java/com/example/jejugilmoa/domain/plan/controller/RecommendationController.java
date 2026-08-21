package com.example.jejugilmoa.domain.plan.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.dto.RecommendationRequest;
import com.example.jejugilmoa.domain.plan.dto.RecommendationResponse;
import com.example.jejugilmoa.domain.plan.service.RecommendationService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "장소 추천", description = "경유지 추천 (stateless — planId 없이 호출)")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "경유지 추천 (Stateless)",
            description = """
                    planId 없이 좌표만으로 경유지를 추천합니다. 최대 10개를 반환합니다.

                    **추천 모드**
                    - `preferredWaypoints` 비어 있음 → 전역 랜덤 추천 (DB에서 RANDOM 정렬)
                    - `preferredWaypoints` 있음 → 앵커 부채꼴 추천 (출발지→선호경유지 방향 부채꼴 범위 내 DB 조회)
                      - DB 결과 없으면 TourAPI locationBasedList2 폴백

                    **카테고리 필터 (`category`)**
                    생략하거나 null이면 전체 카테고리를 대상으로 추천합니다.
                    사용 가능한 값: `FOOD`(음식·맛집), `CAFE`(카페·디저트), `NATURE`(자연), `CULTURE`(역사·문화), `ACTIVITY`(체험·액티비티), `FESTIVAL`(축제·이벤트), `SHOPPING`(쇼핑)
                    단, `CAFE`는 TourAPI 카테고리 매핑이 없어 DB 결과가 없으면 빈 배열(`hasMore=false`)을 반환합니다.

                    **응답 필드**
                    - DB 장소: `placeId` 값 있음, `contentId` null
                    - TourAPI 폴백: `placeId` null, `contentId` 값 있음

                    **새로고침 방법**
                    - 이전 결과의 `placeId`(DB) → `excludedPlaceIds`에 추가
                    - 이전 결과의 `contentId`(폴백) → `excludeContentIds`에 추가
                    - `hasMore=false`이면 더 이상 새 결과 없음 (프론트에서 직접 POI 입력 유도)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "추천 성공",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "DB 장소 추천 결과",
                                    summary = "preferredWaypoints 있고 DB에 결과 있을 때",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공적으로 요청을 처리했습니다.",
                                              "result": {
                                                "items": [
                                                  {
                                                    "placeId": 42,
                                                    "contentId": null,
                                                    "name": "성산일출봉",
                                                    "categoryName": "자연",
                                                    "imageUrl": "https://cdn.example.com/seongsan.jpg",
                                                    "address": "제주특별자치도 서귀포시 성산읍",
                                                    "latitude": 33.4589,
                                                    "longitude": 126.9425
                                                  }
                                                ],
                                                "hasMore": false
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "TourAPI 폴백 결과",
                                    summary = "DB 결과 없어 TourAPI로 폴백됐을 때",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공적으로 요청을 처리했습니다.",
                                              "result": {
                                                "items": [
                                                  {
                                                    "placeId": null,
                                                    "contentId": "126508",
                                                    "name": "한림공원",
                                                    "categoryName": null,
                                                    "imageUrl": "https://tong.visitkorea.or.kr/cms/resource/49/1854849_image2_1.jpg",
                                                    "address": "제주특별자치도 제주시 한림읍 한림로 300",
                                                    "latitude": 33.3942,
                                                    "longitude": 126.2526
                                                  }
                                                ],
                                                "hasMore": true
                                              }
                                            }
                                            """
                            )
                    })
            )
    })
    @PostMapping
    public ApiResponse<RecommendationResponse> recommend(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RecommendationRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                recommendationService.recommendStateless(request));
    }
}
