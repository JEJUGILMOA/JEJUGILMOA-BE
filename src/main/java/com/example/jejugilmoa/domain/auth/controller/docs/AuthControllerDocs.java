package com.example.jejugilmoa.domain.auth.controller.docs;

import com.example.jejugilmoa.domain.auth.dto.OAuthLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthControllerDocs {

    @Operation(
        summary = "소셜 로그인",
        description = """
            프론트엔드에서 전달한 OAuth 인가 코드를 이용하여 로그인을 수행합니다.

            지원 Provider
            - kakao
            - google
            - naver

            최초 로그인인 경우 회원가입이 자동으로 진행됩니다.
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

        @RequestBody OAuthLoginRequest request
    );
}
