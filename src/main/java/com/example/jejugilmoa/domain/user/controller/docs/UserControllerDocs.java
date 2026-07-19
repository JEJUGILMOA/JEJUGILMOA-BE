package com.example.jejugilmoa.domain.user.controller.docs;

import com.example.jejugilmoa.domain.user.dto.UserProfileResponse;
import com.example.jejugilmoa.domain.user.dto.UserUpdateRequest;
import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

public interface UserControllerDocs {

    @Operation(
            summary = "내 프로필 조회",
            description = """
                    로그인한 사용자의 프로필 정보를 조회합니다.

                    응답 항목: 닉네임, 프로필 이미지, 한줄 소개, 완료된 여행 수, 즐겨찾기 수, 뱃지 수, 이메일, 가입일

                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로필 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "USER404_1",
                                              "message": "사용자를 찾을 수 없습니다.",
                                              "result": null
                                            }
                                            """
                            )
                    )
            )
    })
    ApiResponse<UserProfileResponse> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal
    );

    @Operation(
            summary = "내 프로필 수정",
            description = """
                    로그인한 사용자의 프로필 정보를 수정합니다.

                    수정 가능 항목: 닉네임, 프로필 이미지, 한줄 소개
                    수정 불가 항목: 이메일 (응답에만 포함)

                    null 필드는 변경되지 않습니다.

                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로필 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "USER404_1",
                                              "message": "사용자를 찾을 수 없습니다.",
                                              "result": null
                                            }
                                            """
                            )
                    )
            )
    })
    ApiResponse<UserProfileResponse> updateMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserUpdateRequest request
    );
}
