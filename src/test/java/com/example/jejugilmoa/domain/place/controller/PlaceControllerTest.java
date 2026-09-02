package com.example.jejugilmoa.domain.place.controller;

import com.example.jejugilmoa.domain.place.dto.PlaceDetailDto;
import com.example.jejugilmoa.domain.place.dto.PlaceSummaryDto;
import com.example.jejugilmoa.domain.place.dto.PopularPlaceDto;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.service.PlaceQueryService;
import com.example.jejugilmoa.domain.place.service.PlaceSyncService;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlaceController.class)
class PlaceControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PlaceQueryService placeQueryService;
    @MockitoBean PlaceSyncService placeSyncService;

    @Test
    void getPopular_returns200WithTop3() throws Exception {
        var dto = new PopularPlaceDto(1L, "한라산", "img.jpg", 1000, null, null, null);
        given(placeQueryService.getPopular(anyInt(), anyInt(), any()))
            .willReturn(new PageResponse<>(List.of(dto), 0, 20, 1L, 1, true));

        mockMvc.perform(get("/api/places/popular?size=3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.result.content[0].name").value("한라산"))
            .andExpect(jsonPath("$.result.content[0].placeId").value(1));
    }

    @Test
    void browse_returns200WithPage() throws Exception {
        var summary = new PlaceSummaryDto(1L, "한라산", "제주시", "img.jpg", "자연",
                new BigDecimal("33.3617"), new BigDecimal("126.5292"));
        given(placeQueryService.browse(isNull(), eq("자연"), any()))
            .willReturn(new PageResponse<>(List.of(summary), 0, 20, 1L, 1, true));

        mockMvc.perform(get("/api/places?category=자연&page=0&size=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.result.content[0].name").value("한라산"))
            .andExpect(jsonPath("$.result.content[0].latitude").value(33.3617))
            .andExpect(jsonPath("$.result.content[0].longitude").value(126.5292))
            .andExpect(jsonPath("$.result.totalElements").value(1));
    }

    @Test
    void getDetail_returns200() throws Exception {
        given(placeQueryService.getDetail(1L)).willReturn(
            new PlaceDetailDto(1L, "한라산", "제주시", new BigDecimal("33.36"), new BigDecimal("126.53"),
                null, "img.jpg", List.of(), "자연")
        );

        mockMvc.perform(get("/api/places/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.name").value("한라산"))
            .andExpect(jsonPath("$.result.categoryName").value("자연"));
    }

    @Test
    void getDetail_returns404WhenNotFound() throws Exception {
        given(placeQueryService.getDetail(999L))
            .willThrow(new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));

        mockMvc.perform(get("/api/places/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.code").value("PLACE404_1"));
    }
}
