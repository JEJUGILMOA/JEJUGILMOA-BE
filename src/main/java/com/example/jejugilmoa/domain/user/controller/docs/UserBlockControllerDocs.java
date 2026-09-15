package com.example.jejugilmoa.domain.user.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.user.dto.BlockedUserResponse;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface UserBlockControllerDocs {

    @Operation(
            summary = "사용자 차단",
            description = """
                    특정 사용자를 차단합니다.

                    차단된 사용자의 콘텐츠(여행 기록, 여행 계획 등)는 피드에서 노출되지 않습니다.

                    자기 자신은 차단할 수 없습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "차단 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "자기 자신을 차단하려는 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "USER400_3",
                                              "message": "자기 자신을 차단할 수 없습니다.",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "대상 사용자를 찾을 수 없음",
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 차단한 사용자",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "USER409_1",
                                              "message": "이미 차단한 사용자입니다.",
                                              "result": null
                                            }
                                            """
                            )
                    )
            )
    })
    ApiResponse<Void> blockUser(
            @Parameter(description = "차단할 사용자 ID") @PathVariable Long targetUserId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal
    );

    @Operation(
            summary = "사용자 차단 해제",
            description = """
                    차단한 사용자를 해제합니다.

                    차단 해제 후 해당 사용자의 콘텐츠가 다시 노출됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "차단 해제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "차단 내역을 찾을 수 없음",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "USER404_2",
                                              "message": "차단 내역을 찾을 수 없습니다.",
                                              "result": null
                                            }
                                            """
                            )
                    )
            )
    })
    ApiResponse<Void> unblockUser(
            @Parameter(description = "차단 해제할 사용자 ID") @PathVariable Long targetUserId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal
    );

    @Operation(
            summary = "차단 목록 조회",
            description = """
                    내가 차단한 사용자 목록을 차단 최신순으로 반환합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "차단 목록 조회 성공"
            )
    })
    ApiResponse<List<BlockedUserResponse>> getBlockedUsers(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal
    );
}
