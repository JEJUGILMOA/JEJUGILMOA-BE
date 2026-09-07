package com.example.jejugilmoa.domain.plan.controller;

import com.example.jejugilmoa.domain.auth.jwt.*;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.domain.plan.dto.WaypointReorderRequest;
import com.example.jejugilmoa.domain.plan.service.TripService;
import com.example.jejugilmoa.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
@Import(SecurityConfig.class)
@EnableWebSecurity
class TripWaypointOrderControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean TripService service;
    @MockitoBean JwtProvider jwtProvider;
    private final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            new UserPrincipal(42L, Role.USER), null, List.of());

    @Test void publicReorderApiDelegatesWithAuthenticatedOwner() throws Exception {
        mvc.perform(put("/api/trips/1/waypoints/order").with(authentication(auth))
                        .contentType("application/json")
                        .content("{\"visitDate\":\"2026-09-10\",\"waypointIds\":[3,1,2]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true));
        verify(service).reorderWaypoints(1L, 42L,
                new WaypointReorderRequest(LocalDate.of(2026, 9, 10), List.of(3L, 1L, 2L)));
    }

    @Test void invalidOrderBodyIsRejected() throws Exception {
        mvc.perform(put("/api/trips/1/waypoints/order").with(authentication(auth))
                        .contentType("application/json").content("{\"waypointIds\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
