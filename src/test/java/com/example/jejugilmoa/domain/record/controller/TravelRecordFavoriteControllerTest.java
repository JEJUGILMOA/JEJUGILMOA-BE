package com.example.jejugilmoa.domain.record.controller;

import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordUpdateResponse;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.service.TravelRecordFavoriteService;
import com.example.jejugilmoa.domain.record.service.TravelRecordQueryService;
import com.example.jejugilmoa.domain.record.service.TravelRecordReactionService;
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
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentCaptor.forClass;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TravelRecordFavoriteController.class)
@Import(SecurityConfig.class)
@EnableWebSecurity
class TravelRecordFavoriteControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean TravelRecordFavoriteService service;
    @MockitoBean JwtProvider jwtProvider;

    @Test
    void creationReturns201AndDeleteReturns200() throws Exception {
        mockMvc.perform(post("/api/records/77/favorites").with(authentication(auth())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("COMMON201"));
        verify(service).add(42L, 77L);
        mockMvc.perform(delete("/api/records/77/favorites").with(authentication(auth())))
                .andExpect(status().isOk());
        verify(service).delete(42L, 77L);
    }

    @Test
    void allEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/records/77/favorites")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/records/77/favorites")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/records/favorites")).andExpect(status().isUnauthorized());
    }

    @Test
    void listUsesDefaultsAndCardEnvelope() throws Exception {
        given(service.list(eq(42L), any())).willReturn(
                new com.example.jejugilmoa.global.apiPayload.dto.PageResponse<>(List.of(), 0, 20, 0, 0, true));
        mockMvc.perform(get("/api/records/favorites").with(authentication(auth())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.size").value(20));
        verify(service).list(42L, org.springframework.data.domain.PageRequest.of(0, 20));
    }

    @Test
    void invalidPaginationReturnsRecordErrors() throws Exception {
        mockMvc.perform(get("/api/records/favorites?page=-1").with(authentication(auth())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("RECORD400_4"));
        for (int size : new int[]{0, 101}) {
            mockMvc.perform(get("/api/records/favorites?size=" + size).with(authentication(auth())))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("RECORD400_5"));
        }
    }

    @Test
    void duplicateReturns409Envelope() throws Exception {
        org.mockito.Mockito.doThrow(new GeneralException(RecordErrorCode.RECORD_FAVORITE_ALREADY_EXISTS))
                .when(service).add(42L, 77L);
        mockMvc.perform(post("/api/records/77/favorites").with(authentication(auth())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RECORD409_2"));
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(new UserPrincipal(42L, Role.USER), null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
    }
}
