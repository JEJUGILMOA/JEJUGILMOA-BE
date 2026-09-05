package com.example.jejugilmoa.domain.auth.controller;

import com.example.jejugilmoa.domain.auth.dto.AppleLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.domain.auth.jwt.CookieProvider;
import com.example.jejugilmoa.domain.auth.jwt.JwtProperties;
import com.example.jejugilmoa.domain.auth.jwt.TokenPair;
import com.example.jejugilmoa.domain.auth.service.AppleAuthService;
import com.example.jejugilmoa.domain.auth.service.AuthService;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.example.jejugilmoa.domain.auth.support.AppleTokenFixture.NONCE;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AppleAuthControllerTest.CookieConfig.class)
class AppleAuthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService auth;
    @MockitoBean AppleAuthService apple;
    ObjectMapper mapper = new ObjectMapper();

    @TestConfiguration
    static class CookieConfig {
        @Bean CookieProvider cookieProvider() {
            return new CookieProvider(new JwtProperties("unused", 1800000, 1209600000, true));
        }
    }

    @Test void respondsWithExistingEnvelopeAndRealHttpOnlyCookies() throws Exception {
        when(apple.login(any())).thenReturn(new OAuthLoginResponse(1L, "애플 사용자", null, Role.USER, true));
        when(auth.issueTokens(1L, Role.USER)).thenReturn(new TokenPair("access", "refresh"));
        mvc.perform(post("/api/auth/apple/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AppleLoginRequest("token", NONCE))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.userId").value(1))
                .andExpect(jsonPath("$.result.newUser").value(true))
                .andExpect(jsonPath("$.result.role").value("USER"))
                .andExpect(cookie().value("ACCESS_TOKEN", "access"))
                .andExpect(cookie().value("REFRESH_TOKEN", "refresh"))
                .andExpect(cookie().httpOnly("ACCESS_TOKEN", true))
                .andExpect(cookie().httpOnly("REFRESH_TOKEN", true))
                .andExpect(cookie().secure("ACCESS_TOKEN", true))
                .andExpect(cookie().secure("REFRESH_TOKEN", true))
                .andExpect(cookie().path("REFRESH_TOKEN", "/"));
        verify(apple).login(new AppleLoginRequest("token", NONCE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"identityToken\":\"token\"}",
            "{\"identityToken\":\"token\",\"rawNonce\":\"short\"}"})
    void rejectsMissingOrShortInput(String body) throws Exception {
        mvc.perform(post("/api/auth/apple/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false));
        verifyNoInteractions(apple, auth);
    }

    @Test void rejectsOversizedInput() throws Exception {
        for (var request : new AppleLoginRequest[] {
                new AppleLoginRequest("t".repeat(16385), NONCE),
                new AppleLoginRequest("token", "n".repeat(257))}) {
            mvc.perform(post("/api/auth/apple/login").contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()).andExpect(header().doesNotExist("Set-Cookie"));
        }
        verifyNoInteractions(apple, auth);
    }

    @Test void verificationFailureNeverIssuesCookiesOrServiceTokens() throws Exception {
        when(apple.login(any())).thenThrow(new GeneralException(AuthErrorCode.INVALID_APPLE_NONCE));
        mvc.perform(post("/api/auth/apple/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AppleLoginRequest("token", NONCE))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH401_7"))
                .andExpect(header().doesNotExist("Set-Cookie"));
        verifyNoInteractions(auth);
    }
}
