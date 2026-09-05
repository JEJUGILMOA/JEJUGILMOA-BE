package com.example.jejugilmoa.domain.direction.service;

import com.example.jejugilmoa.domain.direction.converter.DirectionConverter;
import com.example.jejugilmoa.domain.direction.dto.DirectionResponse;
import com.example.jejugilmoa.domain.direction.exception.DirectionErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.navermap.NaverDirectionsClient;
import com.example.jejugilmoa.global.external.navermap.NaverMapException;
import com.example.jejugilmoa.global.external.navermap.dto.NaverDirectionsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DirectionServiceTest {

    @Mock NaverDirectionsClient naverDirectionsClient;
    @Spy DirectionConverter directionConverter;
    @InjectMocks DirectionService directionService;

    private static NaverDirectionsResponse successResponse(String option) {
        var summary = new NaverDirectionsResponse.Summary(45231, 3841000L, 0, 52000, 4780, null);
        var route = new NaverDirectionsResponse.NaverRoute(summary,
            List.of(List.of(126.4913534, 33.5104135), List.of(126.4927323, 33.5100244)));
        return new NaverDirectionsResponse(0, "success", "2026-09-04T12:00:00", Map.of(option, List.of(route)));
    }

    @Test
    void getDriving_성공시_경로를_위경도로_변환해_반환한다() {
        given(naverDirectionsClient.getDriving(anyString(), anyString(), isNull(), eq("traoptimal")))
            .willReturn(successResponse("traoptimal"));

        DirectionResponse result = directionService.getDriving(
            33.5104135, 126.4913534, 33.4619478, 126.9425362, null, "traoptimal");

        assertThat(result.option()).isEqualTo("traoptimal");
        assertThat(result.summary().distance()).isEqualTo(45231);
        assertThat(result.summary().duration()).isEqualTo(3841000L);
        // 네이버는 [경도, 위도] 순서 → 응답은 lat/lng 필드로 뒤집혀야 한다
        assertThat(result.path().get(0).lat()).isEqualTo(33.5104135);
        assertThat(result.path().get(0).lng()).isEqualTo(126.4913534);
        // 네이버 규격은 "경도,위도" 문자열로 전달되어야 한다
        verify(naverDirectionsClient).getDriving(
            eq("126.4913534,33.5104135"), eq("126.9425362,33.4619478"), isNull(), eq("traoptimal"));
    }

    @Test
    void getDriving_경유지를_네이버_규격으로_변환한다() {
        given(naverDirectionsClient.getDriving(anyString(), anyString(), anyString(), eq("trafast")))
            .willReturn(successResponse("trafast"));

        directionService.getDriving(33.51, 126.49, 33.46, 126.94,
            "33.4996213,126.5311884|33.45,126.57", "trafast");

        verify(naverDirectionsClient).getDriving(
            anyString(), anyString(), eq("126.5311884,33.4996213|126.57,33.45"), eq("trafast"));
    }

    @Test
    void getDriving_좌표범위_초과시_400() {
        assertThatThrownBy(() ->
            directionService.getDriving(91.0, 126.49, 33.46, 126.94, null, "traoptimal"))
            .isInstanceOf(GeneralException.class)
            .extracting(e -> ((GeneralException) e).getCode())
            .isEqualTo(DirectionErrorCode.INVALID_COORDINATE);
        verifyNoInteractions(naverDirectionsClient);
    }

    @Test
    void getDriving_지원하지_않는_옵션이면_400() {
        assertThatThrownBy(() ->
            directionService.getDriving(33.51, 126.49, 33.46, 126.94, null, "walking"))
            .isInstanceOf(GeneralException.class)
            .extracting(e -> ((GeneralException) e).getCode())
            .isEqualTo(DirectionErrorCode.INVALID_OPTION);
        verifyNoInteractions(naverDirectionsClient);
    }

    @Test
    void getDriving_경유지_6개_이상이면_400() {
        String sixWaypoints = "33.1,126.1|33.2,126.2|33.3,126.3|33.4,126.4|33.5,126.5|33.6,126.6";
        assertThatThrownBy(() ->
            directionService.getDriving(33.51, 126.49, 33.46, 126.94, sixWaypoints, "traoptimal"))
            .isInstanceOf(GeneralException.class)
            .extracting(e -> ((GeneralException) e).getCode())
            .isEqualTo(DirectionErrorCode.TOO_MANY_WAYPOINTS);
    }

    @Test
    void getDriving_경유지_형식이_잘못되면_400() {
        assertThatThrownBy(() ->
            directionService.getDriving(33.51, 126.49, 33.46, 126.94, "33.5;126.5", "traoptimal"))
            .isInstanceOf(GeneralException.class)
            .extracting(e -> ((GeneralException) e).getCode())
            .isEqualTo(DirectionErrorCode.INVALID_WAYPOINT_FORMAT);
    }

    @Test
    void getDriving_네이버가_경로없음_코드를_반환하면_404() {
        given(naverDirectionsClient.getDriving(anyString(), anyString(), isNull(), anyString()))
            .willReturn(new NaverDirectionsResponse(3, "no route", null, null));

        assertThatThrownBy(() ->
            directionService.getDriving(33.51, 126.49, 33.46, 126.94, null, "traoptimal"))
            .isInstanceOf(GeneralException.class)
            .extracting(e -> ((GeneralException) e).getCode())
            .isEqualTo(DirectionErrorCode.ROUTE_NOT_FOUND);
    }

    @Test
    void getDriving_출발지와_목적지가_동일하면_400() {
        given(naverDirectionsClient.getDriving(anyString(), anyString(), isNull(), anyString()))
            .willReturn(new NaverDirectionsResponse(1, "same", null, null));

        assertThatThrownBy(() ->
            directionService.getDriving(33.51, 126.49, 33.51, 126.49, null, "traoptimal"))
            .isInstanceOf(GeneralException.class)
            .extracting(e -> ((GeneralException) e).getCode())
            .isEqualTo(DirectionErrorCode.SAME_START_GOAL);
    }

    @Test
    void getDriving_네이버_호출_실패시_502() {
        given(naverDirectionsClient.getDriving(anyString(), anyString(), isNull(), anyString()))
            .willThrow(new NaverMapException("연결 실패"));

        assertThatThrownBy(() ->
            directionService.getDriving(33.51, 126.49, 33.46, 126.94, null, "traoptimal"))
            .isInstanceOf(GeneralException.class)
            .extracting(e -> ((GeneralException) e).getCode())
            .isEqualTo(DirectionErrorCode.NAVER_API_ERROR);
    }
}
