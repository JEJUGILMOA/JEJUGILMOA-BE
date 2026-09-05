package com.example.jejugilmoa.domain.direction.controller;

import com.example.jejugilmoa.domain.direction.dto.DirectionResponse;
import com.example.jejugilmoa.domain.direction.exception.DirectionErrorCode;
import com.example.jejugilmoa.domain.direction.service.DirectionService;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DirectionController.class)
class DirectionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DirectionService directionService;

    @Test
    void getDriving_returns200WithRoute() throws Exception {
        var response = new DirectionResponse("traoptimal",
            new DirectionResponse.RouteSummary(45231, 3841000L, 0, 52000, 4780),
            List.of(new DirectionResponse.Coordinate(33.5104135, 126.4913534)));
        given(directionService.getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), isNull(), anyString()))
            .willReturn(response);

        mockMvc.perform(get("/api/directions/driving")
                .param("startLat", "33.5104135")
                .param("startLng", "126.4913534")
                .param("goalLat", "33.4619478")
                .param("goalLng", "126.9425362"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.result.option").value("traoptimal"))
            .andExpect(jsonPath("$.result.summary.distance").value(45231))
            .andExpect(jsonPath("$.result.path[0].lat").value(33.5104135))
            .andExpect(jsonPath("$.result.path[0].lng").value(126.4913534));
    }

    @Test
    void getDriving_returns404WhenRouteNotFound() throws Exception {
        given(directionService.getDriving(anyDouble(), anyDouble(), anyDouble(), anyDouble(), isNull(), anyString()))
            .willThrow(new GeneralException(DirectionErrorCode.ROUTE_NOT_FOUND));

        mockMvc.perform(get("/api/directions/driving")
                .param("startLat", "33.51")
                .param("startLng", "126.49")
                .param("goalLat", "33.46")
                .param("goalLng", "126.94"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.code").value("DIRECTION404_1"));
    }

    @Test
    void getDriving_returns400WhenMissingRequiredParam() throws Exception {
        mockMvc.perform(get("/api/directions/driving")
                .param("startLat", "33.51"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.isSuccess").value(false));
    }
}
