package com.example.jejugilmoa.domain.auth.controller;

import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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

        mockMvc.perform(post("/api/v1/auth/oauth/kakao/login")
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

        mockMvc.perform(post("/api/v1/auth/oauth/facebook/login")
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

    private record LoginRequestFixture(
            String authorizationCode,
            String redirectUri,
            String state
    ) {
    }
}
