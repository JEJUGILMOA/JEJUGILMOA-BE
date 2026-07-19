package com.example.jejugilmoa.domain.auth.controller;

import com.example.jejugilmoa.domain.auth.dto.DevAuthResponse;
import com.example.jejugilmoa.domain.auth.dto.DevLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.DevSignUpRequest;
import com.example.jejugilmoa.domain.auth.jwt.CookieProvider;
import com.example.jejugilmoa.domain.auth.jwt.TokenPair;
import com.example.jejugilmoa.domain.auth.service.AuthService;
import com.example.jejugilmoa.domain.auth.service.DevAuthService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!prod")
@Tag(name = "[개발전용] 인증", description = "개발/테스트 환경 전용 이메일 기반 회원가입/로그인 API")
@RestController
@RequestMapping("/dev/auth")
@RequiredArgsConstructor
public class DevAuthController {

    private final DevAuthService devAuthService;
    private final AuthService authService;
    private final CookieProvider cookieProvider;

    @Operation(summary = "[개발전용] 회원가입", description = "이메일과 닉네임으로 개발용 계정을 생성합니다. OAuth 없이 바로 가입됩니다. 응답의 accessToken을 Swagger Authorize에 붙여넣어 바로 인증할 수 있습니다.")
    @PostMapping("/signup")
    public ApiResponse<DevAuthResponse> signUp(
            @Valid @RequestBody DevSignUpRequest request,
            HttpServletResponse response
    ) {
        DevAuthResponse partial = devAuthService.signUp(request);
        TokenPair tokens = authService.issueTokens(partial.userId(), partial.role());
        cookieProvider.addAccessTokenCookie(response, tokens.accessToken());
        cookieProvider.addRefreshTokenCookie(response, tokens.refreshToken());
        DevAuthResponse authResponse = new DevAuthResponse(
                partial.userId(), partial.email(), partial.nickname(), partial.role(), partial.newUser(),
                tokens.accessToken()
        );
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, authResponse);
    }

    @Operation(summary = "[개발전용] 로그인", description = "가입된 이메일로 개발용 로그인을 합니다. 비밀번호 없이 이메일만 사용합니다. 응답의 accessToken을 Swagger Authorize에 붙여넣어 바로 인증할 수 있습니다.")
    @PostMapping("/login")
    public ApiResponse<DevAuthResponse> login(
            @Valid @RequestBody DevLoginRequest request,
            HttpServletResponse response
    ) {
        DevAuthResponse partial = devAuthService.login(request);
        TokenPair tokens = authService.issueTokens(partial.userId(), partial.role());
        cookieProvider.addAccessTokenCookie(response, tokens.accessToken());
        cookieProvider.addRefreshTokenCookie(response, tokens.refreshToken());
        DevAuthResponse authResponse = new DevAuthResponse(
                partial.userId(), partial.email(), partial.nickname(), partial.role(), partial.newUser(),
                tokens.accessToken()
        );
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, authResponse);
    }
}
