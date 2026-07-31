package com.example.jejugilmoa.domain.badge.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.badge.dto.BadgeGroupResponse;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

public interface BadgeControllerDocs {

    @Operation(summary = "내 배지 조회", description = """
            로그인한 사용자의 전체 배지 목록을 화면 표시 그룹(탐험/미식/소셜) 별로 반환합니다.

            - 이미 획득한 배지는 `acquired=true`와 `acquiredAt`이 채워집니다.
            - 미획득 배지는 `currentProgress`/`targetProgress`로 달성 진행도를 확인할 수 있습니다.
            - `HIDDEN` 타입 배지는 획득 전까지 이름/설명/이미지/진행도가 가려집니다.
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "배지 목록 조회 성공",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "성공적으로 요청을 처리했습니다.",
                                      "result": [
                                        {
                                          "group": "EXPLORATION",
                                          "badges": [
                                            {
                                              "badgeId": 1,
                                              "name": "제주 한바퀴",
                                              "description": "제주 전 지역을 방문해보세요.",
                                              "imageUrl": "https://cdn.example.com/badges/1.png",
                                              "acquired": false,
                                              "acquiredAt": null,
                                              "currentProgress": 2,
                                              "targetProgress": 5
                                            }
                                          ]
                                        },
                                        {
                                          "group": "GOURMET",
                                          "badges": []
                                        },
                                        {
                                          "group": "SOCIAL",
                                          "badges": [
                                            {
                                              "badgeId": 7,
                                              "name": "???",
                                              "description": "조건을 달성하면 공개되는 히든 배지입니다.",
                                              "imageUrl": null,
                                              "acquired": false,
                                              "acquiredAt": null,
                                              "currentProgress": null,
                                              "targetProgress": null
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                    """)
                    )
            )
    })
    ApiResponse<List<BadgeGroupResponse>> getMyBadges(
            @AuthenticationPrincipal UserPrincipal principal
    );
}
