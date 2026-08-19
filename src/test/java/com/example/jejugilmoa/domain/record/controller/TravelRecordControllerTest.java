package com.example.jejugilmoa.domain.record.controller;

import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateResponse;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.service.TravelRecordService;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TravelRecordController.class)
@Import(SecurityConfig.class)
@EnableWebSecurity
class TravelRecordControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean TravelRecordService travelRecordService;
    @MockitoBean JwtProvider jwtProvider;

    @Test
    void authenticatedUserCreatesRecord() throws Exception {
        given(travelRecordService.create(eq(42L), any())).willReturn(new TravelRecordCreateResponse(
                77L, 10L, "여행 기록", Visibility.PRIVATE, Instant.parse("2026-08-12T03:00:00Z")));

        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("""
                                {"tripId":10,"title":"여행 기록","imageObjectKeys":[]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.result.recordId").value(77))
                .andExpect(jsonPath("$.result.tripId").value(10))
                .andExpect(jsonPath("$.result.visibility").value("PRIVATE"));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/records")
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\"여행 기록\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_1"));
    }

    @Test
    void duplicateRecordReturnsConflictEnvelope() throws Exception {
        given(travelRecordService.create(eq(42L), any()))
                .willThrow(new GeneralException(RecordErrorCode.RECORD_ALREADY_EXISTS));

        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\"여행 기록\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("RECORD409_1"));
    }

    @Test
    void invalidRequestReturnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));
    }

    @Test
    void blankPlaceImageObjectKeyReturnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("""
                                {"tripId":10,"title":"여행 기록","placeMemos":[
                                  {"travelCourseId":101,"memo":"메모","imageObjectKey":"   "}
                                ]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));
    }

    @Test
    void titleWith50CharactersIsAccepted() throws Exception {
        stubSuccessfulCreate();

        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\"%s\"}".formatted("가".repeat(50))))
                .andExpect(status().isCreated());
    }

    @Test
    void titleWith51CharactersIsRejected() throws Exception {
        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\"%s\"}".formatted("가".repeat(51))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeMemoWith1000CharactersIsAccepted() throws Exception {
        stubSuccessfulCreate();

        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("""
                                {"tripId":10,"title":"기록","placeMemos":[
                                  {"travelCourseId":101,"memo":"%s"}
                                ]}
                                """.formatted("가".repeat(1000))))
                .andExpect(status().isCreated());
    }

    @Test
    void placeMemoWith1001CharactersIsRejected() throws Exception {
        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("""
                                {"tripId":10,"title":"기록","placeMemos":[
                                  {"travelCourseId":101,"memo":"%s"}
                                ]}
                                """.formatted("가".repeat(1001))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void imageObjectKeyWith500CharactersIsAccepted() throws Exception {
        stubSuccessfulCreate();

        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\"기록\",\"imageObjectKeys\":[\"%s\"]}"
                                .formatted("k".repeat(500))))
                .andExpect(status().isCreated());
    }

    @Test
    void imageObjectKeyWith501CharactersIsRejected() throws Exception {
        mockMvc.perform(post("/api/records")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"tripId\":10,\"title\":\"기록\",\"imageObjectKeys\":[\"%s\"]}"
                                .formatted("k".repeat(501))))
                .andExpect(status().isBadRequest());
    }

    private void stubSuccessfulCreate() {
        given(travelRecordService.create(eq(42L), any())).willReturn(new TravelRecordCreateResponse(
                77L, 10L, "여행 기록", Visibility.PRIVATE, Instant.parse("2026-08-12T03:00:00Z")));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(Long userId) {
        UserPrincipal principal = new UserPrincipal(userId, Role.USER);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }
}
