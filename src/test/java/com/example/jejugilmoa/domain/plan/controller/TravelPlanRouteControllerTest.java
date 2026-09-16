package com.example.jejugilmoa.domain.plan.controller;

import com.example.jejugilmoa.domain.auth.jwt.*;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanRoutesResponse;
import com.example.jejugilmoa.domain.plan.enums.RouteGenerationStatus;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.service.TravelPlanRouteService;
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
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import static com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus.READY;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TravelPlanRouteController.class)
@Import(SecurityConfig.class)
@EnableWebSecurity
class TravelPlanRouteControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean TravelPlanRouteService service;
    @MockitoBean JwtProvider jwtProvider;
    private final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            new UserPrincipal(42L, Role.USER), null, List.of());

    @Test void returnsDateFilteredRouteAndLonLatArrayWithoutHash() throws Exception {
        var date = LocalDate.of(2026, 9, 10);
        when(service.getRoutes(1L, date)).thenReturn(new TravelPlanRoutesResponse(1L, new TravelPlanRoutesResponse.Generation(RouteGenerationStatus.PENDING), List.of(
                new TravelPlanRoutesResponse.Route(date, READY, "traoptimal", 18342, 2421000L,
                        Instant.parse("2026-09-07T00:00:00Z"), List.of(List.of(126.49, 33.51)), null))));
        mvc.perform(get("/api/plans/1/routes?date=2026-09-10").with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.routes[0].path[0][0]").value(126.49))
                .andExpect(jsonPath("$.result.routes[0].duration").value(2421000))
                .andExpect(jsonPath("$.result.planId").value(1))
                .andExpect(jsonPath("$.result.generation.status").value("PENDING"))
                .andExpect(jsonPath("$.result.routes[0].date").value("2026-09-10"))
                .andExpect(jsonPath("$.result.routes[0].status").value("READY"))
                .andExpect(jsonPath("$.result.routes[0].option").value("traoptimal"))
                .andExpect(jsonPath("$.result.routes[0].distance").value(18342))
                .andExpect(jsonPath("$.result.routes[0].path[0][1]").value(33.51))
                .andExpect(jsonPath("$.result.routes[0].calculatedAt").value("2026-09-07T00:00:00Z"))
                .andExpect(jsonPath("$.result.routes[0].failureCode").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.result.routes[0].routeHash").doesNotExist())
                .andExpect(jsonPath("$.result.generation.leaseToken").doesNotExist())
                .andExpect(jsonPath("$.result.generation.lastError").doesNotExist());
    }

    @Test void notFoundUsesExistingEnvelope() throws Exception {
        when(service.getRoutes(1L, null)).thenThrow(new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        mvc.perform(get("/api/plans/1/routes").with(authentication(auth)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(PlanErrorCode.PLAN_NOT_FOUND.getCode()));
    }

    @Test void unauthenticatedRequestSucceedsAndInvokesService() throws Exception {
        when(service.getRoutes(1L, null)).thenReturn(
                new TravelPlanRoutesResponse(1L, new TravelPlanRoutesResponse.Generation(RouteGenerationStatus.NOT_REQUESTED), List.of()));
        mvc.perform(get("/api/plans/1/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
        verify(service).getRoutes(1L, null);
    }

    @Test void invalidDateReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/plans/1/routes?date=invalid").with(authentication(auth)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void returnsAllRoutesWithoutDateAndPreservesNonReadyFields() throws Exception {
        var route = new TravelPlanRoutesResponse.Route(LocalDate.of(2026, 9, 11),
                com.example.jejugilmoa.domain.plan.enums.TravelPlanRouteStatus.NOT_REQUIRED,
                "traoptimal", null, null, null, List.of(), null);
        when(service.getRoutes(1L, null)).thenReturn(new TravelPlanRoutesResponse(1L,
                new TravelPlanRoutesResponse.Generation(RouteGenerationStatus.DONE), List.of(route)));
        mvc.perform(get("/api/plans/1/routes").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.generation.status").value("DONE"))
                .andExpect(jsonPath("$.result.routes[0].status").value("NOT_REQUIRED"))
                .andExpect(jsonPath("$.result.routes[0].path").isEmpty())
                .andExpect(jsonPath("$.result.routes[0].distance").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.result.routes[0].duration").value(org.hamcrest.Matchers.nullValue()));
        verify(service).getRoutes(1L, null);
    }

}
