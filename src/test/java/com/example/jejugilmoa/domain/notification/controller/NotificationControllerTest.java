package com.example.jejugilmoa.domain.notification.controller;

import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.notification.dto.NotificationResponse;
import com.example.jejugilmoa.domain.notification.dto.UnreadCountResponse;
import com.example.jejugilmoa.domain.notification.enums.NotificationCategory;
import com.example.jejugilmoa.domain.notification.exception.NotificationErrorCode;
import com.example.jejugilmoa.domain.notification.service.NotificationQueryService;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
@EnableWebSecurity
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean NotificationQueryService notificationQueryService;
    @MockitoBean JwtProvider jwtProvider;

    @Test
    void list_returns200WithPage() throws Exception {
        var response = new NotificationResponse(10L, NotificationCategory.PLAN, "다음 일정이 시작돼요", "본문",
                Instant.parse("2026-08-21T05:41:00Z"), false, "jejugilmoa://plans/1");
        given(notificationQueryService.list(eq(42L), any()))
                .willReturn(new PageResponse<>(List.of(response), 0, 20, 1L, 1, true));

        mockMvc.perform(get("/api/notifications?page=0&size=20")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.content[0].title").value("다음 일정이 시작돼요"))
                .andExpect(jsonPath("$.result.content[0].isRead").value(false));
    }

    @Test
    void list_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_1"));
    }

    @Test
    void list_negativePageReturns400() throws Exception {
        mockMvc.perform(get("/api/notifications?page=-1")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTIFICATION400_2"));
    }

    @Test
    void list_oversizedPageReturns400() throws Exception {
        mockMvc.perform(get("/api/notifications?size=101")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTIFICATION400_3"));
    }

    @Test
    void unreadCount_returns200() throws Exception {
        given(notificationQueryService.unreadCount(42L)).willReturn(new UnreadCountResponse(3));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.count").value(3));
    }

    @Test
    void markAsRead_returns200() throws Exception {
        mockMvc.perform(patch("/api/notifications/read")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"all\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    void markAsRead_invalidRequestReturns400() throws Exception {
        given(notificationQueryService.unreadCount(42L)).willReturn(new UnreadCountResponse(0));
        org.mockito.Mockito.doThrow(new GeneralException(NotificationErrorCode.INVALID_READ_REQUEST))
                .when(notificationQueryService).markAsRead(eq(42L), any());

        mockMvc.perform(patch("/api/notifications/read")
                        .with(authentication(authenticationFor(42L)))
                        .contentType("application/json")
                        .content("{\"notificationIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTIFICATION400_1"));
    }

    @Test
    void delete_returns200() throws Exception {
        mockMvc.perform(delete("/api/notifications/10")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    void delete_returns404WhenNotOwned() throws Exception {
        org.mockito.Mockito.doThrow(new GeneralException(NotificationErrorCode.NOTIFICATION_NOT_FOUND))
                .when(notificationQueryService).delete(42L, 999L);

        mockMvc.perform(delete("/api/notifications/999")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION404_1"));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(Long userId) {
        UserPrincipal principal = new UserPrincipal(userId, Role.USER);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }
}
