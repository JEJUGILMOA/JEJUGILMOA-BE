package com.example.jejugilmoa.domain.auth.controller.docs;

import com.example.jejugilmoa.domain.auth.dto.AppleLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.jwt.CookieProvider;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface AuthControllerDocs {

    @Operation(summary = "iOS Apple 로그인", description = """
            Apple identityToken과 rawNonce로 로그인합니다.
            앱은 SHA-256(rawNonce)의 소문자 hex 값을 Apple 인증 요청 nonce로 지정해야 합니다.
            성공 시 기존 OAuth 응답과 ACCESS_TOKEN/REFRESH_TOKEN HttpOnly 쿠키를 반환합니다.
            nonce 재사용 방지는 1차 버전에 포함되지 않습니다.
            """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수값 또는 길이 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Apple 토큰 또는 nonce 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Apple 공개키 조회 실패")
    })
    ApiResponse<OAuthLoginResponse> appleLogin(@RequestBody AppleLoginRequest request, HttpServletResponse response);


    @Operation(
        summary = "소셜 로그인",
        description = """
            프론트엔드에서 전달한 OAuth 인가 코드를 이용하여 로그인을 수행합니다.

            지원 Provider
            - kakao
            - google
            - naver

            최초 로그인인 경우 회원가입이 자동으로 진행됩니다.
            로그인에 성공하면 액세스/리프레시 토큰이 HttpOnly 쿠키(ACCESS_TOKEN, REFRESH_TOKEN)로 발급됩니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그인 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 Provider 또는 인가 코드",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "AUTH400_1",
                          "message": "지원하지 않는 소셜 로그인 제공자입니다.",
                          "result": null
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "OAuth 인증 실패"
        )
    })
    ApiResponse<OAuthLoginResponse> login(
        @Parameter(
            description = "소셜 로그인 Provider (kakao, google, naver)",
            example = "kakao"
        )
        @PathVariable String provider,

        @RequestBody OAuthLoginRequest request,

        HttpServletResponse response
    );

    @Operation(
        summary = "로그아웃",
        description = """
            REFRESH_TOKEN 쿠키로 전달된 리프레시 토큰을 서버에서 폐기하고,
            ACCESS_TOKEN/REFRESH_TOKEN 쿠키를 모두 제거합니다.
            쿠키가 없거나 이미 무효한 토큰이어도 항상 200으로 응답합니다.

            deviceId 를 함께 전달하면 해당 기기의 FCM 디바이스 토큰도 자동으로 삭제됩니다.
            deviceId 가 없거나 등록된 토큰이 없는 경우에도 정상 응답합니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그아웃 성공"
        )
    })
    ApiResponse<Void> logout(
        @Parameter(description = "리프레시 토큰 쿠키", hidden = true)
        @CookieValue(value = CookieProvider.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,

        @Parameter(description = "FCM 토큰을 삭제할 디바이스 ID (선택)")
        @RequestParam(required = false) String deviceId,

        HttpServletResponse response
    );

    @Operation(
        summary = "토큰 재발급",
        description = """
            REFRESH_TOKEN 쿠키의 리프레시 토큰을 검증한 뒤 즉시 폐기(회전)하고,
            새 액세스/리프레시 토큰을 쿠키로 재발급합니다.

            이미 회전되어 폐기된 토큰이 다시 제시되면 탈취 의심으로 간주해
            해당 사용자의 모든 리프레시 토큰을 무효화하고 401을 반환합니다(재로그인 필요).
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "재발급 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "리프레시 토큰 없음/무효/재사용 감지",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "AUTH401_4",
                          "message": "이미 사용되었거나 탈취 의심되는 리프레시 토큰입니다. 다시 로그인해주세요.",
                          "result": null
                        }
                        """
                )
            )
        )
    })
    ApiResponse<Void> reissue(
        @Parameter(description = "리프레시 토큰 쿠키", hidden = true)
        @CookieValue(value = CookieProvider.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,

        HttpServletResponse response
    );
}
