package com.example.jejugilmoa.domain.auth.controller;

import com.example.jejugilmoa.domain.auth.controller.docs.AuthControllerDocs;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.service.AuthService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "로그인 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @Override
    @Operation(summary = "소셜 로그인", description = "프론트에서 전달한 인가코드로 카카오, 네이버, 구글 로그인을 처리합니다.")
    @PostMapping("/oauth/{provider}/login")
    public ApiResponse<OAuthLoginResponse> login(
            @PathVariable String provider,
            @Valid @RequestBody OAuthLoginRequest request
    ) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                authService.login(provider, request)
        );
    }
}
