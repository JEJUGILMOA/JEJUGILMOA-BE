package com.example.jejugilmoa.domain.user.controller.docs;

import com.example.jejugilmoa.domain.notification.dto.DeviceTokenRegisterRequest;
import com.example.jejugilmoa.domain.user.dto.TravelPreferenceResponse;
import com.example.jejugilmoa.domain.user.dto.TravelPreferenceUpdateRequest;
import com.example.jejugilmoa.domain.user.dto.UserProfileResponse;
import com.example.jejugilmoa.domain.user.dto.UserSettingsResponse;
import com.example.jejugilmoa.domain.user.dto.UserSettingsUpdateRequest;
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
            summary = "디바이스 토큰 등록",
            description = """
                    FCM 푸시 알림을 수신할 디바이스 토큰을 등록합니다.

                    같은 deviceId로 이미 등록된 토큰이 있으면 새 토큰으로 갱신합니다.

                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 등록/갱신 성공"
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
    ApiResponse<Void> registerDeviceToken(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DeviceTokenRegisterRequest request
    );

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

    @Operation(
            summary = "여행 선호도 설정",
            description = """
                    로그인한 사용자의 여행 선호도를 설정합니다.

                    설정 항목: 선호 카테고리(자연/음식/카페/전통시장/역사/체험), 여행 스타일(여유로운/많이둘러보기/도보중심/대중교통중심)

                    아직 선호도가 설정되지 않은 사용자는 최초 요청 시 기본값으로 생성됩니다.
                    null 필드는 변경되지 않습니다.

                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "여행 선호도 설정 성공"
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
    ApiResponse<TravelPreferenceResponse> updateTravelPreferences(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody TravelPreferenceUpdateRequest request
    );

    @Operation(
            summary = "설정 조회",
            description = """
                    로그인한 사용자의 알림 및 위치 권한 설정을 조회합니다.

                    응답 항목: 일정 시작 알림, 기록 작성 알림, 뱃지 획득 알림, 다음 장소 알림, 장소 도착 알림, 마케팅 알림, 위치 권한

                    아직 설정이 없는 사용자는 조회 시 기본값으로 생성됩니다.

                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "설정 조회 성공"
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
    ApiResponse<UserSettingsResponse> getSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal
    );

    @Operation(
            summary = "설정 수정",
            description = """
                    로그인한 사용자의 알림 및 위치 권한 설정을 수정합니다.

                    null 필드는 변경되지 않습니다.

                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "설정 수정 성공"
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
    ApiResponse<UserSettingsResponse> updateSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserSettingsUpdateRequest request
    );
}
