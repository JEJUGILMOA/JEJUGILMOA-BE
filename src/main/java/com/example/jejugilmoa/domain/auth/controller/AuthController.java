package com.example.jejugilmoa.domain.auth.controller;

import com.example.jejugilmoa.domain.auth.controller.docs.AuthControllerDocs;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.jwt.CookieProvider;
import com.example.jejugilmoa.domain.auth.jwt.TokenPair;
import com.example.jejugilmoa.domain.auth.service.AuthService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "로그인/로그아웃/토큰 재발급 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;
    private final CookieProvider cookieProvider;

    @Override
    @Operation(summary = "소셜 로그인", description = "프론트에서 전달한 인가코드로 카카오, 네이버, 구글 로그인을 처리합니다.")
    @PostMapping("/oauth/{provider}/login")
    public ApiResponse<OAuthLoginResponse> login(
            @PathVariable String provider,
            @Valid @RequestBody OAuthLoginRequest request,
            HttpServletResponse response
    ) {
        OAuthLoginResponse loginResponse = authService.login(provider, request);
        TokenPair tokens = authService.issueTokens(loginResponse.userId(), loginResponse.role());
        cookieProvider.addAccessTokenCookie(response, tokens.accessToken());
        cookieProvider.addRefreshTokenCookie(response, tokens.refreshToken());

        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, loginResponse);
    }

    @Override
    @Operation(summary = "로그아웃", description = "리프레시 토큰을 폐기하고 인증 쿠키를 제거합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(value = CookieProvider.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            @RequestParam(required = false) String deviceId,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken, deviceId);
        cookieProvider.clearAuthCookies(response);

        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @Override
    @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 검증하고 회전시켜 새 액세스/리프레시 토큰을 쿠키로 발급합니다.")
    @PostMapping("/reissue")
    public ApiResponse<Void> reissue(
            @CookieValue(value = CookieProvider.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        TokenPair tokens = authService.reissue(refreshToken);
        cookieProvider.addAccessTokenCookie(response, tokens.accessToken());
        cookieProvider.addRefreshTokenCookie(response, tokens.refreshToken());

        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}
