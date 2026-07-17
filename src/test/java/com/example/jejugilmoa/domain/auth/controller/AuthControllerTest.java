package com.example.jejugilmoa.domain.auth.controller;

import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.domain.auth.jwt.CookieProvider;
import com.example.jejugilmoa.domain.auth.jwt.TokenPair;
import com.example.jejugilmoa.domain.auth.service.AuthService;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CookieProvider cookieProvider;

    @Test
    @DisplayName("소셜 로그인 성공 시 사용자 정보를 응답한다")
    void loginSuccess() throws Exception {
        OAuthLoginResponse response = new OAuthLoginResponse(
                1L,
                "제주러",
                "https://example.com/profile.png",
                Role.USER,
                true
        );
        given(authService.login(eq("kakao"), any())).willReturn(response);
        given(authService.issueTokens(eq(1L), eq(Role.USER)))
                .willReturn(new TokenPair("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/oauth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestFixture(
                                "auth-code",
                                "http://localhost:3000/oauth/kakao/callback",
                                null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result.userId").value(1L))
                .andExpect(jsonPath("$.result.nickname").value("제주러"))
                .andExpect(jsonPath("$.result.newUser").value(true));
    }

    @Test
    @DisplayName("지원하지 않는 소셜 제공자면 에러 봉투를 응답한다")
    void loginFailsWhenProviderUnsupported() throws Exception {
        given(authService.login(eq("facebook"), any()))
                .willThrow(new GeneralException(AuthErrorCode.UNSUPPORTED_PROVIDER));

        mockMvc.perform(post("/api/auth/oauth/facebook/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestFixture(
                                "auth-code",
                                "http://localhost:3000/oauth/facebook/callback",
                                null
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH400_1"))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("로그아웃 시 리프레시 토큰을 폐기하고 쿠키를 제거한다")
    void logoutSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie(CookieProvider.REFRESH_TOKEN_COOKIE, "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(authService).logout("refresh-token");
        verify(cookieProvider).clearAuthCookies(any());
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없어도 로그아웃은 성공한다")
    void logoutSuccessWithoutCookie() throws Exception {
        mockMvc.perform(post("/api//auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(authService).logout(isNull());
    }

    @Test
    @DisplayName("리프레시 토큰이 유효하면 새 토큰 쌍을 발급하고 쿠키를 재설정한다")
    void reissueSuccess() throws Exception {
        given(authService.reissue("refresh-token"))
                .willReturn(new TokenPair("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie(CookieProvider.REFRESH_TOKEN_COOKIE, "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(cookieProvider).addAccessTokenCookie(any(), eq("new-access-token"));
        verify(cookieProvider).addRefreshTokenCookie(any(), eq("new-refresh-token"));
    }

    @Test
    @DisplayName("재사용된 리프레시 토큰이면 에러 봉투를 응답한다")
    void reissueFailsWhenTokenReused() throws Exception {
        given(authService.reissue("stolen-token"))
                .willThrow(new GeneralException(AuthErrorCode.REFRESH_TOKEN_REUSED));

        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie(CookieProvider.REFRESH_TOKEN_COOKIE, "stolen-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH401_4"));
    }

    private record LoginRequestFixture(
            String authorizationCode,
            String redirectUri,
            String state
    ) {
    }
}
