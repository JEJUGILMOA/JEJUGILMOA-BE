package com.example.jejugilmoa.domain.plan.controller;

import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.dto.FavoriteCreateRequest;
import com.example.jejugilmoa.domain.plan.dto.FavoritePlaceResponse;
import com.example.jejugilmoa.domain.plan.service.FavoriteService;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import com.example.jejugilmoa.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
@Import(SecurityConfig.class)
@EnableWebSecurity
class FavoriteControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FavoriteService favoriteService;
    @MockitoBean JwtProvider jwtProvider;

    @Test
    void add_returns201AndPassesAuthenticatedUserId() throws Exception {
        mockMvc.perform(post("/api/favorites")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"placeId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"));

        then(favoriteService).should().add(42L, new FavoriteCreateRequest(1L));
    }

    @Test
    void add_withoutPlaceIdReturns400() throws Exception {
        mockMvc.perform(post("/api/favorites")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    void delete_usesPlaceIdAndAuthenticatedUserId() throws Exception {
        mockMvc.perform(delete("/api/favorites/7")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"));

        then(favoriteService).should().delete(42L, 7L);
    }

    @Test
    void list_usesRequestedPageAndSizeAndReturnsPage() throws Exception {
        var response = new FavoritePlaceResponse(1L, "성산일출봉", "관광지", "서귀포시", "image.jpg");
        given(favoriteService.list(42L, PageRequest.of(2, 10)))
                .willReturn(new PageResponse<>(List.of(response), 2, 10, 21, 3, true));

        mockMvc.perform(get("/api/favorites?page=2&size=10")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].placeId").value(1))
                .andExpect(jsonPath("$.result.content[0].category").value("관광지"))
                .andExpect(jsonPath("$.result.page").value(2))
                .andExpect(jsonPath("$.result.size").value(10));

        then(favoriteService).should().list(42L, PageRequest.of(2, 10));
    }

    @Test
    void list_usesDefaultPageAndSize() throws Exception {
        given(favoriteService.list(42L, PageRequest.of(0, 20)))
                .willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/favorites")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isOk());

        then(favoriteService).should().list(42L, PageRequest.of(0, 20));
    }

    @Test
    void list_negativePageReturns400() throws Exception {
        mockMvc.perform(get("/api/favorites?page=-1")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FAVORITE400_2"));
    }

    @Test
    void list_invalidSizeReturns400() throws Exception {
        mockMvc.perform(get("/api/favorites?size=101")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FAVORITE400_3"));
    }

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_1"));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(Long userId) {
        UserPrincipal principal = new UserPrincipal(userId, Role.USER);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }
}
