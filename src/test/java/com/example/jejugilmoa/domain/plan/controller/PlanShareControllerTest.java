package com.example.jejugilmoa.domain.plan.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.dto.ShareLinkResponse;
import com.example.jejugilmoa.domain.plan.dto.SharedPlanResponse;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.service.PlanShareService;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.config.SecurityConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlanShareController.class)
@Import(SecurityConfig.class)
@EnableWebSecurity
class PlanShareControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PlanShareService planShareService;
    @MockitoBean JwtProvider jwtProvider;

    @Test
    void authenticatedOwnerCreatesShareLink() throws Exception {
        given(planShareService.createShareLink(1L, 42L)).willReturn(new ShareLinkResponse(
                1L, "token", Instant.parse("2026-09-03T00:00:00Z")));

        mockMvc.perform(post("/api/plans/1/share-link").with(authentication(authenticationFor(42L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.result.planId").value(1))
                .andExpect(jsonPath("$.result.shareToken").value("token"));
    }

    @Test
    void shareLinkCreationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/plans/1/share-link"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH401_1"));
    }

    @Test
    void anonymousUserCanReadSharedPlan() throws Exception {
        given(planShareService.getSharedPlan("token")).willReturn(new SharedPlanResponse(
                1L, "공유 계획", LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11),
                "출발지", List.of(), null, null, 30_000, null, 30_000));

        mockMvc.perform(get("/api/shared/plans/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result.title").value("공유 계획"))
                .andExpect(jsonPath("$.result.userId").doesNotExist())
                .andExpect(jsonPath("$.result.email").doesNotExist());
    }

    @Test
    void invalidTokenReturnsDomainErrorEnvelopeWithoutAuthentication() throws Exception {
        given(planShareService.getSharedPlan("invalid"))
                .willThrow(new GeneralException(PlanErrorCode.SHARE_TOKEN_NOT_FOUND));

        mockMvc.perform(get("/api/shared/plans/invalid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("PLAN404_4"));
    }

    @Test
    void otherPlanApisRemainAuthenticated() throws Exception {
        mockMvc.perform(get("/api/plans/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/shared/records/token"))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken authenticationFor(Long userId) {
        UserPrincipal principal = new UserPrincipal(userId, Role.USER);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }
}
