package com.example.jejugilmoa.domain.imageupload.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import com.example.jejugilmoa.domain.imageupload.dto.ImageUploadRequest;
import com.example.jejugilmoa.domain.imageupload.dto.ImageUploadResponse;
import com.example.jejugilmoa.domain.imageupload.exception.ImageUploadErrorCode;
import com.example.jejugilmoa.domain.imageupload.service.ImageUploadService;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImageUploadController.class)
@Import(com.example.jejugilmoa.global.config.SecurityConfig.class)
@EnableWebSecurity
class ImageUploadControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ImageUploadService imageUploadService;
    @MockitoBean JwtProvider jwtProvider;

    @Test
    void createUploadUrl_returnsWrappedResponseForAuthenticatedUser() throws Exception {
        var principal = new UserPrincipal(42L, Role.USER);
        var auth = UsernamePasswordAuthenticationToken.authenticated(principal, null, java.util.List.of());
        var request = new ImageUploadRequest("image/jpeg", 3_145_728L);
        given(imageUploadService.createUploadUrl(eq(request), eq(42L))).willReturn(new ImageUploadResponse(
                "records/42/id.jpg", "https://s3.example/upload", "PUT",
                Map.of("Content-Type", "image/jpeg"), Instant.parse("2026-08-02T00:10:00Z")));

        mockMvc.perform(post("/api/image-uploads")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/jpeg\",\"fileSize\":3145728}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result.httpMethod").value("PUT"))
                .andExpect(jsonPath("$.result.requiredHeaders.Content-Type").value("image/jpeg"));
    }

    @Test
    void createUploadUrl_returnsDomainErrorEnvelope() throws Exception {
        var principal = new UserPrincipal(42L, Role.USER);
        var auth = UsernamePasswordAuthenticationToken.authenticated(principal, null, java.util.List.of());
        given(imageUploadService.createUploadUrl(new ImageUploadRequest("image/gif", 1L), 42L))
                .willThrow(new GeneralException(ImageUploadErrorCode.UNSUPPORTED_CONTENT_TYPE));

        mockMvc.perform(post("/api/image-uploads")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/gif\",\"fileSize\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("IMAGE400_1"));
    }

    @Test
    void createUploadUrl_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/image-uploads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/jpeg\",\"fileSize\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_1"));
    }
}
